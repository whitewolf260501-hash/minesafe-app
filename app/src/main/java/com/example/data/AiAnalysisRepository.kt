package com.example.data

import android.util.Log
import com.example.model.AiAnalyzeRequest
import com.example.model.AiAnalyzeResponse
import com.example.model.AiAnomaly
import com.example.model.AiHealthResponse
import com.example.model.AiMetricTrend
import com.example.model.AiObservation
import com.example.model.HistoricalPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class AiAnalysisRepository {
    private val TAG = "MineSafe_AiRepo"
    // 10.0.2.2 maps to the host machine loopback (127.0.0.1) when running in Android emulator/container
    private var baseUrl: String = "http://10.0.2.2:8000"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun setBaseUrl(url: String) {
        baseUrl = url.trim().removeSuffix("/")
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun checkHealth(): Result<AiHealthResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: Servicio no disponible"))
                }
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val health = AiHealthResponse(
                    status = json.optString("status", "ok"),
                    service = json.optString("service", "minesafe-ai-analysis"),
                    version = json.optString("version", "0.1.1")
                )
                Result.success(health)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al comprobar health de IA: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun analyzeSeries(
        deviceId: String,
        observations: List<AiObservation>,
        apiKey: String
    ): Result<AiAnalyzeResponse> = withContext(Dispatchers.IO) {
        if (observations.size < 5) {
            return@withContext Result.failure(
                IllegalArgumentException("Se requieren al menos 5 observaciones para el análisis estadístico.")
            )
        }

        try {
            val rootJson = JSONObject()
            rootJson.put("device_id", deviceId)

            val obsArray = JSONArray()
            for (obs in observations) {
                val oJson = JSONObject()
                oJson.put("timestamp", obs.timestamp)
                if (obs.pm25_raw != null) oJson.put("pm25_raw", obs.pm25_raw)
                if (obs.mq2_raw != null) oJson.put("mq2_raw", obs.mq2_raw)
                if (obs.temperature_c != null) oJson.put("temperature_c", obs.temperature_c)
                if (obs.humidity_pct != null) oJson.put("humidity_pct", obs.humidity_pct)
                oJson.put("unit", obs.unit)
                obsArray.put(oJson)
            }
            rootJson.put("observations", obsArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)

            val reqBuilder = Request.Builder()
                .url("$baseUrl/v1/analyze")
                .post(requestBody)

            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val detail = try {
                        JSONObject(body).optString("detail", "Error HTTP ${response.code}")
                    } catch (e: Exception) {
                        "Error HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception(detail))
                }

                val json = JSONObject(body)

                // Parse trends
                val trendsList = mutableListOf<AiMetricTrend>()
                val trendsArr = json.optJSONArray("trends") ?: JSONArray()
                for (i in 0 until trendsArr.length()) {
                    val tObj = trendsArr.getJSONObject(i)
                    trendsList.add(
                        AiMetricTrend(
                            metric = tObj.optString("metric", ""),
                            first_value = tObj.optDouble("first_value", 0.0),
                            last_value = tObj.optDouble("last_value", 0.0),
                            slope_per_hour = if (tObj.has("slope_per_hour") && !tObj.isNull("slope_per_hour")) tObj.optDouble("slope_per_hour") else null,
                            direction = tObj.optString("direction", "stable"),
                            unit = tObj.optString("unit", "")
                        )
                    )
                }

                // Parse anomalies
                val anomaliesList = mutableListOf<AiAnomaly>()
                val anomArr = json.optJSONArray("anomalies") ?: JSONArray()
                for (i in 0 until anomArr.length()) {
                    val aObj = anomArr.getJSONObject(i)
                    anomaliesList.add(
                        AiAnomaly(
                            timestamp = aObj.optString("timestamp", ""),
                            metric = aObj.optString("metric", ""),
                            value = aObj.optDouble("value", 0.0),
                            median = aObj.optDouble("median", 0.0),
                            robust_z_score = aObj.optDouble("robust_z_score", 0.0),
                            unit = aObj.optString("unit", ""),
                            explanation = aObj.optString("explanation", "")
                        )
                    )
                }

                // Parse limitations
                val limitationsList = mutableListOf<String>()
                val limArr = json.optJSONArray("limitations") ?: JSONArray()
                for (i in 0 until limArr.length()) {
                    limitationsList.add(limArr.getString(i))
                }

                val result = AiAnalyzeResponse(
                    device_id = json.optString("device_id", deviceId),
                    generated_at = json.optString("generated_at", ""),
                    window_start = json.optString("window_start", ""),
                    window_end = json.optString("window_end", ""),
                    observations_count = json.optInt("observations_count", observations.size),
                    analysis_method = json.optString("analysis_method", ""),
                    trends = trendsList,
                    anomalies = anomaliesList,
                    limitations = limitationsList
                )

                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en analyzeSeries: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Convierte puntos históricos reales de Firebase en observaciones ISO-8601 UTC.
     */
    fun convertHistoricalPointsToObservations(points: List<HistoricalPoint>): List<AiObservation> {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val sorted = points.sortedBy { it.timestamp }
        val seen = HashSet<String>()
        val result = mutableListOf<AiObservation>()

        for (p in sorted) {
            val iso = sdf.format(Date(p.timestamp))
            if (seen.contains(iso)) continue
            seen.add(iso)

            result.add(
                AiObservation(
                    timestamp = iso,
                    pm25_raw = p.pm25,
                    mq2_raw = p.gases,
                    temperature_c = p.temperatura,
                    humidity_pct = p.humedad,
                    unit = "raw"
                )
            )
        }

        return result
    }
}
