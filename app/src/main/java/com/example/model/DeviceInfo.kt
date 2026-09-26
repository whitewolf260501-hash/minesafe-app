package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceSummary(
    val deviceId: String = "",
    val name: String = "",
    val lastReadingTime: String = "",
    val status: String = "Online", // "Online", "Offline", "Alerta"
    val temperatura: Double? = null,
    val humedad: Double? = null,
    val mq135: Double? = null,
    val pm25: Double? = null,
    val co: Double? = null,
    val co2: Double? = null,
    val hasAlert: Boolean = false
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
