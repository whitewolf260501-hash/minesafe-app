package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceSummary(
    val deviceId: String = "",
    val name: String = "",
    val lastReadingTime: String = "",
    val lastUpdateServer: Long = 0L,
    val status: String = "Online", // "Online", "Offline", "Alerta"
    val temperatura: Double? = null,
    val humedad: Double? = null,
    val mq135: Double? = null,
    val pm25: Double? = null,
    val co: Double? = null,
    val co2: Double? = null,
    val hasAlert: Boolean = false,
    val alarma: Boolean = false,
    val luz: Boolean = false
)

data class HistoricalPoint(
    val key: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val temperatura: Double = 0.0,
    val humedad: Double = 0.0,
    val gases: Double = 0.0, // mq135 or CO/CO2
    val pm25: Double = 0.0,
    val co: Double = 0.0,
    val co2: Double = 0.0,
    val rawDateString: String = ""
) {
    val displayTime: String
        get() = if (rawDateString.isNotEmpty()) {
            rawDateString
        } else {
            try {
                val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            } catch (e: Exception) {
                "--/-- --:--"
            }
        }
}

data class GatewayData(
    val deviceId: String = "gateway",
    val nombre: String = "Gateway Bocamina (Concentrador LoRa/WiFi)",
    val ip: String = "192.168.1.100",
    val mac: String = "24:6F:28:B4:7C:12",
    val puerto: Int = 8080,
    val isConnected: Boolean = false,
    val lastSeenText: String = "Sin latido en /dispositivos/gateway",
    val lastUpdateServer: Long = 0L,
    val rssi: Int = -62,
    val firmware: String = "v2.4.1-mine-gw",
    val enlace: String = "LoRaWAN 915MHz • WiFi Bocamina (Subterráneo)",
    val redMesh: String = "Minera-Bocamina-Mesh",
    val subred: String = "255.255.255.0",
    val gatewayPredeterminado: String = "192.168.1.1",
    val dns: String = "8.8.8.8",
    val nodosConectados: Int = 4,
    val paquetesRecibidos: Long = 18450L,
    val uptime: String = "16d 4h 12m",
    val protocolo: String = "MQTT / LoRaWAN Subterráneo",
    val canalLoRa: String = "CH 0 (915.2 MHz) SF7"
)

typealias GatewayInfo = GatewayData

enum class MetricStatus {
    SAFE,       // Verde (Verde = Seguro)
    WARNING,    // Ámbar (Ámbar = Alerta)
    CRITICAL    // Rojo (Rojo = Crítico)
}

data class SensorMetricDefinition(
    val id: String,
    val name: String,
    val emoji: String,
    val value: Double,
    val unit: String,
    val threshold: Double,
    val isMinThreshold: Boolean = false,
    val maxScale: Double,
    val regulatoryStandard: String = "DS 132 Art. 138",
    val description: String = ""
) {
    val status: MetricStatus
        get() = if (isMinThreshold) {
            when {
                value < threshold * 0.95 -> MetricStatus.CRITICAL
                value < threshold -> MetricStatus.WARNING
                else -> MetricStatus.SAFE
            }
        } else {
            when {
                value > threshold * 1.05 -> MetricStatus.CRITICAL
                value > threshold * 0.75 -> MetricStatus.WARNING
                else -> MetricStatus.SAFE
            }
        }

    val progressFraction: Float
        get() = (value / maxScale).coerceIn(0.0, 1.0).toFloat()

    val thresholdFraction: Float
        get() = (threshold / maxScale).coerceIn(0.0, 1.0).toFloat()

    val formattedValue: String
        get() = when {
            unit == "%" || unit == "°C" || unit == "m/s" -> String.format(Locale.US, "%.1f", value)
            value >= 100 -> String.format(Locale.US, "%.0f", value)
            else -> String.format(Locale.US, "%.1f", value)
        }
}

data class MiningCriticalSensorsData(
    val deviceId: String = "",
    val deviceName: String = "",
    val sector: String = "Galería 1 - Nivel 4",
    val status: String = "Online",
    val bateria: Int = 94,
    val enlace: String = "LoRaWAN 915MHz • RSSI -64 dBm",
    val temperatura: Double = 24.5,
    val humedad: Double = 58.0,
    val co: Double = 18.0,
    val co2: Double = 640.0,
    val pm25: Double = 22.0,
    val metanoCH4: Double = 0.4,
    val ventilacion: Double = 1.8,
    val ruido: Double = 72.0,
    val oxigenoO2: Double = 20.9
) {
    fun toMetricList(): List<SensorMetricDefinition> = listOf(
        SensorMetricDefinition(
            id = "temp",
            name = "Temperatura",
            emoji = "🌡️",
            value = temperatura,
            unit = "°C",
            threshold = 28.0,
            maxScale = 45.0,
            regulatoryStandard = "DS 132 Art. 138 (Máx 28°C)",
            description = "Control térmico en frentes y galerías de trabajo minero."
        ),
        SensorMetricDefinition(
            id = "hum",
            name = "Humedad",
            emoji = "💧",
            value = humedad,
            unit = "%",
            threshold = 80.0,
            maxScale = 100.0,
            regulatoryStandard = "DS 132 Art. 138 (Máx 80%)",
            description = "Control de condensación ambiental y confort higrotérmico."
        ),
        SensorMetricDefinition(
            id = "co",
            name = "Monóxido CO (MQ-2)",
            emoji = "🔥",
            value = co,
            unit = "ppm",
            threshold = 50.0,
            maxScale = 100.0,
            regulatoryStandard = "DS 132 Art. 140 (Límite 50 ppm)",
            description = "Gas asfixiante derivado de combustión diésel o tronadura."
        ),
        SensorMetricDefinition(
            id = "co2",
            name = "Dióxido Carbono CO2",
            emoji = "☁️",
            value = co2,
            unit = "ppm",
            threshold = 1000.0,
            maxScale = 2000.0,
            regulatoryStandard = "DS 132 Art. 140 (Alerta 1000 ppm)",
            description = "Indicador de acumulación y renovación de aire fresco."
        ),
        SensorMetricDefinition(
            id = "pm25",
            name = "Polvo PM2.5",
            emoji = "🌫️",
            value = pm25,
            unit = "µg/m³",
            threshold = 50.0,
            maxScale = 150.0,
            regulatoryStandard = "DS 132 Art. 144 (Límite 50 µg/m³)",
            description = "Partículas respirables finas capaces de alcanzar alvéolos."
        ),
        SensorMetricDefinition(
            id = "ch4",
            name = "Gas Metano CH4",
            emoji = "⚡",
            value = metanoCH4,
            unit = "% LEL",
            threshold = 5.0,
            maxScale = 20.0,
            regulatoryStandard = "DS 132 Art. 142 (Máx 5% LEL)",
            description = "Gas inflamable/grisú con riesgo de deflagración o ignición."
        ),
        SensorMetricDefinition(
            id = "vent",
            name = "Flujo Ventilación",
            emoji = "💨",
            value = ventilacion,
            unit = "m/s",
            threshold = 0.5,
            isMinThreshold = true,
            maxScale = 4.0,
            regulatoryStandard = "DS 132 Art. 136 (Mín 0.5 m/s)",
            description = "Velocidad de aire obligatoria para barrido en avance."
        ),
        SensorMetricDefinition(
            id = "ruido",
            name = "Ruido Acústico",
            emoji = "🔊",
            value = ruido,
            unit = "dB",
            threshold = 85.0,
            maxScale = 120.0,
            regulatoryStandard = "DS 132 Art. 148 (Máx 85 dB)",
            description = "Exposición a vibración y presión acústica de maquinaria."
        ),
        SensorMetricDefinition(
            id = "o2",
            name = "Oxígeno O2",
            emoji = "🫁",
            value = oxigenoO2,
            unit = "%",
            threshold = 19.5,
            isMinThreshold = true,
            maxScale = 25.0,
            regulatoryStandard = "DS 132 Art. 138 (Mín 19.5% O2)",
            description = "Contenido mínimo de oxígeno vital para la respiración humana."
        )
    )
}

