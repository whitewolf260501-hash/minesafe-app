import { ref, onValue, set } from 'firebase/database';
import { database } from './firebaseService';
import { DeviceAlertConfig, VariableThresholdConfig } from '../types/minesafe';

/**
 * Servicio de Configuración Dinámica de Umbrales para MineSafe.
 * Permite gestionar los límites ambientales almacenados en Firebase Realtime Database
 * sin requerir recompilar ni reflashear el firmware de los nodos ESP32.
 *
 * Estructura en Firebase:
 * configuracion_alertas/{deviceId}/
 *   pm25: { activo, preventivo, advertencia, critico, notificaciones, unidad, descripcion }
 *   mq2: { ... }
 *   temperatura: { ... }
 *   humedad: { ... }
 *   ultimoCambio: { fecha, usuario, rol, motivo }
 */

export const DEFAULT_THRESHOLDS = {
  pm25: {
    activo: true,
    preventivo: 45,
    advertencia: 85,
    critico: 130,
    notificaciones: true,
    unidad: 'ADC (raw)',
    descripcion: 'Sensor de partículas PM2.5 (lectura analógica sin calibrar; no confundir con µg/m³)',
  } as VariableThresholdConfig,
  mq2: {
    activo: true,
    preventivo: 350,
    advertencia: 520,
    critico: 750,
    notificaciones: true,
    unidad: 'ADC (raw)',
    descripcion: 'Sensor analógico MQ-2 para gases combustibles y humo (lectura cruda)',
  } as VariableThresholdConfig,
  temperatura: {
    activo: true,
    preventivo: 26.0,
    advertencia: 30.0,
    critico: 34.0,
    notificaciones: true,
    unidad: '°C',
    descripcion: 'Sensor DHT de temperatura ambiental en socavón minero',
  } as VariableThresholdConfig,
  humedad: {
    activo: true,
    preventivo: 75.0,
    advertencia: 85.0,
    critico: 92.0,
    notificaciones: true,
    unidad: '%',
    descripcion: 'Humedad relativa ambiental en galería subterránea',
  } as VariableThresholdConfig,
};

export class AlertConfigService {
  /**
   * Genera una configuración por defecto con valores iniciales de ejemplo.
   * IMPORTANTE: No son límites oficiales D.S. 132 sin calibración metrológica.
   */
  getDefaultConfig(deviceId: string): DeviceAlertConfig {
    return {
      dispositivo_id: deviceId,
      pm25: { ...DEFAULT_THRESHOLDS.pm25 },
      mq2: { ...DEFAULT_THRESHOLDS.mq2 },
      temperatura: { ...DEFAULT_THRESHOLDS.temperatura },
      humedad: { ...DEFAULT_THRESHOLDS.humedad },
      ultimoCambio: {
        fecha: new Date().toISOString(),
        usuario: 'Sistema (Predeterminado de referencia)',
        rol: 'admin',
        motivo: 'Valores iniciales de referencia de fábrica',
      },
    };
  }

  /**
   * Valida la coherencia lógica de los umbrales:
   * preventivo < advertencia < critico
   */
  validateThresholds(config: DeviceAlertConfig): { valid: boolean; errors: string[] } {
    const errors: string[] = [];
    const vars: Array<{ key: keyof Pick<DeviceAlertConfig, 'pm25' | 'mq2' | 'temperatura' | 'humedad'>; name: string }> = [
      { key: 'pm25', name: 'PM2.5' },
      { key: 'mq2', name: 'MQ-2 Gas' },
      { key: 'temperatura', name: 'Temperatura' },
      { key: 'humedad', name: 'Humedad' },
    ];

    for (const v of vars) {
      const item = config[v.key];
      if (item.activo) {
        if (isNaN(item.preventivo) || isNaN(item.advertencia) || isNaN(item.critico)) {
          errors.push(`En ${v.name}: Todos los umbrales deben ser valores numéricos válidos.`);
          continue;
        }
        if (item.preventivo < 0 || item.advertencia < 0 || item.critico < 0) {
          errors.push(`En ${v.name}: Los valores no pueden ser negativos.`);
        }
        if (item.preventivo >= item.advertencia) {
          errors.push(`En ${v.name}: El umbral Preventivo (${item.preventivo}) debe ser menor al de Advertencia (${item.advertencia}).`);
        }
        if (item.advertencia >= item.critico) {
          errors.push(`En ${v.name}: El umbral de Advertencia (${item.advertencia}) debe ser menor al Crítico (${item.critico}).`);
        }
      }
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Suscribe en tiempo real a los cambios de configuración de un dispositivo en Firebase RTDB.
   * Si no existe configuración previa, retorna la configuración por defecto de referencia.
   */
  subscribeToDeviceAlertConfig(
    deviceId: string,
    onUpdate: (config: DeviceAlertConfig) => void,
    onError?: (err: Error) => void
  ): () => void {
    const configRef = ref(database, `configuracion_alertas/${deviceId}`);

    return onValue(
      configRef,
      (snapshot) => {
        if (!snapshot.exists()) {
          const fallback = this.getDefaultConfig(deviceId);
          onUpdate(fallback);
          return;
        }

        const data = snapshot.val();
        const merged: DeviceAlertConfig = {
          dispositivo_id: deviceId,
          pm25: {
            activo: data.pm25?.activo ?? DEFAULT_THRESHOLDS.pm25.activo,
            preventivo: Number(data.pm25?.preventivo ?? DEFAULT_THRESHOLDS.pm25.preventivo),
            advertencia: Number(data.pm25?.advertencia ?? DEFAULT_THRESHOLDS.pm25.advertencia),
            critico: Number(data.pm25?.critico ?? DEFAULT_THRESHOLDS.pm25.critico),
            notificaciones: data.pm25?.notificaciones ?? DEFAULT_THRESHOLDS.pm25.notificaciones,
            unidad: data.pm25?.unidad || DEFAULT_THRESHOLDS.pm25.unidad,
            descripcion: data.pm25?.descripcion || DEFAULT_THRESHOLDS.pm25.descripcion,
          },
          mq2: {
            activo: data.mq2?.activo ?? DEFAULT_THRESHOLDS.mq2.activo,
            preventivo: Number(data.mq2?.preventivo ?? DEFAULT_THRESHOLDS.mq2.preventivo),
            advertencia: Number(data.mq2?.advertencia ?? DEFAULT_THRESHOLDS.mq2.advertencia),
            critico: Number(data.mq2?.critico ?? DEFAULT_THRESHOLDS.mq2.critico),
            notificaciones: data.mq2?.notificaciones ?? DEFAULT_THRESHOLDS.mq2.notificaciones,
            unidad: data.mq2?.unidad || DEFAULT_THRESHOLDS.mq2.unidad,
            descripcion: data.mq2?.descripcion || DEFAULT_THRESHOLDS.mq2.descripcion,
          },
          temperatura: {
            activo: data.temperatura?.activo ?? DEFAULT_THRESHOLDS.temperatura.activo,
            preventivo: Number(data.temperatura?.preventivo ?? DEFAULT_THRESHOLDS.temperatura.preventivo),
            advertencia: Number(data.temperatura?.advertencia ?? DEFAULT_THRESHOLDS.temperatura.advertencia),
            critico: Number(data.temperatura?.critico ?? DEFAULT_THRESHOLDS.temperatura.critico),
            notificaciones: data.temperatura?.notificaciones ?? DEFAULT_THRESHOLDS.temperatura.notificaciones,
            unidad: data.temperatura?.unidad || DEFAULT_THRESHOLDS.temperatura.unidad,
            descripcion: data.temperatura?.descripcion || DEFAULT_THRESHOLDS.temperatura.descripcion,
          },
          humedad: {
            activo: data.humedad?.activo ?? DEFAULT_THRESHOLDS.humedad.activo,
            preventivo: Number(data.humedad?.preventivo ?? DEFAULT_THRESHOLDS.humedad.preventivo),
            advertencia: Number(data.humedad?.advertencia ?? DEFAULT_THRESHOLDS.humedad.advertencia),
            critico: Number(data.humedad?.critico ?? DEFAULT_THRESHOLDS.humedad.critico),
            notificaciones: data.humedad?.notificaciones ?? DEFAULT_THRESHOLDS.humedad.notificaciones,
            unidad: data.humedad?.unidad || DEFAULT_THRESHOLDS.humedad.unidad,
            descripcion: data.humedad?.descripcion || DEFAULT_THRESHOLDS.humedad.descripcion,
          },
          ultimoCambio: data.ultimoCambio || {
            fecha: new Date().toISOString(),
            usuario: 'Desconocido',
            rol: 'admin',
          },
        };

        onUpdate(merged);
      },
      (error) => {
        console.warn(`Error al suscribir a configuracion_alertas/${deviceId}:`, error);
        if (onError) onError(error);
        else onUpdate(this.getDefaultConfig(deviceId));
      }
    );
  }

  /**
   * Guarda las modificaciones de umbrales en Firebase Realtime Database.
   * Valida la lógica de rangos y registra la auditoría del usuario que realizó el cambio.
   */
  async saveDeviceAlertConfig(
    config: DeviceAlertConfig,
    user: { name: string; role: string; motivo?: string }
  ): Promise<{ success: boolean; message: string }> {
    // 1. Validación de permisos
    const allowedRoles = ['admin', 'superAdmin', 'prevencionista'];
    if (!allowedRoles.includes(user.role)) {
      throw new Error(`Permiso denegado: El rol "${user.role}" no tiene autorización para modificar umbrales ambientales.`);
    }

    // 2. Validación de rangos
    const validation = this.validateThresholds(config);
    if (!validation.valid) {
      throw new Error(`Error de validación:\n• ${validation.errors.join('\n• ')}`);
    }

    const payload: DeviceAlertConfig = {
      ...config,
      ultimoCambio: {
        fecha: new Date().toISOString(),
        usuario: user.name,
        rol: user.role,
        motivo: user.motivo || 'Actualización de umbrales desde plataforma web',
      },
    };

    try {
      const configRef = ref(database, `configuracion_alertas/${config.dispositivo_id}`);
      await set(configRef, payload);
      return {
        success: true,
        message: `Umbrales actualizados exitosamente en Firebase para el nodo ${config.dispositivo_id}.`,
      };
    } catch (err: any) {
      console.error('Error guardando umbrales en Firebase:', err);
      throw new Error(`No se pudo guardar en Firebase: ${err.message || 'Error de conexión'}`);
    }
  }

  /**
   * Restaura los valores de fábrica/ejemplo para un dispositivo en Firebase.
   */
  async restoreDefaultConfig(
    deviceId: string,
    user: { name: string; role: string }
  ): Promise<DeviceAlertConfig> {
    const defaultConfig = this.getDefaultConfig(deviceId);
    defaultConfig.ultimoCambio = {
      fecha: new Date().toISOString(),
      usuario: user.name,
      rol: user.role,
      motivo: 'Restauración de valores de fábrica de referencia',
    };

    await this.saveDeviceAlertConfig(defaultConfig, {
      name: user.name,
      role: user.role,
      motivo: 'Restauración de valores de fábrica de referencia',
    });

    return defaultConfig;
  }
}

export const alertConfigService = new AlertConfigService();
