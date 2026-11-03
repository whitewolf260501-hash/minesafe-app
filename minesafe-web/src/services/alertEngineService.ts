import { ref, onValue, set, update } from 'firebase/database';
import { database } from './firebaseService';
import {
  DeviceTelemetry,
  DeviceAlertConfig,
  AlertLevel,
  AlertEvent,
  EvaluationResult,
  VariableEvaluation,
} from '../types/minesafe';

/**
 * Motor Centralizado de Evaluación de Umbrales y Alertas Ambientales para MineSafe.
 *
 * Características:
 * 1. Compara las lecturas telemétricas contra los umbrales configurados dinámicamente en Firebase.
 * 2. Gestiona 4 estados: 'normal', 'preventivo', 'advertencia', 'critico'.
 * 3. Máquina de estados con desduplicación inteligente (Debounce / In-flight state):
 *    - No genera cientos de alertas repetidas mientras una condición permanece activa.
 *    - Detecta escalamiento (ej: de preventivo a crítico).
 *    - Detecta recuperación (cuando los valores descienden por debajo del umbral de advertencia/preventivo).
 * 4. Persiste los eventos en Firebase RTDB (`eventos_alertas/{deviceId}/{eventId}`) para trazabilidad legal y comités paritarios.
 * 5. Distingue estrictamente superaciones de umbral de anomalías estadísticas calculadas por MineSafe IA.
 */

interface ActiveAlertState {
  eventId: string;
  level: AlertLevel;
  startedAt: string;
  lastValue: number;
}

export class AlertEngineService {
  // Mapa de eventos activos en memoria por clave: `${deviceId}_${variable}`
  private activeEventsMap = new Map<string, ActiveAlertState>();

  /**
   * Evalúa la telemetría en tiempo real de un nodo contra su configuración de umbrales.
   */
  evaluateTelemetry(
    telemetry: DeviceTelemetry,
    config: DeviceAlertConfig
  ): EvaluationResult {
    const timestamp = new Date().toISOString();
    const variablesEval: VariableEvaluation[] = [];
    const newEventsGenerated: AlertEvent[] = [];
    const resolvedEvents: AlertEvent[] = [];

    // Definición de variables a evaluar con sus valores reales de telemetría
    const metricsToEvaluate: Array<{
      variable: 'pm25' | 'mq2' | 'temperatura' | 'humedad';
      label: string;
      value: number | null;
      unit: string;
      threshold: typeof config.pm25;
    }> = [
      {
        variable: 'pm25',
        label: 'Polvo PM2.5',
        value: telemetry.pm25_adc !== undefined ? telemetry.pm25_adc : null,
        unit: config.pm25.unidad,
        threshold: config.pm25,
      },
      {
        variable: 'mq2',
        label: 'Gas MQ-2',
        value: telemetry.mq2_adc !== undefined ? telemetry.mq2_adc : telemetry.mq135_adc !== undefined ? telemetry.mq135_adc : null,
        unit: config.mq2.unidad,
        threshold: config.mq2,
      },
      {
        variable: 'temperatura',
        label: 'Temperatura',
        value: telemetry.temperatura !== undefined ? telemetry.temperatura : null,
        unit: config.temperatura.unidad,
        threshold: config.temperatura,
      },
      {
        variable: 'humedad',
        label: 'Humedad',
        value: telemetry.humedad !== undefined ? telemetry.humedad : null,
        unit: config.humedad.unidad,
        threshold: config.humedad,
      },
    ];

    let highestSeverity: AlertLevel = 'normal';

    for (const item of metricsToEvaluate) {
      if (!item.threshold.activo || item.value === null || isNaN(item.value)) {
        variablesEval.push({
          variable: item.variable,
          label: item.label,
          value: item.value,
          unit: item.unit,
          level: 'normal',
          active: false,
        });
        continue;
      }

      const val = item.value;
      const th = item.threshold;
      let level: AlertLevel = 'normal';
      let exceededThreshold: number | undefined;

      if (val >= th.critico) {
        level = 'critico';
        exceededThreshold = th.critico;
      } else if (val >= th.advertencia) {
        level = 'advertencia';
        exceededThreshold = th.advertencia;
      } else if (val >= th.preventivo) {
        level = 'preventivo';
        exceededThreshold = th.preventivo;
      }

      // Actualizar severidad general
      highestSeverity = this.maxSeverity(highestSeverity, level);

      variablesEval.push({
        variable: item.variable,
        label: item.label,
        value: val,
        unit: item.unit,
        level,
        thresholdExceeded: exceededThreshold,
        active: true,
      });

      // Gestión de estado y desduplicación inteligente
      const stateKey = `${telemetry.deviceId}_${item.variable}`;
      const currentState = this.activeEventsMap.get(stateKey);

      if (level !== 'normal') {
        // La condición supera un umbral
        if (!currentState) {
          // NUEVO EVENTO: No existía condición previa activa
          const eventId = `ev_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
          const newEvent: AlertEvent = {
            id: eventId,
            timestamp,
            deviceId: telemetry.deviceId,
            variable: item.variable,
            variableLabel: item.label,
            valorMedido: val,
            unidad: item.unit,
            umbralSuperado: exceededThreshold!,
            nivel: level,
            estado: 'activo',
            mensaje: `Superación de umbral ${level.toUpperCase()}: ${item.label} registró ${val} ${item.unit} (límite configurado: ${exceededThreshold} ${item.unit})`,
          };

          this.activeEventsMap.set(stateKey, {
            eventId,
            level,
            startedAt: timestamp,
            lastValue: val,
          });

          newEventsGenerated.push(newEvent);
          this.persistEventToFirebase(newEvent);
        } else if (currentState.level !== level) {
          // ESCALACIÓN O CAMBIO DE NIVEL: Actualizar el estado
          const escalatedEvent: AlertEvent = {
            id: currentState.eventId,
            timestamp,
            deviceId: telemetry.deviceId,
            variable: item.variable,
            variableLabel: item.label,
            valorMedido: val,
            unidad: item.unit,
            umbralSuperado: exceededThreshold!,
            nivel: level,
            estado: 'activo',
            mensaje: `Cambio de severidad a ${level.toUpperCase()}: ${item.label} varió a ${val} ${item.unit} (umbral: ${exceededThreshold} ${item.unit})`,
          };

          currentState.level = level;
          currentState.lastValue = val;

          newEventsGenerated.push(escalatedEvent);
          this.updateEventInFirebase(escalatedEvent);
        } else {
          // Condición persiste en el mismo nivel: NO duplicar alertas
          currentState.lastValue = val;
        }
      } else {
        // La condición volvió a NORMAL
        if (currentState) {
          // RECUPERACIÓN: Marcar el evento anterior como resuelto
          const resolvedEvent: AlertEvent = {
            id: currentState.eventId,
            timestamp: currentState.startedAt,
            deviceId: telemetry.deviceId,
            variable: item.variable,
            variableLabel: item.label,
            valorMedido: val,
            unidad: item.unit,
            umbralSuperado: 0,
            nivel: currentState.level as 'preventivo' | 'advertencia' | 'critico',
            estado: 'recuperado',
            mensaje: `Condición normalizada: ${item.label} descendió a ${val} ${item.unit} (por debajo de preventivo ${th.preventivo})`,
            fechaResolucion: timestamp,
          };

          this.activeEventsMap.delete(stateKey);
          resolvedEvents.push(resolvedEvent);
          this.updateEventInFirebase(resolvedEvent);
        }
      }
    }

    return {
      deviceId: telemetry.deviceId,
      timestamp,
      overallLevel: highestSeverity,
      variables: variablesEval,
      newEventsGenerated,
      resolvedEvents,
    };
  }

  /**
   * Determina la severidad máxima entre dos niveles.
   */
  private maxSeverity(a: AlertLevel, b: AlertLevel): AlertLevel {
    const order: Record<AlertLevel, number> = {
      normal: 0,
      preventivo: 1,
      advertencia: 2,
      critico: 3,
    };
    return order[a] >= order[b] ? a : b;
  }

  /**
   * Persiste un nuevo evento de alerta en Firebase Realtime Database.
   */
  private async persistEventToFirebase(event: AlertEvent): Promise<void> {
    try {
      const eventRef = ref(database, `eventos_alertas/${event.deviceId}/${event.id}`);
      await set(eventRef, event);
    } catch (err) {
      console.warn('Error al guardar evento de alerta en Firebase RTDB:', err);
    }
  }

  /**
   * Actualiza el estado de un evento existente en Firebase (por escalación o resolución).
   */
  private async updateEventInFirebase(event: AlertEvent): Promise<void> {
    try {
      const eventRef = ref(database, `eventos_alertas/${event.deviceId}/${event.id}`);
      await update(eventRef, {
        nivel: event.nivel,
        estado: event.estado,
        valorMedido: event.valorMedido,
        mensaje: event.mensaje,
        fechaResolucion: event.fechaResolucion || null,
      });
    } catch (err) {
      console.warn('Error al actualizar evento de alerta en Firebase RTDB:', err);
    }
  }

  /**
   * Suscribe en tiempo real a los eventos registrados en Firebase para un dispositivo
   * (o para todos los dispositivos si deviceId es null).
   */
  subscribeToAlertEvents(
    deviceId: string | null,
    onUpdate: (events: AlertEvent[]) => void
  ): () => void {
    const path = deviceId ? `eventos_alertas/${deviceId}` : 'eventos_alertas';
    const eventsRef = ref(database, path);

    return onValue(
      eventsRef,
      (snapshot) => {
        if (!snapshot.exists()) {
          onUpdate([]);
          return;
        }

        const data = snapshot.val();
        const events: AlertEvent[] = [];

        if (deviceId) {
          for (const [key, val] of Object.entries<any>(data)) {
            if (val && typeof val === 'object') {
              events.push({
                id: val.id || key,
                timestamp: val.timestamp || new Date().toISOString(),
                deviceId: val.deviceId || deviceId,
                variable: val.variable || 'mq2',
                variableLabel: val.variableLabel || 'Variable',
                valorMedido: Number(val.valorMedido ?? 0),
                unidad: val.unidad || '',
                umbralSuperado: Number(val.umbralSuperado ?? 0),
                nivel: val.nivel || 'preventivo',
                estado: val.estado || 'activo',
                mensaje: val.mensaje || 'Superación de umbral',
                fechaResolucion: val.fechaResolucion,
              });
            }
          }
        } else {
          // Múltiples dispositivos
          for (const [devKey, devEvents] of Object.entries<any>(data)) {
            if (devEvents && typeof devEvents === 'object') {
              for (const [evKey, val] of Object.entries<any>(devEvents)) {
                if (val && typeof val === 'object') {
                  events.push({
                    id: val.id || evKey,
                    timestamp: val.timestamp || new Date().toISOString(),
                    deviceId: val.deviceId || devKey,
                    variable: val.variable || 'mq2',
                    variableLabel: val.variableLabel || 'Variable',
                    valorMedido: Number(val.valorMedido ?? 0),
                    unidad: val.unidad || '',
                    umbralSuperado: Number(val.umbralSuperado ?? 0),
                    nivel: val.nivel || 'preventivo',
                    estado: val.estado || 'activo',
                    mensaje: val.mensaje || 'Superación de umbral',
                    fechaResolucion: val.fechaResolucion,
                  });
                }
              }
            }
          }
        }

        // Ordenar del más reciente al más antiguo
        events.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
        onUpdate(events);
      },
      (error) => {
        console.warn('Error leyendo eventos de alerta desde Firebase:', error);
        onUpdate([]);
      }
    );
  }
}

export const alertEngineService = new AlertEngineService();
