import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AiAnalysisService, AiAnalysisError } from './aiAnalysisService';
import { AnalyzeRequest, Observation } from '../types/minesafe';

describe('AiAnalysisService Integration Tests', () => {
  let service: AiAnalysisService;

  beforeEach(() => {
    vi.restoreAllMocks();
    service = new AiAnalysisService({
      baseUrl: 'http://127.0.0.1:8000',
      apiKey: 'test-secret-key',
      timeoutMs: 5000,
    });
  });

  it('1. GET /health: debe responder con estado ok y versión 0.1.1', async () => {
    const mockHealth = {
      status: 'ok',
      service: 'minesafe-ai-analysis',
      version: '0.1.1',
    };

    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockHealth,
    });

    const result = await service.checkHealth();
    expect(result.status).toBe('ok');
    expect(result.version).toBe('0.1.1');
    expect(global.fetch).toHaveBeenCalledWith('http://127.0.0.1:8000/health', expect.any(Object));
  });

  it('2. POST /v1/analyze: debe procesar exitosamente una serie válida', async () => {
    const mockResponse = {
      device_id: 'NODO_U-01',
      generated_at: '2026-10-29T12:00:30Z',
      window_start: '2026-10-29T12:00:00Z',
      window_end: '2026-10-29T12:00:25Z',
      observations_count: 6,
      analysis_method: 'robust_median_mad_and_linear_trend_v1',
      trends: [
        {
          metric: 'pm25_raw',
          first_value: 450,
          last_value: 454,
          slope_per_hour: 12.5,
          direction: 'increasing',
          unit: 'raw ADC / unit from payload',
        },
      ],
      anomalies: [
        {
          timestamp: '2026-10-29T12:00:20Z',
          metric: 'pm25_raw',
          value: 900,
          median: 453.5,
          robust_z_score: 5.2,
          unit: 'raw',
          explanation: 'Valor atípico estadístico respecto de esta ventana; requiere revisión humana.',
        },
      ],
      limitations: [
        'La detección estadística no está validada como alarma de seguridad ni como predicción de incidentes.',
      ],
    };

    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse,
    });

    const request: AnalyzeRequest = {
      device_id: 'NODO_U-01',
      observations: [
        { timestamp: '2026-10-29T12:00:00Z', pm25_raw: 450, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:05Z', pm25_raw: 452, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:10Z', pm25_raw: 455, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:15Z', pm25_raw: 453, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:20Z', pm25_raw: 900, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:25Z', pm25_raw: 454, unit: 'raw' },
      ],
    };

    const response = await service.analyzeSeries(request);
    expect(response.device_id).toBe('NODO_U-01');
    expect(response.anomalies.length).toBe(1);
    expect(response.anomalies[0].value).toBe(900);
    expect(response.trends.length).toBe(1);
    expect(response.trends[0].direction).toBe('increasing');
  });

  it('3. Error HTTP 401: debe lanzar AiAnalysisError con código AUTH_ERROR', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ detail: 'No autorizado. Se requiere Bearer clave válida' }),
    });

    const request: AnalyzeRequest = {
      device_id: 'NODO_U-01',
      observations: [
        { timestamp: '2026-10-29T12:00:00Z', pm25_raw: 450, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:05Z', pm25_raw: 452, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:10Z', pm25_raw: 455, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:15Z', pm25_raw: 453, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:20Z', pm25_raw: 454, unit: 'raw' },
      ],
    };

    await expect(service.analyzeSeries(request)).rejects.toThrow(AiAnalysisError);
    try {
      await service.analyzeSeries(request);
    } catch (err: any) {
      expect(err.statusCode).toBe(401);
      expect(err.code).toBe('AUTH_ERROR');
    }
  });

  it('4. Error HTTP 413: debe manejar serie que supera el máximo permitido', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 413,
      json: async () => ({ detail: 'La serie supera el máximo permitido' }),
    });

    const request: AnalyzeRequest = {
      device_id: 'NODO_U-01',
      observations: Array(10).fill(null).map((_, i) => ({
        timestamp: `2026-10-29T12:00:${i.toString().padStart(2, '0')}Z`,
        pm25_raw: 100,
        unit: 'raw',
      })),
    };

    try {
      await service.analyzeSeries(request);
    } catch (err: any) {
      expect(err.statusCode).toBe(413);
      expect(err.code).toBe('PAYLOAD_TOO_LARGE');
    }
  });

  it('5. Validación local: debe rechazar series con menos de 5 observaciones sin llamar a la red', async () => {
    const fetchSpy = vi.fn();
    global.fetch = fetchSpy;

    const shortRequest: AnalyzeRequest = {
      device_id: 'NODO_U-01',
      observations: [
        { timestamp: '2026-10-29T12:00:00Z', pm25_raw: 450, unit: 'raw' },
        { timestamp: '2026-10-29T12:00:05Z', pm25_raw: 452, unit: 'raw' },
      ],
    };

    await expect(service.analyzeSeries(shortRequest)).rejects.toThrow(
      'Se requieren al menos 5 observaciones'
    );
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('6. Normalización: debe eliminar timestamps duplicados y ordenar cronológicamente', () => {
    const rawObs: Observation[] = [
      { timestamp: '2026-10-29T12:00:10Z', pm25_raw: 300, unit: 'raw' },
      { timestamp: '2026-10-29T12:00:00Z', pm25_raw: 100, unit: 'raw' },
      { timestamp: '2026-10-29T12:00:00Z', pm25_raw: 999, unit: 'raw' }, // Duplicado
      { timestamp: '2026-10-29T12:00:05Z', pm25_raw: 200, unit: 'raw' },
    ];

    const result = service.prepareObservations(rawObs);
    expect(result.length).toBe(3); // Duplicado eliminado
    expect(result[0].timestamp).toBe('2026-10-29T12:00:00.000Z');
    expect(result[1].timestamp).toBe('2026-10-29T12:00:05.000Z');
    expect(result[2].timestamp).toBe('2026-10-29T12:00:10.000Z');
  });

  it('7. Network Error: debe capturar falla de conexión cuando el servidor está apagado', async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error('Failed to fetch'));

    await expect(service.checkHealth()).rejects.toThrow(
      'No se pudo conectar con el servicio MineSafe IA'
    );
  });
});
