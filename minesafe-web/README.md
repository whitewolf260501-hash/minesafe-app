# MineSafe Web - Cliente Integrado con MineSafe IA

Aplicación web React + TypeScript + Vite para monitoreo ambiental minero con integración al backend de análisis estadístico **MineSafe IA Analysis API (FastAPI v0.1.1)**.

---

## 1. Características Implementadas

1. **Servicio Centralizado (`src/services/aiAnalysisService.ts`):**
   - Conexión tipada con `GET /health` y `POST /v1/analyze`.
   - Normalización de marcas temporales a formato ISO-8601 UTC ('Z').
   - Detección y eliminación de timestamps duplicados requerida por el backend.
   - Manejo robusto de errores HTTP: `401 Unauthorized`, `413 Payload Too Large`, `422 Unprocessable Entity` y timeouts de red con `AbortController`.
   - No expone claves privadas en el código JavaScript del navegador.

2. **Arquitectura de Seguridad:**
   - La clave `MINESAFE_AI_API_KEY` se almacena exclusivamente en variables de servidor (sin prefijo `VITE_`).
   - El proxy de desarrollo en Vite (`/api/ai`) inyecta el encabezado `Authorization: Bearer <KEY>` en el servidor local de desarrollo.
   - En producción se provee la arquitectura recomendada mediante Cloud Functions o API Gateway.

3. **Conexión con Datos Reales de Firebase:**
   - Lecturas reales desde Firebase Realtime Database (`https://minefase-fc5f5-default-rtdb.firebaseio.com`).
   - Diferenciación explícita de lecturas analógicas sin calibrar (`unit="raw"`) vs calibradas.
   - No se inventan valores ficticios de CO₂ ni históricos sintéticos cuando hay menos de 5 observaciones; se muestra un mensaje informativo claro sobre la limitación estadística.

4. **Interfaz Visual de MineSafe IA:**
   - **Resumen:** Dispositivo analizado, ventana temporal UTC, conteo de observaciones válidas y estado de la API.
   - **Detección de Anomalías:** Tabla detallada con valor observado, mediana, puntuación Z robusta (MAD) y explicación estadística.
   - **Análisis de Tendencias:** Indicadores visuales de tendencia (aumento, disminución, estable o datos insuficientes), pendiente estimada por hora y comparación entre primer y último valor.
   - **Informes:** Descarga de informe estructurado en formato JSON con metadatos de auditoría y advertencias normativas.

---

## 2. Requisitos Previos

- **Node.js:** Versión 18 o superior.
- **Backend MineSafe IA:** En ejecución en `http://127.0.0.1:8000`.

---

## 3. Instalación y Ejecución en Windows con Visual Studio Code

### Paso 1: Configurar variables de entorno
1. En la carpeta `minesafe-web/`, copia el archivo `.env.example` y renómbralo a `.env`:
   ```cmd
   copy .env.example .env
   ```
2. Abre `.env` y configura el valor de `MINESAFE_AI_API_KEY` con la misma clave configurada en el backend `minesafe-ai/.env`.

### Paso 2: Instalar dependencias
Abre una terminal en `minesafe-web/` y ejecuta:
```cmd
npm install
```

### Paso 3: Ejecutar en modo desarrollo
```cmd
npm run dev
```
La aplicación web estará disponible en: **`http://localhost:5173`**.

### Paso 4: Ejecutar pruebas unitarias
```cmd
npm test
```

---

## 4. Reglas de Seguridad de Firebase Recomendadas

Para proteger la base de datos `minefase-fc5f5` de lecturas/escrituras públicas no autorizadas e implementar la autorización estricta para la **Configuración Dinámica de Umbrales**, utiliza las siguientes reglas en Firebase Realtime Database:

```json
{
  "rules": {
    "dispositivos": {
      ".read": "auth != null",
      "$deviceId": {
        ".write": "auth != null && (auth.token.role === 'admin' || auth.token.role === 'superAdmin' || auth.uid === $deviceId)"
      }
    },
    "configuracion_alertas": {
      ".read": "auth != null",
      "$deviceId": {
        ".write": "auth != null && (auth.token.role === 'admin' || auth.token.role === 'superAdmin' || auth.token.role === 'prevencionista')",
        ".validate": "newData.hasChildren(['pm25', 'mq2', 'temperatura', 'humedad'])"
      }
    },
    "eventos_alertas": {
      ".read": "auth != null",
      "$deviceId": {
        ".write": "auth != null"
      }
    },
    "historico": {
      ".read": "auth != null",
      ".write": "auth != null && auth.token.role === 'admin'"
    },
    "incidencias": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

---

## 5. Arquitectura de Sincronización con Nodos ESP32 (Sin Reprogramar Firmware)

Para que los nodos ESP32 y Gateway apliquen los umbrales dinámicos sin modificar ni recompilar su código:

1. **Vía Gateway MQTT / HTTP / Firebase Stream:**
   - El Gateway ESP32 (Nodo B) mantiene una suscripción SSE (Server-Sent Events) o sondeo HTTP periódico cada 60 segundos a:
     `https://minefase-fc5f5-default-rtdb.firebaseio.com/configuracion_alertas/{deviceId}.json`
   - Cuando el operador guarda una modificación desde la web, Firebase envía el JSON con los nuevos umbrales (`preventivo`, `advertencia`, `critico`).
   - El Gateway guarda los valores en su memoria no volátil (`EEPROM` / `Preferences.h`) y los difunde a los nodos sensores de socavón mediante el protocolo local **ESP-NOW** en una trama de broadcast de configuración (`TYPE_CONFIG_SYNC`).
2. **Evaluación en Nube (Cloud Alert Engine):**
   - Paralelamente, el servicio `AlertEngineService` en la web y el backend evalúan cada paquete de telemetría entrante en tiempo real contra los umbrales vigentes almacenados en `configuracion_alertas/`, registrando eventos y activando notificaciones incluso si el nodo físico estuviera operando en modo esclavo sin conexión directa a internet.

