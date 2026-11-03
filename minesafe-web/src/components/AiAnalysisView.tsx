import React, { useState, useEffect } from 'react';
import {
  AnalyzeResponse,
  HealthResponse,
  Observation,
} from '../types/minesafe';
import { aiAnalysisService, AiAnalysisError } from '../services/aiAnalysisService';
import { firebaseMiningService } from '../services/firebaseService';
import {
  Brain,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Minus,
  CheckCircle2,
  XCircle,
  RefreshCw,
  Download,
  Info,
  Clock,
  Cpu,
  Layers,
  ShieldAlert,
} from 'lucide-react';

interface AiAnalysisViewProps {
  selectedDeviceId: string;
  availableDevices: Array<{ deviceId: string; nombre?: string }>;
  onSelectDevice: (deviceId: string) => void;
}

export const AiAnalysisView: React.FC<AiAnalysisViewProps> = ({
  selectedDeviceId,
  availableDevices,
  onSelectDevice,
}) => {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [healthLoading, setHealthLoading] = useState<boolean>(true);
  const [healthError, setHealthError] = useState<string | null>(null);

  const [analysis, setAnalysis] = useState<AnalyzeResponse | null>(null);
  const [observations, setObservations] = useState<Observation[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [limitationsNotice, setLimitationsNotice] = useState<string | null>(null);

  // Comprobar salud de la API MineSafe IA
  const checkApiHealth = async () => {
    setHealthLoading(true);
    setHealthError(null);
    try {
      const res = await aiAnalysisService.checkHealth();
      setHealth(res);
    } catch (e: any) {
      setHealthError(e.message || 'Servicio de IA desconectado');
      setHealth(null);
    } finally {
      setHealthLoading(false);
    }
  };

  useEffect(() => {
    checkApiHealth();
  }, []);

  // Cargar datos reales y solicitar análisis
  const runAnalysis = async () => {
    if (!selectedDeviceId) return;

    setLoading(true);
    setError(null);
    setLimitationsNotice(null);

    try {
      // 1. Obtener lecturas reales desde Firebase
      const realPoints = await firebaseMiningService.fetchRealObservations(selectedDeviceId, 100);
      setObservations(realPoints);

      if (realPoints.length < 5) {
        setLimitationsNotice(
          `Se encontraron solo ${realPoints.length} observación(es) real(es) para el dispositivo ${selectedDeviceId}. El algoritmo estadístico de MineSafe IA requiere un mínimo de 5 lecturas reales en la serie temporal para calcular la mediana y tendencia.`
        );
        setAnalysis(null);
        setLoading(false);
        return;
      }

      // 2. Enviar a la API de análisis estadístico
      const response = await aiAnalysisService.analyzeSeries({
        device_id: selectedDeviceId,
        observations: realPoints,
      });

      setAnalysis(response);
    } catch (err: any) {
      if (err instanceof AiAnalysisError) {
        setError(`${err.message} (Código: ${err.code || err.statusCode || 'ERROR'})`);
      } else {
        setError(err.message || 'Ocurrió un error inesperado al analizar la serie temporal.');
      }
      setAnalysis(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedDeviceId) {
      runAnalysis();
    }
  }, [selectedDeviceId]);

  // Cargar serie de demostración documentada explícitamente como sintética para pruebas
  const loadExampleTestSeries = async () => {
    setLoading(true);
    setError(null);
    setLimitationsNotice(
      '⚠️ ATENCIÓN: Se ha cargado la serie de prueba documentada (EJEMPLO_SOLICITUD.json) con fines de demostración técnica local. Estas mediciones no corresponden a registros operativos reales de la faena.'
    );

    const demoSeries: Observation[] = [
      { timestamp: '2026-10-29T12:00:00Z', pm25_raw: 450, mq2_raw: 200, temperature_c: 22.1, humidity_pct: 48, unit: 'raw' },
      { timestamp: '2026-10-29T12:00:05Z', pm25_raw: 452, mq2_raw: 202, temperature_c: 22.2, humidity_pct: 48.5, unit: 'raw' },
      { timestamp: '2026-10-29T12:00:10Z', pm25_raw: 455, mq2_raw: 201, temperature_c: 22.2, humidity_pct: 49, unit: 'raw' },
      { timestamp: '2026-10-29T12:00:15Z', pm25_raw: 453, mq2_raw: 204, temperature_c: 22.3, humidity_pct: 49.2, unit: 'raw' },
      { timestamp: '2026-10-29T12:00:20Z', pm25_raw: 900, mq2_raw: 205, temperature_c: 22.4, humidity_pct: 49.5, unit: 'raw' },
      { timestamp: '2026-10-29T12:00:25Z', pm25_raw: 454, mq2_raw: 203, temperature_c: 22.4, humidity_pct: 49.4, unit: 'raw' },
    ];

    setObservations(demoSeries);

    try {
      const response = await aiAnalysisService.analyzeSeries({
        device_id: 'NODO_DEMO_TEST',
        observations: demoSeries,
      });
      setAnalysis(response);
    } catch (err: any) {
      setError(err.message || 'Error al enviar serie de prueba.');
    } finally {
      setLoading(false);
    }
  };

  // Descarga de informe estructurado
  const downloadReport = () => {
    if (!analysis) return;

    const reportContent = {
      informe: 'MineSafe AI Analysis Report',
      version_api: health?.version || '0.1.1',
      dispositivo: analysis.device_id,
      generado_en_utc: analysis.generated_at,
      ventana_observacion: {
        inicio: analysis.window_start,
        fin: analysis.window_end,
        total_observaciones: analysis.observations_count,
      },
      metodo_analisis: analysis.analysis_method,
      tendencias_metricas: analysis.trends,
      anomalias_detectadas: analysis.anomalies,
      limitaciones_legales_y_tecnicas: analysis.limitations,
      aviso_seguridad:
        'Este informe contiene análisis estadístico experimental (mediana/MAD y regresión lineal). No constituye alarma certificada de seguridad ni reemplaza detectores certificados bajo D.S. 132.',
    };

    const blob = new Blob([JSON.stringify(reportContent, null, 2)], {
      type: 'application/json;charset=utf-8',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `minesafe-ai-report-${analysis.device_id}-${new Date().toISOString().slice(0, 10)}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      {/* Encabezado y Estado del Backend */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center space-x-3">
            <div className="p-3 bg-teal-500/10 text-teal-400 rounded-lg border border-teal-500/20">
              <Brain className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h2 className="text-lg font-bold text-slate-100">MineSafe IA Analysis</h2>
                <span className="px-2 py-0.5 text-xs font-semibold rounded bg-teal-500/10 text-teal-300 border border-teal-500/20">
                  v0.1.1
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Análisis estadístico de series temporales (Detección de anomalías MAD y tendencias lineales)
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {/* Indicador de Salud del Backend */}
            <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-800 text-xs">
              {healthLoading ? (
                <>
                  <RefreshCw className="w-3.5 h-3.5 animate-spin text-slate-400" />
                  <span className="text-slate-400">Verificando API...</span>
                </>
              ) : health ? (
                <>
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <span className="text-emerald-300 font-medium">API Conectada</span>
                </>
              ) : (
                <>
                  <XCircle className="w-4 h-4 text-rose-400" />
                  <span className="text-rose-300 font-medium" title={healthError || undefined}>
                    {healthError ? 'API Desconectada' : 'API Desconectada'}
                  </span>
                </>
              )}
              <button
                onClick={checkApiHealth}
                title="Reintentar conexión con API"
                className="ml-1 text-slate-400 hover:text-slate-200 transition"
              >
                <RefreshCw className="w-3 h-3" />
              </button>
            </div>

            {/* Selector de Dispositivo */}
            <select
              value={selectedDeviceId}
              onChange={(e) => onSelectDevice(e.target.value)}
              className="bg-slate-950 border border-slate-700 text-slate-200 text-xs rounded-lg px-3 py-2 font-medium focus:outline-none focus:border-teal-500"
            >
              {availableDevices.map((dev) => (
                <option key={dev.deviceId} value={dev.deviceId}>
                  {dev.nombre || dev.deviceId}
                </option>
              ))}
            </select>

            <button
              onClick={runAnalysis}
              disabled={loading}
              className="flex items-center space-x-1.5 bg-teal-600 hover:bg-teal-500 text-white px-3 py-2 rounded-lg text-xs font-semibold transition disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span>{loading ? 'Analizando...' : 'Analizar Serie'}</span>
            </button>
          </div>
        </div>

        {/* Advertencia técnica de seguridad */}
        <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-start space-x-2 text-xs text-amber-300/90 bg-amber-500/5 p-2.5 rounded-lg border border-amber-500/20">
          <Info className="w-4 h-4 flex-shrink-0 mt-0.5 text-amber-400" />
          <p>
            <strong>Aviso de Seguridad Operativa:</strong> La detección estadística se basa en desviación mediana absoluta (MAD) sobre la serie observada. No constituye un sistema de alarma certificado bajo la norma minera D.S. 132 ni reemplaza las alertas automáticas de umbral directo de los detectores portátiles.
          </p>
        </div>
      </div>

      {/* Notificación de limitaciones o error */}
      {error && (
        <div className="bg-rose-950/40 border border-rose-800 text-rose-200 p-4 rounded-xl text-sm flex items-start space-x-3">
          <AlertTriangle className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h4 className="font-semibold text-rose-300">Error en el análisis de IA</h4>
            <p className="text-xs text-rose-200/90 mt-1">{error}</p>
            <div className="mt-3 flex items-center space-x-2">
              <button
                onClick={runAnalysis}
                className="px-2.5 py-1 text-xs bg-rose-800 hover:bg-rose-700 text-white rounded font-medium transition"
              >
                Reintentar
              </button>
              <button
                onClick={loadExampleTestSeries}
                className="px-2.5 py-1 text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 rounded font-medium transition"
              >
                Probar con Serie de Demostración Local
              </button>
            </div>
          </div>
        </div>
      )}

      {limitationsNotice && (
        <div className="bg-amber-950/30 border border-amber-800/60 text-amber-200 p-4 rounded-xl text-sm flex items-start space-x-3">
          <Info className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h4 className="font-semibold text-amber-300">Condición de Datos en Firebase</h4>
            <p className="text-xs text-amber-200/90 mt-1">{limitationsNotice}</p>
            <div className="mt-3">
              <button
                onClick={loadExampleTestSeries}
                className="px-3 py-1.5 text-xs bg-slate-800 hover:bg-slate-700 text-slate-100 rounded-lg font-medium border border-slate-700 transition"
              >
                Cargar Serie de Prueba Documentada (EJEMPLO_SOLICITUD.json)
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SECCIÓN A: Resumen del Análisis */}
      {analysis && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
              <div className="flex items-center space-x-2 text-slate-400 text-xs mb-1">
                <Cpu className="w-4 h-4 text-teal-400" />
                <span>Dispositivo Analizado</span>
              </div>
              <p className="text-base font-bold text-slate-100 truncate">{analysis.device_id}</p>
              <span className="text-[10px] text-slate-400 font-mono mt-1 block">
                Método: {analysis.analysis_method}
              </span>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
              <div className="flex items-center space-x-2 text-slate-400 text-xs mb-1">
                <Layers className="w-4 h-4 text-cyan-400" />
                <span>Observaciones Válidas</span>
              </div>
              <p className="text-base font-bold text-slate-100">{analysis.observations_count} registros</p>
              <span className="text-[10px] text-slate-400 mt-1 block">
                Unidades analizadas: {observations[0]?.unit === 'raw' ? 'ADC Crudo (raw)' : 'Calibradas'}
              </span>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
              <div className="flex items-center space-x-2 text-slate-400 text-xs mb-1">
                <Clock className="w-4 h-4 text-indigo-400" />
                <span>Ventana Temporal</span>
              </div>
              <p className="text-xs font-semibold text-slate-200">
                {new Date(analysis.window_start).toLocaleTimeString()} - {new Date(analysis.window_end).toLocaleTimeString()}
              </p>
              <span className="text-[10px] text-slate-400 mt-1 block truncate">
                {new Date(analysis.window_start).toLocaleDateString()}
              </span>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
              <div className="flex items-center space-x-2 text-slate-400 text-xs mb-1">
                <ShieldAlert className="w-4 h-4 text-amber-400" />
                <span>Anomalías Estadísticas</span>
              </div>
              <div className="flex items-baseline space-x-2">
                <p className={`text-base font-bold ${analysis.anomalies.length > 0 ? 'text-amber-400' : 'text-emerald-400'}`}>
                  {analysis.anomalies.length}
                </p>
                <span className="text-xs text-slate-400">
                  {analysis.anomalies.length === 0 ? 'Serie regular' : 'Atípicos detectados'}
                </span>
              </div>
              <span className="text-[10px] text-slate-400 mt-1 block">
                Z-Score umbral |Z| ≥ 3.5
              </span>
            </div>
          </div>

          {/* SECCIÓN B: Detección de Anomalías */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-2">
                <AlertTriangle className="w-5 h-5 text-amber-400" />
                <h3 className="text-sm font-bold text-slate-100 uppercase tracking-wider">
                  Detección de Anomalías Estadísticas
                </h3>
              </div>
              <span className="text-xs text-slate-400">
                Diferenciación obligatoria: No implica necesariamente un peligro físico confirmado
              </span>
            </div>

            {analysis.anomalies.length === 0 ? (
              <div className="bg-slate-950/60 border border-slate-800 rounded-lg p-6 text-center">
                <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto mb-2" />
                <p className="text-sm font-semibold text-slate-200">No se detectaron valores anómalos</p>
                <p className="text-xs text-slate-400 mt-1 max-w-lg mx-auto">
                  Todas las lecturas de la serie se encuentran dentro de ±3.5 desviaciones medianas absolutas (MAD) respecto del centro de la ventana.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400 uppercase font-mono text-[11px]">
                      <th className="py-2.5 px-3">Fecha y Hora (UTC)</th>
                      <th className="py-2.5 px-3">Variable</th>
                      <th className="py-2.5 px-3">Valor Observado</th>
                      <th className="py-2.5 px-3">Mediana Ventana</th>
                      <th className="py-2.5 px-3">Robust Z-Score</th>
                      <th className="py-2.5 px-3">Explicación Estadística</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-mono">
                    {analysis.anomalies.map((anom, idx) => (
                      <tr key={idx} className="hover:bg-slate-800/30 transition">
                        <td className="py-2.5 px-3 text-slate-300">
                          {new Date(anom.timestamp).toLocaleTimeString()} ({new Date(anom.timestamp).toLocaleDateString()})
                        </td>
                        <td className="py-2.5 px-3 font-semibold text-teal-300">
                          {anom.metric}
                        </td>
                        <td className="py-2.5 px-3 text-rose-300 font-bold">
                          {anom.value} <span className="text-[10px] text-slate-400 font-sans">{anom.unit}</span>
                        </td>
                        <td className="py-2.5 px-3 text-slate-400">
                          {anom.median}
                        </td>
                        <td className="py-2.5 px-3">
                          <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-rose-500/10 text-rose-300 border border-rose-500/30">
                            {anom.robust_z_score > 0 ? `+${anom.robust_z_score}` : anom.robust_z_score}
                          </span>
                        </td>
                        <td className="py-2.5 px-3 text-slate-300 font-sans text-xs">
                          {anom.explanation}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* SECCIÓN C: Análisis de Tendencias */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-2">
                <TrendingUp className="w-5 h-5 text-teal-400" />
                <h3 className="text-sm font-bold text-slate-100 uppercase tracking-wider">
                  Análisis de Tendencias por Métrica
                </h3>
              </div>
              <span className="text-xs text-slate-400">Regresión lineal de primer orden</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {analysis.trends.map((t) => {
                const isIncreasing = t.direction === 'increasing';
                const isDecreasing = t.direction === 'decreasing';
                const isStable = t.direction === 'stable';

                return (
                  <div
                    key={t.metric}
                    className="bg-slate-950 border border-slate-800 rounded-lg p-4 space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-200 uppercase font-mono">
                        {t.metric}
                      </span>
                      <div className="flex items-center space-x-1">
                        {isIncreasing && (
                          <span className="flex items-center text-xs font-bold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20">
                            <TrendingUp className="w-3.5 h-3.5 mr-1" />
                            Aumento
                          </span>
                        )}
                        {isDecreasing && (
                          <span className="flex items-center text-xs font-bold text-cyan-400 bg-cyan-500/10 px-2 py-0.5 rounded border border-cyan-500/20">
                            <TrendingDown className="w-3.5 h-3.5 mr-1" />
                            Disminución
                          </span>
                        )}
                        {isStable && (
                          <span className="flex items-center text-xs font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                            <Minus className="w-3.5 h-3.5 mr-1" />
                            Estable
                          </span>
                        )}
                        {t.direction === 'insufficient_data' && (
                          <span className="text-xs text-slate-400 bg-slate-800 px-2 py-0.5 rounded">
                            Sin datos
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                      <div className="bg-slate-900 p-2 rounded border border-slate-800/80">
                        <span className="text-[10px] text-slate-400 block font-sans">Primer Valor</span>
                        <span className="text-slate-200 font-semibold">{t.first_value}</span>
                      </div>
                      <div className="bg-slate-900 p-2 rounded border border-slate-800/80">
                        <span className="text-[10px] text-slate-400 block font-sans">Último Valor</span>
                        <span className="text-slate-200 font-semibold">{t.last_value}</span>
                      </div>
                    </div>

                    <div className="text-[11px] text-slate-400 pt-1 border-t border-slate-800">
                      Pendiente estimada:{' '}
                      <strong className="text-slate-200 font-mono">
                        {t.slope_per_hour !== null ? `${t.slope_per_hour}/hora` : 'N/A'}
                      </strong>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* SECCIÓN D: Limitaciones y Descarga de Informes */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <h3 className="text-sm font-bold text-slate-100 uppercase tracking-wider">
                  Informe Técnico y Limitaciones Declaradas
                </h3>
                <p className="text-xs text-slate-400">
                  Conclusiones técnicas calculadas por la API bajo transparencia metodológica
                </p>
              </div>

              <button
                onClick={downloadReport}
                className="flex items-center space-x-2 bg-slate-800 hover:bg-slate-700 text-slate-100 px-3.5 py-2 rounded-lg text-xs font-bold border border-slate-700 transition"
              >
                <Download className="w-4 h-4 text-teal-400" />
                <span>Descargar Informe Técnico (.json)</span>
              </button>
            </div>

            <ul className="space-y-1.5 text-xs text-slate-300">
              {analysis.limitations.map((lim, i) => (
                <li key={i} className="flex items-start space-x-2">
                  <span className="text-teal-400 font-bold">•</span>
                  <span>{lim}</span>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </div>
  );
};
