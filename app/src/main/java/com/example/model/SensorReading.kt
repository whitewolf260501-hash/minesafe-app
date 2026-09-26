package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TrendDirection(val symbol: String, val text: String) {
    RISING("↗", "sube"),
    FALLING("↘", "baja"),
    STABLE("→", "estable");

    val display: String get() = "$symbol $text"
}

data class LiveSensorData(
    val temperatura: Double = 0.0,
    val humedad: Double = 0.0,
    val mq135: Double = 0.0,
    val pm25: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val tempTrend: TrendDirection = TrendDirection.STABLE,
    val humTrend: TrendDirection = TrendDirection.STABLE,
    val mqTrend: TrendDirection = TrendDirection.STABLE,
    val pmTrend: TrendDirection = TrendDirection.STABLE
) {
    val formattedTime: String
        get() = try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "--:--:--"
        }
}
