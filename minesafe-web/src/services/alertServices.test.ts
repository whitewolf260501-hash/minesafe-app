import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AlertConfigService } from './alertConfigService';
import { AlertEngineService } from './alertEngineService';
import { DeviceTelemetry, DeviceAlertConfig } from '../types/minesafe';

// Mock de Firebase Database
vi.mock('firebase/database', () => ({
  getDatabase: vi.fn(() => ({})),
  ref: vi.fn((_db, path) => ({ path })),
  set: vi.fn(() => Promise.resolve()),
  update: vi.fn(() => Promise.resolve()),
  onValue: vi.fn(),
}));

describe('Servicio de Configuración Dinámica de Umbrales (AlertConfigService)', () => {
  let configService: AlertConfigService;

  beforeEach(() => {
    vi.clearAllMocks();
    configService = new AlertConfigService();
  });

  it('1. Genera configuración inicial con las 4 variables requeridas y valores de ejemplo', () => {
    const config = configService.getDefaultConfig('device_ESP32_TEST');

    expect(config.dispositivo_id).toBe('device_ESP32_TEST');
    expect(config.pm25).toBeDefined();
    expect(config.mq2).toBeDefined();
    expect(config.temperatura).toBeDefined();
    expect(config.humedad).toBeDefined();

    expect(config.pm25.unidad).toBe('ADC (raw)');
    expect(config.mq2.unidad).toBe('ADC (raw)');
    expect(config.temperatura.unidad).toBe('°C');
    expect(config.humedad.unidad).toBe('%');

    // Comprobar orden lógico por defecto
    expect(config.pm25.preventivo).toBeLessThan(config.pm25.advertencia);
    expect(config.pm25.advertencia).toBeLessThan(config.pm25.critico);
  });

  it('2. Valida correctamente rangos coherentes e inválidos (preventivo < advertencia < crítico)', () => {
    const validConfig = configService.getDefaultConfig('device_TEST');
    const validCheck = configService.validateThresholds(validConfig);
    expect(validCheck.valid).toBe(true);
    expect(validCheck.errors).toHaveLength(0);

    // Caso inválido: preventivo mayor que advertencia
    const invalidConfig: DeviceAlertConfig = {
      ...validConfig,
      pm25: {
        ...validConfig.pm25,
        preventivo: 150,
        advertencia: 100, // Menor que preventivo -> Error
        critico: 200,
      },
    };

    const invalidCheck = configService.validateThresholds(invalidConfig);
    expect(invalidCheck.valid).toBe(false);
    expect(invalidCheck.errors.some((e) => e.includes('Preventivo'))).toBe(true);
  });

  it('3. Rechaza valores negativos o NaN en umbrales', () => {
    const config = configService.getDefaultConfig('device_TEST');
    config.temperatura.preventivo = -5;

    const result = configService.validateThresholds(config);
    expect(result.valid).toBe(false);
    expect(result.errors.some((e) => e.includes('negativos'))).toBe(true);
  });

  it('4. Valida permisos: Administrador y Prevencionista pueden guardar; Operador es rechazado', async () => {
    const validConfig = configService.getDefaultConfig('device_TEST');

    // Admin puede guardar
    await expect(
      configService.saveDeviceAlertConfig(validConfig, {
        name: 'Manuel Crisóstomo',
        role: 'admin',
      })
    ).resolves.toHaveProperty('success', true);

    // Prevencionista puede guardar
    await expect(
      configService.saveDeviceAlertConfig(validConfig, {
        name: 'Prevencionista Faena',
        role: 'prevencionista',
      })
    ).resolves.toHaveProperty('success', true);

    // Operador NO puede guardar
    await expect(
      configService.saveDeviceAlertConfig(validConfig, {
        name: 'Operador Turno',
        role: 'operador',
      })
    ).rejects.toThrow(/Permiso denegado/);
  });
});

describe('Motor de Evaluación de Alertas (AlertEngineService)', () => {
  let engineService: AlertEngineService;
  let testConfig: DeviceAlertConfig;

  beforeEach(() => {
    vi.clearAllMocks();
    engineService = new AlertEngineService();
    const configService = new AlertConfigService();
    testConfig = configService.getDefaultConfig('device_ESP32_01');
    // Límites controlados:
    // MQ-2: preventivo=350, advertencia=520, critico=750
    // PM2.5: preventivo=45, advertencia=85, critico=130
  });

  it('1. Detecta estado "normal" cuando todas las lecturas están bajo umbral preventivo', () => {
    const safeTelemetry: DeviceTelemetry = {
      deviceId: 'device_ESP32_01',
      temperatura: 22.0,
      humedad: 60.0,
      mq2_adc: 180,
      pm25_adc: 25,
    };

    const result = engineService.evaluateTelemetry(safeTelemetry, testConfig);
    expect(result.overallLevel).toBe('normal');
    expect(result.newEventsGenerated).toHaveLength(0);
    expect(result.resolvedEvents).toHaveLength(0);
  });

  it('2. Detecta niveles preventivo, advertencia y crítico con precisión', () => {
    // Caso Preventivo en MQ-2 (380 >= 350 pero < 520)
    const prevTelemetry: DeviceTelemetry = {
      deviceId: 'device_ESP32_01',
      temperatura: 22.0,
      humedad: 60.0,
      mq2_adc: 380,
      pm25_adc: 30,
    };

    const resPrev = engineService.evaluateTelemetry(prevTelemetry, testConfig);
    expect(resPrev.overallLevel).toBe('preventivo');
    expect(resPrev.newEventsGenerated).toHaveLength(1);
    expect(resPrev.newEventsGenerated[0].nivel).toBe('preventivo');
    expect(resPrev.newEventsGenerated[0].umbralSuperado).toBe(350);

    // Caso Crítico en PM2.5 (145 >= 130)
    const critTelemetry: DeviceTelemetry = {
      deviceId: 'device_ESP32_01',
      temperatura: 22.0,
      humedad: 60.0,
      mq2_adc: 200,
      pm25_adc: 145,
    };

    const resCrit = engineService.evaluateTelemetry(critTelemetry, testConfig);
    expect(resCrit.overallLevel).toBe('critico');
    expect(resCrit.newEventsGenerated.some((e) => e.nivel === 'critico')).toBe(true);
  });

  it('3. Evita spam de alertas duplicadas ante lecturas consecutivas en el mismo nivel (Debounce / In-flight state)', () => {
    const alertTelemetry: DeviceTelemetry = {
      deviceId: 'device_ESP32_01',
      temperatura: 22.0,
      humedad: 60.0,
      mq2_adc: 800, // Crítico
      pm25_adc: 20,
    };

    // Primera evaluación: genera evento crítico
    const firstEval = engineService.evaluateTelemetry(alertTelemetry, testConfig);
    expect(firstEval.newEventsGenerated).toHaveLength(1);

    // Segunda evaluación con el mismo nivel crítico: NO debe duplicar evento
    const secondEval = engineService.evaluateTelemetry(alertTelemetry, testConfig);
    expect(secondEval.newEventsGenerated).toHaveLength(0);

    // Tercera evaluación: aún en crítico, no genera evento duplicado
    const thirdEval = engineService.evaluateTelemetry(
      { ...alertTelemetry, mq2_adc: 820 },
      testConfig
    );
    expect(thirdEval.newEventsGenerated).toHaveLength(0);
  });

  it('4. Detecta recuperación cuando los valores descienden y marca evento como recuperado', () => {
    // 1. Iniciar condición crítica
    engineService.evaluateTelemetry(
      {
        deviceId: 'device_ESP32_01',
        temperatura: 22.0,
        humedad: 60.0,
        mq2_adc: 800, // Crítico
        pm25_adc: 20,
      },
      testConfig
    );

    // 2. La atmósfera se normaliza (MQ-2 desciende a 150 < preventivo 350)
    const normalTelemetry: DeviceTelemetry = {
      deviceId: 'device_ESP32_01',
      temperatura: 22.0,
      humedad: 60.0,
      mq2_adc: 150,
      pm25_adc: 20,
    };

    const recoveryEval = engineService.evaluateTelemetry(normalTelemetry, testConfig);
    expect(recoveryEval.overallLevel).toBe('normal');
    expect(recoveryEval.resolvedEvents).toHaveLength(1);
    expect(recoveryEval.resolvedEvents[0].estado).toBe('recuperado');
    expect(recoveryEval.resolvedEvents[0].fechaResolucion).toBeDefined();
  });
});
