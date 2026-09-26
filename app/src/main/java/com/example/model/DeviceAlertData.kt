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

data class AlertDeviceReading(
    val deviceId: String = "device_38A839E81F84",
    val co: Double = 0.0,
    val co2: Double = 0.0,
    val pm10: Double = 0.0,
    val pm25: Double = 0.0,
    val humedad: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Reglas exactas especificadas por el usuario:
     * - CO > 50 -> alerta "Nivel peligroso de monóxido de carbono"
     * - CO2 > 1000 -> alerta "Concentración alta de dióxido de carbono"
     * - PM10 > 100 -> alerta "Alta contaminación por partículas PM10"
     * - PM2_5 > 50 -> alerta "Alta contaminación por partículas PM2.5"
     * - humedad > 80 -> alerta "Humedad excesiva detectada"
     */
    val activeAlerts: List<SafetyAlert>
        get() {
            val alerts = mutableListOf<SafetyAlert>()

            if (co > 50) {
                alerts.add(
                    SafetyAlert(
                        id = "alert_co",
                        title = "Nivel peligroso de monóxido de carbono",
                        description = "CO detectado por encima del umbral crítico de 50 ppm. Riesgo inmediato para el personal minero.",
                        metricName = "CO",
                        currentValue = co,
                        thresholdValue = 50.0,
                        unit = "ppm",
                        severity = AlertSeverity.DANGER
                    )
                )
            }

            if (co2 > 1000) {
                alerts.add(
                    SafetyAlert(
                        id = "alert_co2",
                        title = "Concentración alta de dióxido de carbono",
                        description = "CO2 superior a 1000 ppm. Posible deficiencia de ventilación en socavón o galería.",
                        metricName = "CO2",
                        currentValue = co2,
                        thresholdValue = 1000.0,
                        unit = "ppm",
                        severity = AlertSeverity.WARNING
                    )
                )
            }

            if (pm10 > 100) {
                alerts.add(
                    SafetyAlert(
                        id = "alert_pm10",
                        title = "Alta contaminación por partículas PM10",
                        description = "Polvo en suspensión PM10 superior a 100 µg/m³. Requiere uso obligatorio de respiradores.",
                        metricName = "PM10",
                        currentValue = pm10,
                        thresholdValue = 100.0,
                        unit = "µg/m³",
                        severity = AlertSeverity.WARNING
                    )
                )
            }

            if (pm25 > 50) {
                alerts.add(
                    SafetyAlert(
                        id = "alert_pm25",
                        title = "Alta contaminación por partículas PM2.5",
                        description = "Partículas finas respirables PM2.5 superan 50 µg/m³. Riesgo respiratorio severo.",
                        metricName = "PM2.5",
                        currentValue = pm25,
                        thresholdValue = 50.0,
                        unit = "µg/m³",
                        severity = AlertSeverity.DANGER
                    )
                )
            }

            if (humedad > 80) {
                alerts.add(
                    SafetyAlert(
                        id = "alert_humidity",
                        title = "Humedad excesiva detectada",
                        description = "Humedad ambiental superior al 80%. Riesgo de condensación, resbalones y corrosión.",
                        metricName = "Humedad",
                        currentValue = humedad,
                        thresholdValue = 80.0,
                        unit = "%",
                        severity = AlertSeverity.WARNING
                    )
                )
            }

            return alerts
        }

    val isAllSafe: Boolean get() = activeAlerts.isEmpty()
}
