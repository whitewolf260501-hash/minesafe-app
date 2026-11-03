import {
  AnalyzeRequest,
  AnalyzeResponse,
  HealthResponse,
  Observation,
} from '../types/minesafe';

export class AiAnalysisError extends Error {
  constructor(
    message: string,
    public statusCode?: number,
    public code?: 'AUTH_ERROR' | 'PAYLOAD_TOO_LARGE' | 'VALIDATION_ERROR' | 'SERVICE_UNAVAILABLE' | 'TIMEOUT' | 'NETWORK_ERROR' | 'PARSE_ERROR'
  ) {
    super(message);
    this.name = 'AiAnalysisError';
  }
}

/**
 * Servicio cliente para interactuar con la API MineSafe IA Analysis (FastAPI).
 */
export class AiAnalysisService {
  private baseUrl: string;
  private directApiKey: string | null;
  private timeoutMs: number;

  constructor(options?: {
    baseUrl?: string;
    apiKey?: string;
    timeoutMs?: number;
  }) {
    // Si no se especifica, toma la variable de entorno o usa el endpoint relativo '/api/ai'
    // (el proxy de Vite redirige '/api/ai' a la URL configurada y añade el header Authorization en el servidor local)
    this.baseUrl =
      options?.baseUrl ||
      (typeof import.meta !== 'undefined' && import.meta.env?.VITE_AI_ANALYSIS_API_URL) ||
      '/api/ai';

    // Permite pasar una clave explícita solo en entornos seguros (ej: backend-to-backend o tests locales)
    this.directApiKey = options?.apiKey || null;
    this.timeoutMs = options?.timeoutMs || 12000;
  }

  /**
   * Verifica el estado del servicio de IA (GET /health).
   */
  async checkHealth(): Promise<HealthResponse> {
    const url = `${this.baseUrl.replace(/\/+$/, '')}/health`;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);

    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: { Accept: 'application/json' },
        signal: controller.signal,
      });

      clearTimeout(timer);

      if (!response.ok) {
        throw new AiAnalysisError(
          `Servicio de IA respondió con estado HTTP ${response.status}`,
          response.status,
          'SERVICE_UNAVAILABLE'
        );
      }

      const data = (await response.json()) as HealthResponse;
      return data;
    } catch (err: unknown) {
      clearTimeout(timer);
      if (err instanceof AiAnalysisError) throw err;
      if (err instanceof Error && err.name === 'AbortError') {
        throw new AiAnalysisError(
          'Tiempo de espera agotado al conectar con el servicio de IA.',
          408,
          'TIMEOUT'
        );
      }
      throw new AiAnalysisError(
        'No se pudo conectar con el servicio MineSafe IA. Verifica que el servidor FastAPI esté iniciado.',
        0,
        'NETWORK_ERROR'
      );
    }
  }

  /**
   * Envía una serie temporal a analizar a POST /v1/analyze.
   */
  async analyzeSeries(request: AnalyzeRequest): Promise<AnalyzeResponse> {
    if (!request.device_id || request.device_id.trim().length === 0) {
      throw new AiAnalysisError('El identificador del dispositivo (device_id) es obligatorio.', 400, 'VALIDATION_ERROR');
    }

    if (!request.observations || request.observations.length < 5) {
      throw new AiAnalysisError(
        `Se requieren al menos 5 observaciones para el análisis estadístico (se recibieron ${request.observations?.length || 0}).`,
        422,
        'VALIDATION_ERROR'
      );
    }

    // Normalizar y ordenar observaciones por marca temporal
    const normalized = this.prepareObservations(request.observations);

    const payload: AnalyzeRequest = {
      device_id: request.device_id.trim(),
      observations: normalized,
    };

    const url = `${this.baseUrl.replace(/\/+$/, '')}/v1/analyze`;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    };

    // Si se configuró clave directa (solo para testing o servidor seguro)
    if (this.directApiKey) {
      headers['Authorization'] = `Bearer ${this.directApiKey}`;
    }

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
        signal: controller.signal,
      });

      clearTimeout(timer);

      if (!response.ok) {
        let detail = `Error HTTP ${response.status}`;
        try {
          const errJson = await response.json();
          detail = errJson.detail || detail;
        } catch {
          // ignore json parse error
        }

        if (response.status === 401) {
          throw new AiAnalysisError(
            `No autorizado (401): ${detail}. Revisa MINESAFE_AI_API_KEY.`,
            401,
            'AUTH_ERROR'
          );
        } else if (response.status === 413) {
          throw new AiAnalysisError(
            `La serie temporal supera el máximo de observaciones permitido: ${detail}`,
            413,
            'PAYLOAD_TOO_LARGE'
          );
        } else if (response.status === 422) {
          throw new AiAnalysisError(
            `Datos de la serie inválidos (422): ${detail}`,
            422,
            'VALIDATION_ERROR'
          );
        } else if (response.status === 503) {
          throw new AiAnalysisError(
            `El servicio de IA no está disponible o no tiene configurada la clave en el servidor: ${detail}`,
            503,
            'SERVICE_UNAVAILABLE'
          );
        }

        throw new AiAnalysisError(detail, response.status);
      }

      const data = (await response.json()) as AnalyzeResponse;
      return data;
    } catch (err: unknown) {
      clearTimeout(timer);
      if (err instanceof AiAnalysisError) throw err;
      if (err instanceof Error && err.name === 'AbortError') {
        throw new AiAnalysisError(
          'Tiempo de espera agotado al consultar el análisis a MineSafe IA.',
          408,
          'TIMEOUT'
        );
      }
      throw new AiAnalysisError(
        'Error de red al enviar la serie a MineSafe IA. Verifica la conectividad.',
        0,
        'NETWORK_ERROR'
      );
    }
  }

  /**
   * Normaliza y valida una lista de observaciones antes de enviar a la API:
   * - Elimina timestamps duplicados (exigencia del backend FastAPI).
   * - Valida que contenga formato ISO-8601 UTC con 'Z'.
   * - Asegura que 'unit' sea 'raw' salvo confirmación de calibración.
   * - Ordena cronológicamente.
   */
  prepareObservations(rawObservations: Observation[]): Observation[] {
    const seen = new Set<string>();
    const valid: Observation[] = [];

    for (const obs of rawObservations) {
      if (!obs.timestamp) continue;

      let isoTimestamp = obs.timestamp;
      try {
        const d = new Date(obs.timestamp);
        if (isNaN(d.getTime())) continue;
        // Garantizar formato UTC exacto con 'Z'
        isoTimestamp = d.toISOString();
      } catch {
        continue;
      }

      if (seen.has(isoTimestamp)) continue;
      seen.add(isoTimestamp);

      valid.push({
        timestamp: isoTimestamp,
        pm25_raw: obs.pm25_raw !== undefined && obs.pm25_raw !== null && isFinite(obs.pm25_raw) ? Number(obs.pm25_raw) : null,
        mq2_raw: obs.mq2_raw !== undefined && obs.mq2_raw !== null && isFinite(obs.mq2_raw) ? Number(obs.mq2_raw) : null,
        temperature_c: obs.temperature_c !== undefined && obs.temperature_c !== null && isFinite(obs.temperature_c) ? Number(obs.temperature_c) : null,
        humidity_pct: obs.humidity_pct !== undefined && obs.humidity_pct !== null && isFinite(obs.humidity_pct) ? Number(obs.humidity_pct) : null,
        unit: obs.unit === 'calibrated' ? 'calibrated' : 'raw',
      });
    }

    return valid.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
  }
}

export const aiAnalysisService = new AiAnalysisService();
