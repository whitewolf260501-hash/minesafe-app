package com.example.model

enum class AlertSeverity {
    DANGER,   // Red
    WARNING   // Amber
}

data class SafetyAlert(
    val id: String,
    val title: String,
    val description: String,
    val metricName: String,
    val currentValue: Double,
    val thresholdValue: Double,
    val unit: String,
    val severity: AlertSeverity
)

data class AlertThresholdSettings(
    val thCo: Double = 50.0,
    val thCo2: Double = 1000.0,
    val thPm10: Double = 100.0,
    val thPm25: Double = 50.0,
    val thHumedad: Double = 80.0,
    val thTemperatura: Double = 28.0,
    val thMq2: Double = 400.0,
    val activoCo: Boolean = true,
    val activoCo2: Boolean = true,
    val activoPm10: Boolean = true,
    val activoPm25: Boolean = true,
    val activoHumedad: Boolean = true,
    val activoTemperatura: Boolean = true,
    val activoMq2: Boolean = true,
    val ultimoCambioFecha: String = "Valores de referencia de fábrica",
    val ultimoCambioUsuario: String = "Sistema"
)

data class AlertDeviceReading(
    val deviceId: String = "device_38A839E81F84",
    val co: Double = 0.0,
    val co2: Double = 0.0,
    val pm10: Double = 0.0,
    val pm25: Double = 0.0,
    val humedad: Double = 0.0,
    val temperatura: Double = 22.0,
    val mq2: Double = 250.0,
    val timestamp: Long = System.currentTimeMillis(),
    val thresholds: AlertThresholdSettings = AlertThresholdSettings()
) {
    /**
     * Reglas dinámicas configurables:
     * Compara los valores medidos contra los umbrales configurados por el usuario.
     */
    fun evaluateAlerts(th: AlertThresholdSettings = thresholds): List<SafetyAlert> {
        val alerts = mutableListOf<SafetyAlert>()

        if (th.activoCo && co > th.thCo) {
            alerts.add(
                SafetyAlert(
                    id = "alert_co",
                    title = "Nivel peligroso de monóxido de carbono",
                    description = "CO detectado por encima del umbral configurado de ${th.thCo} ppm. Riesgo inmediato para el personal minero.",
                    metricName = "CO",
                    currentValue = co,
                    thresholdValue = th.thCo,
                    unit = "ppm",
                    severity = AlertSeverity.DANGER
                )
            )
        }

        if (th.activoCo2 && co2 > th.thCo2) {
            alerts.add(
                SafetyAlert(
                    id = "alert_co2",
                    title = "Concentración alta de dióxido de carbono",
                    description = "CO2 superior a ${th.thCo2} ppm. Posible deficiencia de ventilación en socavón o galería.",
                    metricName = "CO2",
                    currentValue = co2,
                    thresholdValue = th.thCo2,
                    unit = "ppm",
                    severity = AlertSeverity.WARNING
                )
            )
        }

        if (th.activoPm10 && pm10 > th.thPm10) {
            alerts.add(
                SafetyAlert(
                    id = "alert_pm10",
                    title = "Alta contaminación por partículas PM10",
                    description = "Polvo en suspensión PM10 superior a ${th.thPm10} µg/m³. Requiere uso obligatorio de respiradores.",
                    metricName = "PM10",
                    currentValue = pm10,
                    thresholdValue = th.thPm10,
                    unit = "µg/m³",
                    severity = AlertSeverity.WARNING
                )
            )
        }

        if (th.activoPm25 && pm25 > th.thPm25) {
            alerts.add(
                SafetyAlert(
                    id = "alert_pm25",
                    title = "Alta contaminación por partículas PM2.5",
                    description = "Partículas finas respirables PM2.5 superan ${th.thPm25} µg/m³. Riesgo respiratorio severo.",
                    metricName = "PM2.5",
                    currentValue = pm25,
                    thresholdValue = th.thPm25,
                    unit = "µg/m³",
                    severity = AlertSeverity.DANGER
                )
            )
        }

        if (th.activoHumedad && humedad > th.thHumedad) {
            alerts.add(
                SafetyAlert(
                    id = "alert_humidity",
                    title = "Humedad excesiva detectada",
                    description = "Humedad ambiental superior al ${th.thHumedad}%. Riesgo de condensación, resbalones y corrosión.",
                    metricName = "Humedad",
                    currentValue = humedad,
                    thresholdValue = th.thHumedad,
                    unit = "%",
                    severity = AlertSeverity.WARNING
                )
            )
        }

        if (th.activoTemperatura && temperatura > th.thTemperatura) {
            alerts.add(
                SafetyAlert(
                    id = "alert_temp",
                    title = "Temperatura elevada en socavón",
                    description = "Temperatura ambiental supera ${th.thTemperatura} °C. Riesgo de estrés térmico para la cuadrilla.",
                    metricName = "Temperatura",
                    currentValue = temperatura,
                    thresholdValue = th.thTemperatura,
                    unit = "°C",
                    severity = AlertSeverity.WARNING
                )
            )
        }

        if (th.activoMq2 && mq2 > th.thMq2) {
            alerts.add(
                SafetyAlert(
                    id = "alert_mq2",
                    title = "Gases inflamables MQ-2 elevados",
                    description = "Lectura analógica MQ-2 supera ${th.thMq2} ADC. Posible presencia de gases combustibles o humo.",
                    metricName = "Gas MQ-2",
                    currentValue = mq2,
                    thresholdValue = th.thMq2,
                    unit = "ADC",
                    severity = AlertSeverity.DANGER
                )
            )
        }

        return alerts
    }

    val activeAlerts: List<SafetyAlert> get() = evaluateAlerts(thresholds)
    val isAllSafe: Boolean get() = activeAlerts.isEmpty()
}

data class ReportedIncident(
    val id: String = "",
    val tipo: String = "Incidencia General",
    val ubicacion: String = "Galería Principal",
    val gravedad: String = "Media", // Baja, Media, Crítica, Emergencia
    val descripcion: String = "",
    val autorNombre: String = "",
    val autorRol: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val alarmaActivada: Boolean = false,
    val estado: String = "Activa" // Activa, Atendida, Resuelta
) {
    val formattedDate: String
        get() = try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            "--:--"
        }
}
