package com.example.model

data class AiObservation(
    val timestamp: String,
    val pm25_raw: Double? = null,
    val mq2_raw: Double? = null,
    val temperature_c: Double? = null,
    val humidity_pct: Double? = null,
    val unit: String = "raw"
)

data class AiAnalyzeRequest(
    val device_id: String,
    val observations: List<AiObservation>
)

data class AiMetricTrend(
    val metric: String,
    val first_value: Double,
    val last_value: Double,
    val slope_per_hour: Double?,
    val direction: String, // "increasing", "decreasing", "stable", "insufficient_data"
    val unit: String
)

data class AiAnomaly(
    val timestamp: String,
    val metric: String,
    val value: Double,
    val median: Double,
    val robust_z_score: Double,
    val unit: String,
    val explanation: String
)

data class AiAnalyzeResponse(
    val device_id: String,
    val generated_at: String,
    val window_start: String,
    val window_end: String,
    val observations_count: Int,
    val analysis_method: String,
    val trends: List<AiMetricTrend>,
    val anomalies: List<AiAnomaly>,
    val limitations: List<String>
)

data class AiHealthResponse(
    val status: String,
    val service: String,
    val version: String
)
