import React, { useState, useEffect, useMemo } from 'react';
import {
  DeviceTelemetry,
  DeviceAlertConfig,
  AlertEvent,
} from '../types/minesafe';
import { alertConfigService } from '../services/alertConfigService';
import { alertEngineService } from '../services/alertEngineService';
import {
  Bell,
  CheckCircle2,
  AlertTriangle,
  Flame,
  Wind,
  Thermometer,
  Droplets,
  Save,
  RotateCcw,
  Shield,
  Clock,
  User,
  Sliders,
  Info,
  Check,
  AlertCircle,
  Eye,
  Filter,
} from 'lucide-react';

interface AlertConfigViewProps {
  selectedDeviceId: string;
  availableDevices: DeviceTelemetry[];
  onSelectDevice: (deviceId: string) => void;
}

export const AlertConfigView: React.FC<AlertConfigViewProps> = ({
  selectedDeviceId,
  availableDevices,
  onSelectDevice,
}) => {
  // Configuración cargada desde Firebase
  const [config, setConfig] = useState<DeviceAlertConfig>(() =>
    alertConfigService.getDefaultConfig(selectedDeviceId)
  );
  // Estado editable local
  const [editableConfig, setEditableConfig] = useState<DeviceAlertConfig>(() =>
    alertConfigService.getDefaultConfig(selectedDeviceId)
  );

  // Rol del usuario activo (Simulación de sesión de seguridad)
  const [currentUserRole, setCurrentUserRole] = useState<'admin' | 'prevencionista' | 'operador'>('admin');
  const [currentUserName, setCurrentUserName] = useState<string>('Manuel Crisóstomo (SuperAdmin)');

  // Estados de interfaz y feedback
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [saveSuccessMsg, setSaveSuccessMsg] = useState<string | null>(null);
  const [saveErrorMsg, setSaveErrorMsg] = useState<string | null>(null);
  const [showConfirmRestoreModal, setShowConfirmRestoreModal] = useState<boolean>(false);
  const [isRestoring, setIsRestoring] = useState<boolean>(false);

  // Eventos de alerta del motor
  const [events, setEvents] = useState<AlertEvent[]>([]);
  const [eventFilter, setEventFilter] = useState<'all' | 'activo' | 'recuperado'>('all');

  // Dispositivo activo actual
  const currentTelemetry = availableDevices.find((d) => d.deviceId === selectedDeviceId);

  // 1. Suscripción a Firebase RTDB para la configuración de umbrales
  useEffect(() => {
    setSaveSuccessMsg(null);
    setSaveErrorMsg(null);

    const unsubscribe = alertConfigService.subscribeToDeviceAlertConfig(
      selectedDeviceId,
      (remoteConfig) => {
        setConfig(remoteConfig);
        setEditableConfig(JSON.parse(JSON.stringify(remoteConfig)));
      },
      (err) => {
        setSaveErrorMsg(`Error de conexión con Firebase: ${err.message}`);
      }
    );

    return () => unsubscribe();
  }, [selectedDeviceId]);

  // 2. Suscripción a los eventos generados por el motor de alertas en Firebase RTDB
  useEffect(() => {
    const unsubscribe = alertEngineService.subscribeToAlertEvents(
      selectedDeviceId,
      (remoteEvents) => {
        setEvents(remoteEvents);
      }
    );

    return () => unsubscribe();
  }, [selectedDeviceId]);

  // 3. Ejecución del motor de alertas cuando llega nueva telemetría
  useEffect(() => {
    if (currentTelemetry && config) {
      const evaluation = alertEngineService.evaluateTelemetry(currentTelemetry, config);
      if (evaluation.newEventsGenerated.length > 0 || evaluation.resolvedEvents.length > 0) {
        // Los eventos se persisten automáticamente en Firebase por el servicio
      }
    }
  }, [currentTelemetry, config]);

  // Validación en tiempo real
  const validation = useMemo(() => {
    return alertConfigService.validateThresholds(editableConfig);
  }, [editableConfig]);

  // Comprobar si hay cambios pendientes sin guardar
  const hasUnsavedChanges = useMemo(() => {
    return JSON.stringify(config) !== JSON.stringify(editableConfig);
  }, [config, editableConfig]);

  // Permisos según el rol seleccionado
  const canEdit = currentUserRole === 'admin' || currentUserRole === 'prevencionista';

  // Manejador de cambio de umbral
  const handleThresholdChange = (
    variable: 'pm25' | 'mq2' | 'temperatura' | 'humedad',
    field: 'preventivo' | 'advertencia' | 'critico',
    val: string
  ) => {
    const num = parseFloat(val);
    setEditableConfig((prev) => ({
      ...prev,
      [variable]: {
        ...prev[variable],
        [field]: isNaN(num) ? 0 : num,
      },
    }));
  };

  // Manejador de switches (activo / notificaciones)
  const handleToggle = (
    variable: 'pm25' | 'mq2' | 'temperatura' | 'humedad',
    field: 'activo' | 'notificaciones'
  ) => {
    setEditableConfig((prev) => ({
      ...prev,
      [variable]: {
        ...prev[variable],
        [field]: !prev[variable][field],
      },
    }));
  };

  // Guardar configuración en Firebase
  const handleSave = async () => {
    if (!canEdit) {
      setSaveErrorMsg('No posee permisos de administración para modificar los umbrales.');
      return;
    }

    if (!validation.valid) {
      setSaveErrorMsg('Corrige los errores de rango antes de guardar.');
      return;
    }

    setIsSaving(true);
    setSaveSuccessMsg(null);
    setSaveErrorMsg(null);

    try {
      const result = await alertConfigService.saveDeviceAlertConfig(editableConfig, {
        name: currentUserName,
        role: currentUserRole,
        motivo: 'Ajuste dinámico de umbrales desde plataforma web',
      });
      setSaveSuccessMsg(result.message);
      setTimeout(() => setSaveSuccessMsg(null), 5000);
    } catch (err: any) {
      setSaveErrorMsg(err.message || 'Error al guardar en Firebase');
    } finally {
      setIsSaving(false);
    }
  };

  // Restaurar valores de referencia predeterminados
  const handleRestoreDefaults = async () => {
    if (!canEdit) return;
    setIsRestoring(true);
    try {
      await alertConfigService.restoreDefaultConfig(selectedDeviceId, {
        name: currentUserName,
        role: currentUserRole,
      });
      setShowConfirmRestoreModal(false);
      setSaveSuccessMsg('Valores predeterminados de referencia restaurados exitosamente.');
      setTimeout(() => setSaveSuccessMsg(null), 5000);
    } catch (err: any) {
      setSaveErrorMsg(err.message || 'Error al restaurar');
    } finally {
      setIsRestoring(false);
    }
  };

  // Filtrado de eventos
  const filteredEvents = useMemo(() => {
    if (eventFilter === 'all') return events;
    return events.filter((e) => e.estado === eventFilter);
  }, [events, eventFilter]);

  return (
    <div className="space-y-6">
      {/* 1. Cabecera y Selector de Dispositivo & Rol */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-gradient-to-br from-amber-500 to-orange-600 rounded-xl text-slate-950 font-black">
              <Sliders className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-white tracking-tight">
                  Configuración Dinámica de Umbrales y Alertas
                </h2>
                <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/20">
                  Firebase RTDB
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Ajuste centralizado en la nube aplicado en tiempo real sin reprogramar firmware ESP32.
              </p>
            </div>
          </div>

          {/* Selectores de Dispositivo y Rol de Usuario */}
          <div className="flex flex-wrap items-center gap-3">
            {/* Selector de Dispositivo */}
            <div className="flex items-center space-x-2 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800 text-xs">
              <span className="text-slate-400 font-semibold">Nodo:</span>
              <select
                value={selectedDeviceId}
                onChange={(e) => onSelectDevice(e.target.value)}
                className="bg-transparent text-slate-200 font-mono font-bold focus:outline-none cursor-pointer"
              >
                {availableDevices.map((d) => (
                  <option key={d.deviceId} value={d.deviceId} className="bg-slate-900 text-white">
                    {d.nombre || d.deviceId} ({d.deviceId})
                  </option>
                ))}
              </select>
            </div>

            {/* Selector de Rol para Validación de Permisos */}
            <div className="flex items-center space-x-2 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800 text-xs">
              <User className="w-3.5 h-3.5 text-teal-400" />
              <span className="text-slate-400 font-semibold">Rol:</span>
              <select
                value={currentUserRole}
                onChange={(e) => {
                  const role = e.target.value as 'admin' | 'prevencionista' | 'operador';
                  setCurrentUserRole(role);
                  if (role === 'admin') setCurrentUserName('Manuel Crisóstomo (SuperAdmin)');
                  else if (role === 'prevencionista') setCurrentUserName('Ing. Prevención Riesgos (Prevencionista)');
                  else setCurrentUserName('Operador de Guardia (Solo Lectura)');
                }}
                className="bg-transparent text-slate-200 font-semibold focus:outline-none cursor-pointer"
              >
                <option value="admin" className="bg-slate-900 text-white">SuperAdmin (Total)</option>
                <option value="prevencionista" className="bg-slate-900 text-white">Prevencionista (Edición)</option>
                <option value="operador" className="bg-slate-900 text-white">Operador (Solo Lectura)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Advertencia de solo lectura si el rol no tiene permisos */}
        {!canEdit && (
          <div className="mt-4 p-3 bg-blue-500/10 border border-blue-500/20 rounded-lg flex items-center space-x-2 text-xs text-blue-300">
            <Info className="w-4 h-4 text-blue-400 shrink-0" />
            <span>
              <strong>Modo Consulta (Solo Lectura):</strong> Su rol actual ({currentUserRole}) no tiene privilegios para guardar modificaciones de umbrales. Seleccione rol SuperAdmin o Prevencionista para habilitar la edición.
            </span>
          </div>
        )}
      </div>

      {/* 2. Banner Normativo D.S. N° 132 / SERNAGEOMIN */}
      <div className="p-4 bg-amber-500/10 border border-amber-500/30 rounded-xl flex items-start space-x-3 text-xs text-amber-200">
        <Shield className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-bold text-amber-300">
            AVISO DE CUMPLIMIENTO Y DELIMITACIÓN NORMATIVA (D.S. N° 132 / SERNAGEOMIN)
          </p>
          <p className="text-amber-200/90 leading-relaxed text-[11px]">
            Los umbrales configurables en este panel corresponden a límites operacionales de referencia para la plataforma IoT. Las mediciones de los sensores analógicos <strong>MQ-2</strong> y <strong>PM2.5</strong> se leen y procesan como valores ADC crudos (<code className="bg-amber-950/60 px-1 py-0.5 rounded text-amber-300 font-mono">unit: raw</code>) y <strong>no deben ser interpretados como concentraciones volumétricas oficiales (ppm o µg/m³)</strong> sin certificación metrológica oficial vigente (Art. 136 del D.S. N° 132).
          </p>
        </div>
      </div>

      {/* 3. Notificaciones de Éxito y Error */}
      {saveSuccessMsg && (
        <div className="p-3.5 bg-emerald-500/10 border border-emerald-500/30 rounded-xl flex items-center space-x-2.5 text-xs text-emerald-300">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span className="font-medium">{saveSuccessMsg}</span>
        </div>
      )}

      {saveErrorMsg && (
        <div className="p-3.5 bg-rose-500/10 border border-rose-500/30 rounded-xl flex items-start space-x-2.5 text-xs text-rose-300">
          <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
          <span className="whitespace-pre-line font-medium">{saveErrorMsg}</span>
        </div>
      )}

      {!validation.valid && (
        <div className="p-3.5 bg-rose-500/10 border border-rose-500/30 rounded-xl text-xs text-rose-300 space-y-1">
          <div className="flex items-center space-x-2 font-bold text-rose-400">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>Errores de validación en los umbrales configurados:</span>
          </div>
          <ul className="list-disc list-inside space-y-0.5 pl-2 text-[11px]">
            {validation.errors.map((err, i) => (
              <li key={i}>{err}</li>
            ))}
          </ul>
        </div>
      )}

      {/* 4. Tarjetas de Configuración por Variable */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* PM2.5 */}
        <VariableConfigCard
          title="Polvo en Suspensión PM2.5"
          subtitle="Lectura analógica cruda del sensor óptico (ADC raw)"
          icon={<Wind className="w-5 h-5 text-purple-400" />}
          unit="ADC"
          accentColor="purple"
          currentValue={currentTelemetry?.pm25_adc}
          threshold={editableConfig.pm25}
          disabled={!canEdit}
          onChange={(field, val) => handleThresholdChange('pm25', field, val)}
          onToggle={(field) => handleToggle('pm25', field)}
        />

        {/* MQ-2 Gas */}
        <VariableConfigCard
          title="Gas Combustible MQ-2"
          subtitle="Lectura analógica cruda para gases inflamables y humo (ADC raw)"
          icon={<Flame className="w-5 h-5 text-amber-400" />}
          unit="ADC"
          accentColor="amber"
          currentValue={currentTelemetry?.mq2_adc ?? currentTelemetry?.mq135_adc}
          threshold={editableConfig.mq2}
          disabled={!canEdit}
          onChange={(field, val) => handleThresholdChange('mq2', field, val)}
          onToggle={(field) => handleToggle('mq2', field)}
        />

        {/* Temperatura */}
        <VariableConfigCard
          title="Temperatura Ambiental"
          subtitle="Sensor térmico en socavón de faena minera"
          icon={<Thermometer className="w-5 h-5 text-orange-400" />}
          unit="°C"
          accentColor="orange"
          currentValue={currentTelemetry?.temperatura}
          threshold={editableConfig.temperatura}
          disabled={!canEdit}
          onChange={(field, val) => handleThresholdChange('temperatura', field, val)}
          onToggle={(field) => handleToggle('temperatura', field)}
        />

        {/* Humedad */}
        <VariableConfigCard
          title="Humedad Relativa"
          subtitle="Sensor de humedad relativa en galería subterránea"
          icon={<Droplets className="w-5 h-5 text-cyan-400" />}
          unit="%"
          accentColor="cyan"
          currentValue={currentTelemetry?.humedad}
          threshold={editableConfig.humedad}
          disabled={!canEdit}
          onChange={(field, val) => handleThresholdChange('humedad', field, val)}
          onToggle={(field) => handleToggle('humedad', field)}
        />
      </div>

      {/* 5. Barra de Acciones y Metadatos de Auditoría */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="text-xs text-slate-400 flex items-center space-x-2">
          <Clock className="w-4 h-4 text-slate-500 shrink-0" />
          <span>
            Último cambio guardado:{' '}
            <strong className="text-slate-200">
              {config.ultimoCambio?.fecha
                ? new Date(config.ultimoCambio.fecha).toLocaleString()
                : 'Inicial de referencia'}
            </strong>{' '}
            por <strong className="text-slate-200">{config.ultimoCambio?.usuario || 'Sistema'}</strong>
          </span>
        </div>

        <div className="flex items-center space-x-3 w-full sm:w-auto justify-end">
          {/* Botón Restaurar Predeterminados */}
          <button
            onClick={() => setShowConfirmRestoreModal(true)}
            disabled={!canEdit || isSaving || isRestoring}
            className="flex items-center space-x-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 disabled:opacity-50 transition"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Restaurar Valores</span>
          </button>

          {/* Botón Guardar Cambios */}
          <button
            onClick={handleSave}
            disabled={!canEdit || isSaving || !validation.valid || !hasUnsavedChanges}
            className={`flex items-center space-x-1.5 px-4 py-2 rounded-lg text-xs font-bold transition shadow ${
              hasUnsavedChanges && validation.valid && canEdit
                ? 'bg-teal-600 hover:bg-teal-500 text-white'
                : 'bg-slate-800 text-slate-500 cursor-not-allowed'
            }`}
          >
            {isSaving ? (
              <span className="flex items-center space-x-1">
                <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin mr-1" />
                <span>Guardando...</span>
              </span>
            ) : (
              <>
                <Save className="w-3.5 h-3.5" />
                <span>Guardar Umbrales en Firebase</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* 6. Distinción conceptual: Umbrales vs MineSafe IA */}
      <div className="bg-slate-900/60 border border-slate-800/80 rounded-xl p-4 text-xs text-slate-300 flex items-start space-x-3">
        <div className="p-2 bg-indigo-500/10 rounded-lg text-indigo-400 shrink-0">
          <Info className="w-4 h-4" />
        </div>
        <div className="space-y-1">
          <h4 className="font-bold text-slate-200">
            Diferenciación Técnica: Umbrales Ambientales vs Detección de Anomalías con IA
          </h4>
          <p className="text-slate-400 leading-relaxed text-[11px]">
            • <strong>Umbral Ambiental (Este Panel):</strong> Evalúa en tiempo real si una medición excede límites físicos programados (<span className="text-amber-400">Preventivo</span>, <span className="text-orange-400">Advertencia</span>, <span className="text-rose-400">Crítico</span>). Si se supera, genera un evento de alerta operacional inmediato.<br />
            • <strong>Anomalía Estadística (MineSafe IA):</strong> Evalúa la dispersión de series temporales mediante la desviación mediana absoluta (MAD con <code className="bg-slate-950 px-1 py-0.5 rounded text-indigo-300 font-mono">|Robust Z| ≥ 3.5</code>). Identifica variaciones inusuales en la muestra incluso si no superan un umbral fijo, y no activa sirenas físicas.
          </p>
        </div>
      </div>

      {/* 7. Vista / Historial de Eventos del Motor de Alertas */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-2.5">
            <Bell className="w-4 h-4 text-teal-400" />
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">
              Historial de Eventos del Motor de Alertas ({filteredEvents.length})
            </h3>
          </div>

          {/* Filtros de Eventos */}
          <div className="flex items-center space-x-1 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
            <Filter className="w-3.5 h-3.5 text-slate-400 ml-1.5 mr-1" />
            <button
              onClick={() => setEventFilter('all')}
              className={`px-2.5 py-1 rounded font-semibold transition ${
                eventFilter === 'all' ? 'bg-slate-800 text-white' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Todos ({events.length})
            </button>
            <button
              onClick={() => setEventFilter('activo')}
              className={`px-2.5 py-1 rounded font-semibold transition ${
                eventFilter === 'activo' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Activos ({events.filter((e) => e.estado === 'activo').length})
            </button>
            <button
              onClick={() => setEventFilter('recuperado')}
              className={`px-2.5 py-1 rounded font-semibold transition ${
                eventFilter === 'recuperado' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Recuperados ({events.filter((e) => e.estado === 'recuperado').length})
            </button>
          </div>
        </div>

        {/* Tabla de Eventos */}
        {filteredEvents.length === 0 ? (
          <div className="py-8 text-center text-slate-500 text-xs">
            <CheckCircle2 className="w-8 h-8 mx-auto text-emerald-500/40 mb-2" />
            <p>No hay eventos registrados bajo el filtro seleccionado para este nodo.</p>
            <p className="text-[11px] text-slate-600 mt-0.5">
              Todas las lecturas recibidas se mantienen dentro de los rangos operacionales normales.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 uppercase text-[10px] tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-2.5 px-3">Fecha y Hora</th>
                  <th className="py-2.5 px-3">Variable</th>
                  <th className="py-2.5 px-3">Valor Detectado</th>
                  <th className="py-2.5 px-3">Umbral Superado</th>
                  <th className="py-2.5 px-3">Nivel</th>
                  <th className="py-2.5 px-3">Estado</th>
                  <th className="py-2.5 px-3">Detalle / Resolución</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80 font-mono text-[11px]">
                {filteredEvents.map((ev) => {
                  const isActivo = ev.estado === 'activo';
                  const levelBadge = {
                    preventivo: 'bg-amber-500/20 text-amber-300 border-amber-500/30',
                    advertencia: 'bg-orange-500/20 text-orange-300 border-orange-500/30',
                    critico: 'bg-rose-500/20 text-rose-300 border-rose-500/30',
                    normal: 'bg-slate-800 text-slate-300 border-slate-700',
                  }[ev.nivel];

                  return (
                    <tr key={ev.id} className="hover:bg-slate-800/40 transition">
                      <td className="py-2.5 px-3 text-slate-300 font-sans">
                        {new Date(ev.timestamp).toLocaleString()}
                      </td>
                      <td className="py-2.5 px-3 font-bold text-slate-200">
                        {ev.variableLabel}
                      </td>
                      <td className="py-2.5 px-3 text-white font-bold">
                        {ev.valorMedido} {ev.unidad}
                      </td>
                      <td className="py-2.5 px-3 text-slate-400">
                        {ev.umbralSuperado > 0 ? `${ev.umbralSuperado} ${ev.unidad}` : 'N/A'}
                      </td>
                      <td className="py-2.5 px-3">
                        <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold border uppercase ${levelBadge}`}>
                          {ev.nivel}
                        </span>
                      </td>
                      <td className="py-2.5 px-3">
                        {isActivo ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                            <span className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-pulse mr-1" />
                            Activo
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                            <Check className="w-3 h-3 mr-1" />
                            Recuperado
                          </span>
                        )}
                      </td>
                      <td className="py-2.5 px-3 font-sans text-slate-400 text-[10px] max-w-xs truncate" title={ev.mensaje}>
                        {isActivo ? ev.mensaje : `Resuelto ${ev.fechaResolucion ? new Date(ev.fechaResolucion).toLocaleTimeString() : ''}`}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal de Confirmación para Restaurar Valores de Fábrica */}
      {showConfirmRestoreModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl">
            <div className="flex items-center space-x-3 text-amber-400">
              <div className="p-2 bg-amber-500/10 rounded-xl">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-white">¿Restaurar valores de referencia?</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Esta acción sobrescribirá en Firebase los umbrales configurados para el nodo{' '}
              <strong className="text-white font-mono">{selectedDeviceId}</strong> con los valores iniciales de fábrica de ejemplo.
            </p>
            <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 text-[11px] text-slate-400 space-y-1">
              <p>• PM2.5: Preventivo 45, Advertencia 85, Crítico 130 ADC</p>
              <p>• MQ-2: Preventivo 350, Advertencia 520, Crítico 750 ADC</p>
              <p>• Temperatura: Preventivo 26°C, Advertencia 30°C, Crítico 34°C</p>
              <p>• Humedad: Preventivo 75%, Advertencia 85%, Crítico 92%</p>
            </div>
            <div className="flex items-center justify-end space-x-3 pt-2">
              <button
                onClick={() => setShowConfirmRestoreModal(false)}
                className="px-4 py-2 rounded-lg text-xs font-semibold bg-slate-800 text-slate-300 hover:bg-slate-700 transition"
              >
                Cancelar
              </button>
              <button
                onClick={handleRestoreDefaults}
                disabled={isRestoring}
                className="px-4 py-2 rounded-lg text-xs font-bold bg-amber-600 hover:bg-amber-500 text-white transition flex items-center space-x-1"
              >
                {isRestoring ? (
                  <span>Restaurando...</span>
                ) : (
                  <>
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Confirmar Restauración</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

/**
 * Componente de Tarjeta de Configuración por Variable
 */
interface VariableConfigCardProps {
  title: string;
  subtitle: string;
  icon: React.ReactNode;
  unit: string;
  accentColor: 'purple' | 'amber' | 'orange' | 'cyan';
  currentValue?: number;
  threshold: DeviceAlertConfig['pm25'];
  disabled: boolean;
  onChange: (field: 'preventivo' | 'advertencia' | 'critico', val: string) => void;
  onToggle: (field: 'activo' | 'notificaciones') => void;
}

const VariableConfigCard: React.FC<VariableConfigCardProps> = ({
  title,
  subtitle,
  icon,
  unit,
  currentValue,
  threshold,
  disabled,
  onChange,
  onToggle,
}) => {
  // Cálculo de posición relativa del valor actual respecto a los umbrales
  const hasCurrent = currentValue !== undefined && !isNaN(currentValue);

  return (
    <div
      className={`bg-slate-900 border rounded-xl p-5 transition ${
        threshold.activo ? 'border-slate-800' : 'border-slate-800/50 opacity-70'
      }`}
    >
      {/* Cabecera de la tarjeta */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center space-x-3">
          <div className="p-2 bg-slate-950 rounded-xl border border-slate-800">{icon}</div>
          <div>
            <h3 className="text-sm font-bold text-white">{title}</h3>
            <p className="text-[11px] text-slate-400">{subtitle}</p>
          </div>
        </div>

        {/* Switch de Activo */}
        <label className="flex items-center space-x-2 text-xs font-semibold cursor-pointer select-none">
          <span className="text-[11px] text-slate-400">
            {threshold.activo ? 'Monitoreo ON' : 'Pausado'}
          </span>
          <button
            type="button"
            disabled={disabled}
            onClick={() => onToggle('activo')}
            className={`w-9 h-5 flex items-center rounded-full p-0.5 transition ${
              threshold.activo ? 'bg-teal-600' : 'bg-slate-700'
            } ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
          >
            <div
              className={`bg-white w-4 h-4 rounded-full shadow-md transform transition ${
                threshold.activo ? 'translate-x-4' : 'translate-x-0'
              }`}
            />
          </button>
        </label>
      </div>

      {/* Lectura actual en vivo */}
      <div className="mt-4 p-3 bg-slate-950 rounded-lg border border-slate-800/80 flex items-center justify-between text-xs">
        <div className="flex items-center space-x-2 text-slate-400">
          <Eye className="w-3.5 h-3.5 text-teal-400" />
          <span>Medición Actual en Vivo:</span>
        </div>
        <span className="font-mono font-bold text-slate-200">
          {hasCurrent ? `${currentValue} ${unit}` : 'Sin datos'}
        </span>
      </div>

      {/* Campos de Umbrales: Preventivo, Advertencia, Crítico */}
      <div className="grid grid-cols-3 gap-3 mt-4">
        {/* Preventivo */}
        <div className="space-y-1">
          <label className="text-[10px] font-bold uppercase tracking-wider text-amber-400 block">
            Preventivo
          </label>
          <div className="relative">
            <input
              type="number"
              step="any"
              disabled={disabled || !threshold.activo}
              value={threshold.preventivo}
              onChange={(e) => onChange('preventivo', e.target.value)}
              className="w-full bg-slate-950 border border-amber-500/40 rounded-lg py-1.5 px-2.5 text-xs text-white font-mono font-bold focus:outline-none focus:border-amber-400 disabled:opacity-50"
            />
            <span className="absolute right-2 top-2 text-[10px] text-slate-500 font-mono">
              {unit}
            </span>
          </div>
        </div>

        {/* Advertencia */}
        <div className="space-y-1">
          <label className="text-[10px] font-bold uppercase tracking-wider text-orange-400 block">
            Advertencia
          </label>
          <div className="relative">
            <input
              type="number"
              step="any"
              disabled={disabled || !threshold.activo}
              value={threshold.advertencia}
              onChange={(e) => onChange('advertencia', e.target.value)}
              className="w-full bg-slate-950 border border-orange-500/40 rounded-lg py-1.5 px-2.5 text-xs text-white font-mono font-bold focus:outline-none focus:border-orange-400 disabled:opacity-50"
            />
            <span className="absolute right-2 top-2 text-[10px] text-slate-500 font-mono">
              {unit}
            </span>
          </div>
        </div>

        {/* Crítico */}
        <div className="space-y-1">
          <label className="text-[10px] font-bold uppercase tracking-wider text-rose-400 block">
            Crítico
          </label>
          <div className="relative">
            <input
              type="number"
              step="any"
              disabled={disabled || !threshold.activo}
              value={threshold.critico}
              onChange={(e) => onChange('critico', e.target.value)}
              className="w-full bg-slate-950 border border-rose-500/40 rounded-lg py-1.5 px-2.5 text-xs text-white font-mono font-bold focus:outline-none focus:border-rose-400 disabled:opacity-50"
            />
            <span className="absolute right-2 top-2 text-[10px] text-slate-500 font-mono">
              {unit}
            </span>
          </div>
        </div>
      </div>

      {/* Switch de Notificaciones */}
      <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs">
        <span className="text-[11px] text-slate-400 flex items-center space-x-1.5">
          <Bell className="w-3.5 h-3.5 text-slate-500" />
          <span>Habilitar avisos sonoros y visuales</span>
        </span>
        <button
          type="button"
          disabled={disabled || !threshold.activo}
          onClick={() => onToggle('notificaciones')}
          className={`w-8 h-4 flex items-center rounded-full p-0.5 transition ${
            threshold.notificaciones ? 'bg-amber-600' : 'bg-slate-700'
          } ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
        >
          <div
            className={`bg-white w-3 h-3 rounded-full shadow transform transition ${
              threshold.notificaciones ? 'translate-x-4' : 'translate-x-0'
            }`}
          />
        </button>
      </div>
    </div>
  );
};
