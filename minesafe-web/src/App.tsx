import React, { useState, useEffect } from 'react';
import { firebaseMiningService } from './services/firebaseService';
import { DeviceTelemetry } from './types/minesafe';
import { AlertConfigView } from './components/AlertConfigView';
import {
  Shield,
  Activity,
  Bell,
  HardDrive,
  Thermometer,
  Droplets,
  Wind,
  Flame,
  CheckCircle2,
  Sliders,
} from 'lucide-react';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'config_alertas' | 'monitoreo' | 'alertas' | 'dispositivos'>('config_alertas');
  const [devices, setDevices] = useState<DeviceTelemetry[]>([
    {
      deviceId: 'device_38A839E81F84',
      nombre: 'ESP32 Sensor Principal (Galería 1)',
      temperatura: 21.8,
      humedad: 58.4,
      mq2_adc: 310,
      pm25_adc: 42,
      alarma: false,
      luz: false,
      ultimoReporte: Date.now(),
    },
    {
      deviceId: 'gateway_bocamina_01',
      nombre: 'Gateway Bocamina Principal',
      temperatura: 19.5,
      humedad: 62.0,
      mq2_adc: 215,
      pm25_adc: 28,
      alarma: false,
      luz: false,
      ultimoReporte: Date.now(),
    },
  ]);
  const [selectedDeviceId, setSelectedDeviceId] = useState<string>('device_38A839E81F84');

  useEffect(() => {
    // Suscribirse a los dispositivos reales en Firebase Realtime Database
    const unsubscribe = firebaseMiningService.subscribeToDevices((updatedDevices) => {
      if (updatedDevices.length > 0) {
        setDevices(updatedDevices);
        if (!updatedDevices.some((d) => d.deviceId === selectedDeviceId)) {
          setSelectedDeviceId(updatedDevices[0].deviceId);
        }
      }
    });

    return () => unsubscribe();
  }, [selectedDeviceId]);

  const currentDevice = devices.find((d) => d.deviceId === selectedDeviceId) || devices[0];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Barra superior de navegación */}
      <header className="bg-slate-900 border-b border-slate-800 sticky top-0 z-30 px-4 lg:px-8 py-3">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-teal-500 rounded-lg text-slate-950 font-black">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-base font-extrabold tracking-tight text-white flex items-center gap-2">
                MineSafe
                <span className="text-[10px] uppercase font-bold px-1.5 py-0.5 rounded bg-teal-500/20 text-teal-300 border border-teal-500/30">
                  D.S. 132
                </span>
              </h1>
              <p className="text-[11px] text-slate-400">
                Monitoreo Ambiental IoT & Análisis de Series Temporales
              </p>
            </div>
          </div>

          {/* Selector de Pestañas */}
          <nav className="flex items-center space-x-1 bg-slate-950 p-1 rounded-xl border border-slate-800 text-xs">
            <button
              onClick={() => setActiveTab('config_alertas')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg font-bold transition ${
                activeTab === 'config_alertas'
                  ? 'bg-amber-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Sliders className="w-4 h-4" />
              <span>Configuración de Alertas</span>
            </button>
            <button
              onClick={() => setActiveTab('monitoreo')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg font-bold transition ${
                activeTab === 'monitoreo'
                  ? 'bg-teal-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Activity className="w-4 h-4" />
              <span>Monitoreo</span>
            </button>
            <button
              onClick={() => setActiveTab('alertas')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg font-bold transition ${
                activeTab === 'alertas'
                  ? 'bg-teal-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Bell className="w-4 h-4" />
              <span>Riesgos</span>
            </button>
            <button
              onClick={() => setActiveTab('dispositivos')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg font-bold transition ${
                activeTab === 'dispositivos'
                  ? 'bg-teal-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <HardDrive className="w-4 h-4" />
              <span>Nodos IoT</span>
            </button>
          </nav>
        </div>
      </header>

      {/* Contenido Principal */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 lg:p-8">
        {activeTab === 'config_alertas' && (
          <AlertConfigView
            selectedDeviceId={selectedDeviceId}
            availableDevices={devices}
            onSelectDevice={setSelectedDeviceId}
          />
        )}

        {activeTab === 'monitoreo' && (
          <div className="space-y-6">
            {/* Cabecera del dispositivo */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <span className="text-xs font-bold text-teal-400 uppercase tracking-wider">
                  Dispositivo Seleccionado
                </span>
                <h2 className="text-lg font-bold text-white mt-0.5">
                  {currentDevice.nombre || currentDevice.deviceId}
                </h2>
                <span className="text-xs text-slate-400 font-mono">
                  ID: {currentDevice.deviceId}
                </span>
              </div>

              <div className="flex items-center space-x-2">
                <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                  Transmisión en Vivo
                </span>
              </div>
            </div>

            {/* Tarjetas de sensores reales */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
                <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
                  <span>Temperatura</span>
                  <Thermometer className="w-4 h-4 text-orange-400" />
                </div>
                <p className="text-2xl font-bold font-mono text-slate-100">
                  {currentDevice.temperatura !== undefined ? `${currentDevice.temperatura} °C` : '--'}
                </p>
                <span className="text-[11px] text-slate-400 mt-1 block">Rango térmico de galería</span>
              </div>

              <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
                <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
                  <span>Humedad Relativa</span>
                  <Droplets className="w-4 h-4 text-cyan-400" />
                </div>
                <p className="text-2xl font-bold font-mono text-slate-100">
                  {currentDevice.humedad !== undefined ? `${currentDevice.humedad} %` : '--'}
                </p>
                <span className="text-[11px] text-slate-400 mt-1 block">Humedad en socavón</span>
              </div>

              <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
                <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
                  <span>Gas MQ-2 / MQ-135</span>
                  <Flame className="w-4 h-4 text-amber-400" />
                </div>
                <p className="text-2xl font-bold font-mono text-slate-100">
                  {currentDevice.mq2_adc !== undefined ? currentDevice.mq2_adc : currentDevice.mq135_adc || '--'}
                </p>
                <span className="text-[11px] text-slate-400 mt-1 block">Lectura analógica (raw ADC)</span>
              </div>

              <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
                <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
                  <span>Polvo en Suspensión</span>
                  <Wind className="w-4 h-4 text-purple-400" />
                </div>
                <p className="text-2xl font-bold font-mono text-slate-100">
                  {currentDevice.pm25_adc !== undefined ? currentDevice.pm25_adc : '--'}
                </p>
                <span className="text-[11px] text-slate-400 mt-1 block">Sensor PM2.5 (raw ADC)</span>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'alertas' && (
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 text-center space-y-4">
            <Shield className="w-12 h-12 text-teal-400 mx-auto" />
            <h3 className="text-base font-bold text-white">Panel de Control de Riesgos & Actuadores</h3>
            <p className="text-xs text-slate-400 max-w-md mx-auto">
              Los actuadores físicos (buzzer, baliza y ventilación forzada) responden exclusivamente a las condiciones normativas D.S. 132 y no son accionados por la IA estadística.
            </p>
            <div className="pt-2 flex justify-center space-x-3">
              <span className="px-3 py-1.5 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-semibold">
                Atmósfera Operacional Segura
              </span>
            </div>
          </div>
        )}

        {activeTab === 'dispositivos' && (
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
            <h3 className="text-sm font-bold text-slate-100 uppercase tracking-wider mb-4">
              Nodos IoT y Gateway Registrados ({devices.length})
            </h3>
            <div className="divide-y divide-slate-800 font-mono text-xs">
              {devices.map((dev) => (
                <div key={dev.deviceId} className="py-3 flex items-center justify-between">
                  <div>
                    <span className="font-bold text-slate-200">{dev.nombre || dev.deviceId}</span>
                    <span className="text-slate-400 block text-[11px]">ID: {dev.deviceId}</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-teal-500/10 text-teal-300 border border-teal-500/20 font-sans">
                      Conectado RTDB
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
