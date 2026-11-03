/**
 * Contratos de datos de MineSafe y de la API MineSafe IA Analysis (v0.1.1).
 */

export interface Observation {
  timestamp: string; // ISO-8601 UTC (ej: "2026-10-29T12:00:00Z")
  pm25_raw?: number | null;
  mq2_raw?: number | null;
  temperature_c?: number | null;
  humidity_pct?: number | null;
  unit: 'raw' | 'calibrated';
}

export interface AnalyzeRequest {
  device_id: string;
  observations: Observation[];
}

export interface MetricTrend {
  metric: string;
  first_value: number;
  last_value: number;
  slope_per_hour: number | null;
  direction: 'increasing' | 'decreasing' | 'stable' | 'insufficient_data';
  unit: string;
}

export interface Anomaly {
  timestamp: string;
  metric: string;
  value: number;
  median: number;
  robust_z_score: number;
  unit: string;
  explanation: string;
}

export interface AnalyzeResponse {
  device_id: string;
  generated_at: string;
  window_start: string;
  window_end: string;
  observations_count: number;
  analysis_method: string;
  trends: MetricTrend[];
  anomalies: Anomaly[];
  limitations: string[];
}

export interface HealthResponse {
  status: string;
  service: string;
  version: string;
}

export interface DeviceTelemetry {
  deviceId: string;
  nombre?: string;
  temperatura?: number;
  humedad?: number;
  mq135_adc?: number;
  mq2_adc?: number;
  pm25_adc?: number;
  alarma?: boolean;
  luz?: boolean;
  ultimoReporte?: number;
}

export type AlertLevel = 'normal' | 'preventivo' | 'advertencia' | 'critico';

export interface VariableThresholdConfig {
  activo: boolean;
  preventivo: number;
  advertencia: number;
  critico: number;
  notificaciones: boolean;
  unidad: string;
  descripcion?: string;
}

export interface DeviceAlertConfig {
  dispositivo_id: string;
  pm25: VariableThresholdConfig;
  mq2: VariableThresholdConfig;
  temperatura: VariableThresholdConfig;
  humedad: VariableThresholdConfig;
  ultimoCambio?: {
    fecha: string;
    usuario: string;
    rol: string;
    motivo?: string;
  };
}

export interface AlertEvent {
  id: string;
  timestamp: string;
  deviceId: string;
  variable: 'pm25' | 'mq2' | 'temperatura' | 'humedad';
  variableLabel: string;
  valorMedido: number;
  unidad: string;
  umbralSuperado: number;
  nivel: 'preventivo' | 'advertencia' | 'critico';
  estado: 'activo' | 'recuperado';
  mensaje: string;
  fechaResolucion?: string;
}

export interface VariableEvaluation {
  variable: 'pm25' | 'mq2' | 'temperatura' | 'humedad';
  label: string;
  value: number | null;
  unit: string;
  level: AlertLevel;
  thresholdExceeded?: number;
  active: boolean;
}

export interface EvaluationResult {
  deviceId: string;
  timestamp: string;
  overallLevel: AlertLevel;
  variables: VariableEvaluation[];
  newEventsGenerated: AlertEvent[];
  resolvedEvents: AlertEvent[];
}
