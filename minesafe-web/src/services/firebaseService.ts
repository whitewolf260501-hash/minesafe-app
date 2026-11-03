import { initializeApp, getApps, getApp } from 'firebase/app';
import { getDatabase, ref, onValue, get } from 'firebase/database';
import { Observation, DeviceTelemetry } from '../types/minesafe';

const firebaseConfig = {
  apiKey: import.meta.env?.VITE_FIREBASE_API_KEY || 'AIzaSyPlaceholderKeyForClient',
  authDomain: import.meta.env?.VITE_FIREBASE_AUTH_DOMAIN || 'minefase-fc5f5.firebaseapp.com',
  databaseURL: import.meta.env?.VITE_FIREBASE_DATABASE_URL || 'https://minefase-fc5f5-default-rtdb.firebaseio.com',
  projectId: import.meta.env?.VITE_FIREBASE_PROJECT_ID || 'minefase-fc5f5',
  storageBucket: import.meta.env?.VITE_FIREBASE_STORAGE_BUCKET || 'minefase-fc5f5.appspot.com',
  messagingSenderId: import.meta.env?.VITE_FIREBASE_MESSAGING_SENDER_ID || '873613635290',
  appId: import.meta.env?.VITE_FIREBASE_APP_ID || '1:873613635290:web:abcdef123456',
};

const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
export const database = getDatabase(app);

export class FirebaseMiningService {
  /**
   * Escucha en tiempo real la lista de dispositivos registrados en Firebase RTDB.
   */
  subscribeToDevices(onUpdate: (devices: DeviceTelemetry[]) => void): () => void {
    const devicesRef = ref(database, 'dispositivos');
    return onValue(
      devicesRef,
      (snapshot) => {
        if (!snapshot.exists()) {
          onUpdate([]);
          return;
        }

        const data = snapshot.val();
        const devices: DeviceTelemetry[] = [];

        for (const [key, val] of Object.entries<any>(data)) {
          devices.push({
            deviceId: key,
            nombre: val.nombre || `Nodo ${key.slice(-6)}`,
            temperatura: val.temperatura !== undefined ? Number(val.temperatura) : undefined,
            humedad: val.humedad !== undefined ? Number(val.humedad) : undefined,
            mq135_adc: val.mq135 !== undefined ? Number(val.mq135) : val.gas_adc !== undefined ? Number(val.gas_adc) : undefined,
            mq2_adc: val.mq2 !== undefined ? Number(val.mq2) : val.gas_adc !== undefined ? Number(val.gas_adc) : undefined,
            pm25_adc: val.pm25 !== undefined ? Number(val.pm25) : undefined,
            alarma: Boolean(val.alarma || val.buzzer),
            luz: Boolean(val.luz || val.led),
            ultimoReporte: val.timestamp ? Number(val.timestamp) : Date.now(),
          });
        }

        onUpdate(devices);
      },
      (error) => {
        console.warn('Error leyendo dispositivos desde Firebase RTDB:', error.message);
        onUpdate([]);
      }
    );
  }

  /**
   * Obtiene la serie temporal real registrada en Firebase para un dispositivo.
   * Si no hay suficientes registros reales, devuelve un arreglo vacío sin inventar datos sintéticos.
   */
  async fetchRealObservations(deviceId: string, maxPoints = 100): Promise<Observation[]> {
    try {
      // 1. Intentar leer nodo de histórico del dispositivo
      const historyRef = ref(database, `historico/${deviceId}`);
      const snapshot = await get(historyRef);

      if (snapshot.exists()) {
        const val = snapshot.val();
        const observations: Observation[] = [];

        const entries = Array.isArray(val) ? val : Object.values(val);
        for (const entry of entries as any[]) {
          if (!entry) continue;
          const ts = entry.timestamp || entry.fecha || entry.time;
          if (!ts) continue;

          const date = new Date(typeof ts === 'number' && ts < 10000000000 ? ts * 1000 : ts);
          if (isNaN(date.getTime())) continue;

          observations.push({
            timestamp: date.toISOString(),
            pm25_raw: entry.pm25 !== undefined ? Number(entry.pm25) : entry.pm25_raw !== undefined ? Number(entry.pm25_raw) : null,
            mq2_raw: entry.mq2 !== undefined ? Number(entry.mq2) : entry.mq135 !== undefined ? Number(entry.mq135) : null,
            temperature_c: entry.temperatura !== undefined ? Number(entry.temperatura) : entry.temp !== undefined ? Number(entry.temp) : null,
            humidity_pct: entry.humedad !== undefined ? Number(entry.humedad) : entry.hum !== undefined ? Number(entry.hum) : null,
            unit: 'raw', // ADC crudo según diseño de seguridad
          });
        }

        if (observations.length > 0) {
          return observations
            .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
            .slice(-maxPoints);
        }
      }

      // 2. Si no hay nodo de histórico pero el dispositivo tiene lectura actual, leer el nodo actual
      const currentRef = ref(database, `dispositivos/${deviceId}`);
      const curSnap = await get(currentRef);
      if (curSnap.exists()) {
        const d = curSnap.val();
        const date = d.timestamp ? new Date(d.timestamp) : new Date();
        return [
          {
            timestamp: date.toISOString(),
            pm25_raw: d.pm25 !== undefined ? Number(d.pm25) : null,
            mq2_raw: d.mq2 !== undefined ? Number(d.mq2) : d.mq135 !== undefined ? Number(d.mq135) : null,
            temperature_c: d.temperatura !== undefined ? Number(d.temperatura) : null,
            humidity_pct: d.humedad !== undefined ? Number(d.humedad) : null,
            unit: 'raw',
          },
        ];
      }

      return [];
    } catch (e: any) {
      console.warn(`Error al consultar histórico de ${deviceId} en Firebase:`, e.message);
      return [];
    }
  }
}

export const firebaseMiningService = new FirebaseMiningService();
