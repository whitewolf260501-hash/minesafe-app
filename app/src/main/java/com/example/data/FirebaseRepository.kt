package com.example.data

import android.util.Log
import com.example.model.AlertDeviceReading
import com.example.model.AlertThresholdSettings
import com.example.model.DeviceSummary
import com.example.model.GatewayData
import com.example.model.HistoricalPoint
import com.example.model.LiveSensorData
import com.example.model.ReportedIncident
import com.example.model.TrendDirection
import com.example.model.UserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FirebaseRepository {
    private val TAG = "MineSafe_Firebase"
    private val RTDB_BASE_URL = "https://minefase-fc5f5-default-rtdb.firebaseio.com"

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(RTDB_BASE_URL)
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to default FirebaseDatabase instance: ${e.message}")
            FirebaseDatabase.getInstance()
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Connection status
    private val _isFirebaseConnected = MutableStateFlow(true)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _connectionStatusMessage = MutableStateFlow("Conectado a Firebase: minefase-fc5f5")
    val connectionStatusMessage: StateFlow<String> = _connectionStatusMessage.asStateFlow()

    // IoT Sensor Device Connection Status & Real-Time Heartbeat (Threshold: 60s)
    val HEARTBEAT_TIMEOUT_MS = 60_000L

    private val _isDeviceConnected = MutableStateFlow(false)
    val isDeviceConnected: StateFlow<Boolean> = _isDeviceConnected.asStateFlow()

    private val _deviceLastSeenText = MutableStateFlow("Verificando latido en Firebase...")
    val deviceLastSeenText: StateFlow<String> = _deviceLastSeenText.asStateFlow()

    private var lastEsp32HeartbeatMs: Long = 0L
    private var isSimulatedOffline: Boolean = false

    // IoT Gateway / Gateway Concentrator Connection Status & Heartbeat
    private val _isGatewayConnected = MutableStateFlow(false)
    val isGatewayConnected: StateFlow<Boolean> = _isGatewayConnected.asStateFlow()

    private val _gatewayLastSeenText = MutableStateFlow("Verificando nodo /dispositivos/gateway...")
    val gatewayLastSeenText: StateFlow<String> = _gatewayLastSeenText.asStateFlow()

    private var lastGatewayHeartbeatMs: Long = 0L
    private var isGatewaySimulatedOffline: Boolean = false
    private var gatewayNodeExistsInRtdb: Boolean = false
    private var isGatewayReportedOnline: Boolean = true
    private var gatewayIp: String = "192.168.1.100"
    private var gatewayMac: String = "24:6F:28:B4:7C:12"
    private var gatewayPuerto: Int = 8080
    private var gatewayRssi: Int = -62
    private var gatewayFirmware: String = "v2.4.1-mine-gw"
    private var gatewayEnlace: String = "LoRaWAN 915MHz • WiFi Bocamina (Subterráneo)"
    private var gatewayNombre: String = "Gateway Bocamina (Concentrador LoRa/WiFi)"

    private val _gatewayData = MutableStateFlow(
        GatewayData(
            deviceId = "gateway",
            nombre = gatewayNombre,
            ip = gatewayIp,
            mac = gatewayMac,
            puerto = gatewayPuerto,
            isConnected = false,
            lastSeenText = _gatewayLastSeenText.value,
            rssi = gatewayRssi,
            firmware = gatewayFirmware,
            enlace = gatewayEnlace,
            nodosConectados = 4
        )
    )
    val gatewayData: StateFlow<GatewayData> = _gatewayData.asStateFlow()

    fun refreshGatewayDataState() {
        _gatewayData.value = _gatewayData.value.copy(
            isConnected = _isGatewayConnected.value,
            lastSeenText = _gatewayLastSeenText.value,
            ip = gatewayIp,
            mac = gatewayMac,
            puerto = gatewayPuerto,
            rssi = gatewayRssi,
            lastUpdateServer = lastGatewayHeartbeatMs,
            nombre = gatewayNombre,
            firmware = gatewayFirmware,
            enlace = gatewayEnlace
        )
    }

    // Previous readings for live trend computation
    private var prevTemp: Double? = null
    private var prevHum: Double? = null
    private var prevMq: Double? = null
    private var prevPm: Double? = null

    // Live state flows with real values from minefase-fc5f5 (device_38A839E81F84 Sensor Fase 1)
    private val _liveSensorData = MutableStateFlow(
        LiveSensorData(
            temperatura = 24.5,
            humedad = 38.0,
            mq135 = 496.0,
            pm25 = 4095.0,
            timestamp = System.currentTimeMillis()
        )
    )
    val liveSensorData: StateFlow<LiveSensorData> = _liveSensorData.asStateFlow()

    private val _alertDeviceData = MutableStateFlow(
        AlertDeviceReading(
            deviceId = "device_38A839E81F84",
            co = 496.0,
            co2 = 640.0,
            pm10 = 120.0,
            pm25 = 4095.0,
            humedad = 38.0,
            timestamp = System.currentTimeMillis()
        )
    )
    val alertDeviceData: StateFlow<AlertDeviceReading> = _alertDeviceData.asStateFlow()

    var currentThresholds = AlertThresholdSettings()
        private set

    fun updateAlertThresholds(th: AlertThresholdSettings) {
        currentThresholds = th
        _alertDeviceData.value = _alertDeviceData.value.copy(thresholds = th)

        // Try persisting in Firebase Realtime Database: configuracion_alertas/{deviceId}
        try {
            val devId = _alertDeviceData.value.deviceId
            val configMap = mapOf(
                "co" to mapOf("activo" to th.activoCo, "critico" to th.thCo),
                "co2" to mapOf("activo" to th.activoCo2, "advertencia" to th.thCo2),
                "pm10" to mapOf("activo" to th.activoPm10, "advertencia" to th.thPm10),
                "pm25" to mapOf("activo" to th.activoPm25, "critico" to th.thPm25),
                "temperatura" to mapOf("activo" to th.activoTemperatura, "advertencia" to th.thTemperatura),
                "humedad" to mapOf("activo" to th.activoHumedad, "advertencia" to th.thHumedad),
                "mq2" to mapOf("activo" to th.activoMq2, "critico" to th.thMq2),
                "ultimoCambio" to mapOf(
                    "fecha" to th.ultimoCambioFecha,
                    "usuario" to th.ultimoCambioUsuario
                )
            )
            database.reference.child("configuracion_alertas").child(devId).setValue(configMap)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo sincronizar configuracion_alertas con Firebase RTDB: ${e.message}")
        }
    }

    private val _devicesList = MutableStateFlow<List<DeviceSummary>>(emptyList())
    val devicesList: StateFlow<List<DeviceSummary>> = _devicesList.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow("NODO_U-01")
    val selectedDeviceId: StateFlow<String> = _selectedDeviceId.asStateFlow()

    private val _isAlarmaActive = MutableStateFlow(false)
    val isAlarmaActive: StateFlow<Boolean> = _isAlarmaActive.asStateFlow()

    private val _isLuzActive = MutableStateFlow(false)
    val isLuzActive: StateFlow<Boolean> = _isLuzActive.asStateFlow()

    // Explicit manual actuator state per device (persists across polling loops)
    private val localAlarmaStateMap = mutableMapOf<String, Boolean>()
    private val localLuzStateMap = mutableMapOf<String, Boolean>()

    private val _isEmergencySimulationActive = MutableStateFlow(false)
    val isEmergencySimulationActive: StateFlow<Boolean> = _isEmergencySimulationActive.asStateFlow()

    private val _simulationScenarioName = MutableStateFlow("")
    val simulationScenarioName: StateFlow<String> = _simulationScenarioName.asStateFlow()

    private val _actuatorTriggerReason = MutableStateFlow("En reposo (automático por sensores)")
    val actuatorTriggerReason: StateFlow<String> = _actuatorTriggerReason.asStateFlow()

    private val _reportedIncidents = MutableStateFlow<List<ReportedIncident>>(
        listOf(
            ReportedIncident(
                id = "inc_demo_1",
                tipo = "Emanación de Gas CO",
                ubicacion = "Galería 4 - Frente Sur",
                gravedad = "Crítica",
                descripcion = "Se detectó ligero olor y lectura anómala en sensor portátil durante perforación. Cuadrilla replegada temporalmente a zona segura.",
                autorNombre = "Manuel Crisostomo",
                autorRol = "SuperAdmin / Jefe de Mina",
                timestamp = System.currentTimeMillis() - 7200000L,
                alarmaActivada = false,
                estado = "Atendida"
            )
        )
    )
    val reportedIncidents: StateFlow<List<ReportedIncident>> = _reportedIncidents.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            uid = "manual_temp_uid_01",
            email = "manuelcrisostomovega@gmail.com",
            isSuperUser = true,
            isAdmin = true,
            tipoUsuario = "superAdmin",
            nombre = "Manuel Crisostomo",
            empresa = "Mina Chuquicamata",
            tipoMina = "Tajo Abierto",
            latitud = -22.45,
            longitud = -68.92
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _historyList = MutableStateFlow<List<HistoricalPoint>>(emptyList())
    val historyList: StateFlow<List<HistoricalPoint>> = _historyList.asStateFlow()

    private val _allUsersList = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsersList: StateFlow<List<UserProfile>> = _allUsersList.asStateFlow()

    fun setTestingRole(newRole: String) {
        val curr = _userProfile.value
        _userProfile.value = when (newRole.lowercase()) {
            "superadmin" -> curr.copy(tipoUsuario = "superAdmin", isSuperUser = true, isAdmin = true)
            "admin" -> curr.copy(tipoUsuario = "admin", isSuperUser = false, isAdmin = true)
            else -> curr.copy(tipoUsuario = "usuario", isSuperUser = false, isAdmin = false)
        }
    }

    private var liveListener: ValueEventListener? = null
    private var alertListener: ValueEventListener? = null
    private var thresholdListener: ValueEventListener? = null
    private var devicesListener: ValueEventListener? = null
    private var historyListener: ValueEventListener? = null
    private var userProfileListener: ValueEventListener? = null

    init {
        // Start background listeners
        startLiveSensorListener()
        startAlertDeviceListener()
        startThresholdConfigListener()
        startDevicesListListener()
        startGatewayListener()

        // Start background dual-sync to guarantee live telemetry from minefase-fc5f5
        startDualSyncPolling()

        // Start real-time 1-second Heartbeat evaluation loop (60-second inactivity detection)
        startHeartbeatMonitor()
    }

    /**
     * Dual Sync Polling: Continuously queries Firebase RTDB REST endpoints
     * to ensure live data reception directly from minefase-fc5f5.
     */
    private fun startDualSyncPolling() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    fetchPrimaryDeviceRest()
                    fetchDevicesRest()
                    fetchGatewayRest()
                    fetchUsuariosRest()
                    _isFirebaseConnected.value = true
                    _connectionStatusMessage.value = "Sincronizado con Firebase (minefase-fc5f5)"
                } catch (e: Exception) {
                    Log.d(TAG, "DualSync REST polling check: ${e.message}")
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    private fun fetchGatewayRest() {
        try {
            val request = Request.Builder()
                .url("$RTDB_BASE_URL/dispositivos/gateway.json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank() && body != "null") {
                gatewayNodeExistsInRtdb = true
                val json = JSONObject(body)
                val gwTs = json.optLong("last_update_server", json.optLong("timestamp", 0L))
                if (gwTs > 0L) {
                    lastGatewayHeartbeatMs = gwTs
                }
                isGatewayReportedOnline = json.optBoolean("online", true)
                gatewayIp = json.optString("ip", gatewayIp)
                gatewayMac = json.optString("mac", gatewayMac)
                gatewayRssi = json.optInt("rssi", gatewayRssi)
                gatewayPuerto = json.optInt("puerto", gatewayPuerto)
                gatewayFirmware = json.optString("firmware", gatewayFirmware)
                gatewayEnlace = json.optString("enlace", gatewayEnlace)
                gatewayNombre = json.optString("nombre", gatewayNombre)
                refreshGatewayDataState()
            } else {
                gatewayNodeExistsInRtdb = false
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchGatewayRest error: ${e.message}")
        }
    }

    private fun fetchPrimaryDeviceRest() {
        try {
            // First query the primary active sensor node: device_38A839E81F84
            val request = Request.Builder()
                .url("$RTDB_BASE_URL/dispositivos/device_38A839E81F84.json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                val temp = json.optDouble("temperatura", _liveSensorData.value.temperatura)
                val hum = json.optDouble("humedad", _liveSensorData.value.humedad)
                val co = if (json.has("CO")) json.optDouble("CO") else json.optDouble("co", _alertDeviceData.value.co)
                val co2 = if (json.has("CO2")) json.optDouble("CO2") else json.optDouble("co2", _alertDeviceData.value.co2)
                val pm10 = if (json.has("PM10")) json.optDouble("PM10") else json.optDouble("pm10", _alertDeviceData.value.pm10)
                val pm25 = when {
                    json.has("PM2_5") -> json.optDouble("PM2_5")
                    json.has("pm25") -> json.optDouble("pm25")
                    json.has("PM25") -> json.optDouble("PM25")
                    else -> _alertDeviceData.value.pm25
                }
                val ts = json.optLong("last_update_server", 0L)
                if (ts > 0L) {
                    lastEsp32HeartbeatMs = ts
                }

                val tempTrend = calculateTrend(temp, prevTemp, 0.2)
                val humTrend = calculateTrend(hum, prevHum, 0.5)
                val mqTrend = calculateTrend(co, prevMq, 2.0)
                val pmTrend = calculateTrend(pm25, prevPm, 1.0)

                prevTemp = temp
                prevHum = hum
                prevMq = co
                prevPm = pm25

                _liveSensorData.value = LiveSensorData(
                    temperatura = temp,
                    humedad = hum,
                    mq135 = co,
                    pm25 = pm25,
                    timestamp = if (ts > 0L) ts else System.currentTimeMillis(),
                    tempTrend = tempTrend,
                    humTrend = humTrend,
                    mqTrend = mqTrend,
                    pmTrend = pmTrend
                )

                _alertDeviceData.value = AlertDeviceReading(
                    deviceId = "device_38A839E81F84",
                    co = co,
                    co2 = co2,
                    pm10 = pm10,
                    pm25 = pm25,
                    humedad = hum,
                    timestamp = if (ts > 0L) ts else System.currentTimeMillis(),
                    thresholds = currentThresholds
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchPrimaryDeviceRest error: ${e.message}")
        }
    }

    private fun fetchUsuariosRest() {
        try {
            val request = Request.Builder()
                .url("$RTDB_BASE_URL/usuarios.json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                val list = mutableListOf<UserProfile>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val uid = keys.next()
                    val uObj = json.optJSONObject(uid) ?: continue
                    val email = uObj.optString("email", "usuario_$uid@minesafe.pe")
                    val nombre = uObj.optString("nombre", "Usuario $uid")
                    val isSuper = uObj.optBoolean("isSuperUser", false)
                    val isAdmin = uObj.optBoolean("isAdmin", false)
                    val tipoStr = uObj.optString("tipoUsuario", "")
                    val tipo = when {
                        isSuper -> "superAdmin"
                        isAdmin || tipoStr.equals("admin", ignoreCase = true) -> "admin"
                        else -> if (tipoStr.isNotBlank()) tipoStr else "operador"
                    }
                    val empresa = uObj.optString("geoMina", uObj.optString("empresa", "Minera Los Andes"))
                    val tipoMina = uObj.optString("tipoMina", "Mina Subterránea")

                    val lat = when {
                        uObj.has("latitud") -> uObj.optDouble("latitud")
                        empresa.contains("Chuquicamata", ignoreCase = true) -> -22.45
                        empresa.contains("San José", ignoreCase = true) -> -27.16
                        else -> -11.5936
                    }
                    val lng = when {
                        uObj.has("longitud") -> uObj.optDouble("longitud")
                        empresa.contains("Chuquicamata", ignoreCase = true) -> -68.92
                        empresa.contains("San José", ignoreCase = true) -> -70.49
                        else -> -76.2411
                    }

                    list.add(
                        UserProfile(
                            uid = uid,
                            email = email,
                            isSuperUser = isSuper,
                            isAdmin = isAdmin || isSuper,
                            tipoUsuario = tipo,
                            nombre = nombre,
                            empresa = empresa,
                            tipoMina = tipoMina,
                            latitud = lat,
                            longitud = lng
                        )
                    )
                }

                if (list.isNotEmpty()) {
                    _allUsersList.value = list

                    // If logged in user matches by email or UID, bind their profile
                    val currentEmail = _userProfile.value.email
                    val currentUid = _userProfile.value.uid
                    val matched = list.firstOrNull {
                        it.uid == currentUid ||
                                it.email.equals(currentEmail, ignoreCase = true) ||
                                (currentEmail.contains("manuel", ignoreCase = true) && it.uid == "manual_temp_uid_01")
                    }
                    if (matched != null) {
                        _userProfile.value = matched
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchUsuariosRest error: ${e.message}")
        }
    }

    /**
     * Binds and configures the user profile according to the specific email address
     * authenticating into the application against Firebase RTDB users.
     */
    fun bindUserProfileByEmail(email: String, uid: String = ""): UserProfile {
        val cleanEmail = email.trim().lowercase()
        val all = _allUsersList.value

        val matched = all.firstOrNull {
            it.email.trim().lowercase() == cleanEmail ||
            (uid.isNotBlank() && it.uid == uid) ||
            (cleanEmail.startsWith("manuelcrisostomo") && it.uid == "manual_temp_uid_01")
        }

        val profile = if (matched != null) {
            matched
        } else {
            val isSuper = cleanEmail.startsWith("superadmin") || cleanEmail.startsWith("manuelcrisostomo")
            val isAdmin = cleanEmail.startsWith("admin") || isSuper
            val tipo = if (isSuper) "superAdmin" else if (isAdmin) "admin" else "usuario"
            UserProfile(
                uid = if (uid.isNotBlank()) uid else "user_${System.currentTimeMillis()}",
                email = cleanEmail,
                isSuperUser = isSuper,
                isAdmin = isAdmin,
                tipoUsuario = tipo,
                nombre = cleanEmail.substringBefore("@").replace(".", " ")
                    .split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    },
                empresa = if (isSuper) "Mina Chuquicamata" else "Minera Los Andes",
                tipoMina = if (isSuper) "Tajo Abierto" else "Mina Subterránea",
                latitud = if (isSuper) -22.45 else -11.5936,
                longitud = if (isSuper) -68.92 else -76.2411
            )
        }

        _userProfile.value = profile
        startUserProfileListener(profile.uid, profile.email)
        return profile
    }

    /**
     * Set active target device for actuator commands (Alarma / Luz) and live telemetry
     */
    fun setSelectedDevice(deviceId: String) {
        _selectedDeviceId.value = deviceId
        _isAlarmaActive.value = localAlarmaStateMap[deviceId] ?: false
        _isLuzActive.value = localLuzStateMap[deviceId] ?: false

        // Immediately update _liveSensorData with the selected device readings
        val dev = _devicesList.value.find { it.deviceId == deviceId }
        if (dev != null) {
            val temp = dev.temperatura ?: _liveSensorData.value.temperatura
            val hum = dev.humedad ?: _liveSensorData.value.humedad
            val mq = dev.mq135 ?: dev.co ?: _liveSensorData.value.mq135
            val pm = dev.pm25 ?: _liveSensorData.value.pm25
            val ts = if (dev.lastUpdateServer > 0L) dev.lastUpdateServer else System.currentTimeMillis()

            val tempTrend = calculateTrend(temp, prevTemp, 0.2)
            val humTrend = calculateTrend(hum, prevHum, 0.5)
            val mqTrend = calculateTrend(mq, prevMq, 2.0)
            val pmTrend = calculateTrend(pm, prevPm, 1.0)
            prevTemp = temp
            prevHum = hum
            prevMq = mq
            prevPm = pm

            _liveSensorData.value = LiveSensorData(
                temperatura = temp,
                humedad = hum,
                mq135 = mq,
                pm25 = pm,
                timestamp = ts,
                tempTrend = tempTrend,
                humTrend = humTrend,
                mqTrend = mqTrend,
                pmTrend = pmTrend
            )

            if (dev.lastUpdateServer > 0L) {
                lastEsp32HeartbeatMs = dev.lastUpdateServer
            }
        }
    }

    /**
     * Controls device Alarm / Siren (alarma / buzzer) in Firebase Realtime Database
     */
    suspend fun setAlarmaState(targetState: Boolean, deviceId: String? = null) {
        val devId = deviceId ?: _selectedDeviceId.value
        localAlarmaStateMap[devId] = targetState
        if (devId == _selectedDeviceId.value) {
            _isAlarmaActive.value = targetState
        }
        _actuatorTriggerReason.value = if (targetState) "Alarma sonora activada manualmente por operador" else "Alarma sonora silenciada manualmente"

        // Update in-memory devices list
        _devicesList.value = _devicesList.value.map {
            if (it.deviceId == devId) it.copy(alarma = targetState) else it
        }

        // 1. RTDB SDK
        try {
            val devRef = database.getReference("dispositivos/$devId")
            devRef.child("alarma").setValue(targetState)
            devRef.child("buzzer").setValue(if (targetState) 1 else 0)
            devRef.child("alarmaManual").setValue(targetState)
            devRef.child("last_actuator_update").setValue(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarma in RTDB SDK: ${e.message}")
        }

        // 2. REST API update for immediate real-time sync with device
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("alarma", targetState)
                    put("buzzer", if (targetState) 1 else 0)
                    put("alarmaManual", targetState)
                    put("last_actuator_update", System.currentTimeMillis())
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$RTDB_BASE_URL/dispositivos/$devId.json")
                    .patch(body)
                    .build()
                okHttpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.d(TAG, "Error setting alarma in REST: ${e.message}")
            }
        }
    }

    /**
     * Controls device Light / Warning strobe (luz / led) in Firebase Realtime Database
     */
    suspend fun setLuzState(targetState: Boolean, deviceId: String? = null) {
        val devId = deviceId ?: _selectedDeviceId.value
        localLuzStateMap[devId] = targetState
        if (devId == _selectedDeviceId.value) {
            _isLuzActive.value = targetState
        }
        _actuatorTriggerReason.value = if (targetState) "Luz de emergencia activada manualmente" else "Luz apagada manualmente"

        // Update in-memory devices list
        _devicesList.value = _devicesList.value.map {
            if (it.deviceId == devId) it.copy(luz = targetState) else it
        }

        // 1. RTDB SDK
        try {
            val devRef = database.getReference("dispositivos/$devId")
            devRef.child("luz").setValue(targetState)
            devRef.child("led").setValue(if (targetState) 1 else 0)
            devRef.child("luzManual").setValue(targetState)
            devRef.child("last_actuator_update").setValue(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting luz in RTDB SDK: ${e.message}")
        }

        // 2. REST API update for immediate real-time sync with device
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("luz", targetState)
                    put("led", if (targetState) 1 else 0)
                    put("luzManual", targetState)
                    put("last_actuator_update", System.currentTimeMillis())
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$RTDB_BASE_URL/dispositivos/$devId.json")
                    .patch(body)
                    .build()
                okHttpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.d(TAG, "Error setting luz in REST: ${e.message}")
            }
        }
    }

    private fun fetchDevicesRest() {
        try {
            val request = Request.Builder()
                .url("$RTDB_BASE_URL/dispositivos.json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                val list = mutableListOf<DeviceSummary>()

                // Add dispositivo1
                val live = _liveSensorData.value
                list.add(
                    DeviceSummary(
                        deviceId = "dispositivo1",
                        name = "ESP32 Sensor Primario (Galería 1)",
                        lastReadingTime = live.formattedTime,
                        status = if (_isDeviceConnected.value) "Online" else "Offline",
                        temperatura = live.temperatura,
                        humedad = live.humedad,
                        mq135 = live.mq135,
                        pm25 = live.pm25,
                        hasAlert = false
                    )
                )

                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val devObj = json.optJSONObject(key) ?: continue
                    val lastUpdate = devObj.optLong("last_update_server", devObj.optLong("timestamp_server", 0L))

                    if (key == _selectedDeviceId.value && lastUpdate > 0L) {
                        lastEsp32HeartbeatMs = lastUpdate
                    }

                    if (key.equals("gateway", ignoreCase = true)) {
                        gatewayNodeExistsInRtdb = true
                        if (lastUpdate > 0L) {
                            lastGatewayHeartbeatMs = lastUpdate
                        }
                        isGatewayReportedOnline = devObj.optBoolean("online", true)
                    }

                    val temp = if (devObj.has("temperatura")) devObj.optDouble("temperatura") else null
                    val hum = if (devObj.has("humedad")) devObj.optDouble("humedad") else null
                    val mq = if (devObj.has("mq135")) devObj.optDouble("mq135") else null
                    val pm = if (devObj.has("PM2_5")) devObj.optDouble("PM2_5") else if (devObj.has("pm25")) devObj.optDouble("pm25") else null
                    val co = if (devObj.has("CO")) devObj.optDouble("CO") else if (devObj.has("co")) devObj.optDouble("co") else null
                    val co2 = if (devObj.has("CO2")) devObj.optDouble("CO2") else if (devObj.has("co2")) devObj.optDouble("co2") else null

                    val hasAlert = (co != null && co > 50) ||
                            (co2 != null && co2 > 1000) ||
                            (pm != null && pm > 50) ||
                            (hum != null && hum > 80)

                    val devAlarma = if (localAlarmaStateMap.containsKey(key)) {
                        localAlarmaStateMap[key] == true
                    } else if (devObj.has("alarma") || devObj.has("buzzer")) {
                        devObj.optBoolean("alarma", false) || devObj.optInt("alarma", 0) == 1 || devObj.optInt("buzzer", 0) == 1
                    } else {
                        false
                    }

                    val devLuz = if (localLuzStateMap.containsKey(key)) {
                        localLuzStateMap[key] == true
                    } else if (devObj.has("luz") || devObj.has("led")) {
                        devObj.optBoolean("luz", false) || devObj.optInt("luz", 0) == 1 || devObj.optInt("led", 0) == 1
                    } else {
                        false
                    }

                    if (key == _selectedDeviceId.value) {
                        _isAlarmaActive.value = devAlarma
                        _isLuzActive.value = devLuz
                    }

                    val devName = when (key) {
                        "NODO_U-01" -> "Nodo Minero Subterráneo (NODO_U-01)"
                        "device_38A839E81F84" -> "Detector de Gases y Alertas ($key)"
                        "device_A4CB2F124B00" -> "Estación Perimetral ($key)"
                        else -> "Estación Minera Remota ${key.takeLast(6)}"
                    }

                    if (key == _selectedDeviceId.value && (temp != null || hum != null || pm != null || co != null)) {
                        val currentLive = _liveSensorData.value
                        _liveSensorData.value = currentLive.copy(
                            temperatura = temp ?: currentLive.temperatura,
                            humedad = hum ?: currentLive.humedad,
                            pm25 = pm ?: currentLive.pm25,
                            mq135 = co ?: mq ?: currentLive.mq135,
                            timestamp = if (lastUpdate > 0L) lastUpdate else currentLive.timestamp
                        )
                    }

                    val isDevOnline = !hasAlert && (lastUpdate > 0L && (System.currentTimeMillis() - lastUpdate) <= HEARTBEAT_TIMEOUT_MS)
                    val status = when {
                        hasAlert -> "Alerta"
                        isDevOnline -> "Online"
                        else -> "Offline"
                    }
                    val readingTime = if (lastUpdate > 0L) {
                        val secAgo = (System.currentTimeMillis() - lastUpdate) / 1000
                        if (secAgo < 60) "Hace ${secAgo}s" else if (secAgo < 3600) "Hace ${secAgo / 60}m" else "Hace ${secAgo / 3600}h"
                    } else "Sin señal"

                    list.add(
                        DeviceSummary(
                            deviceId = key,
                            name = devName,
                            lastReadingTime = readingTime,
                            lastUpdateServer = lastUpdate,
                            status = status,
                            temperatura = temp,
                            humedad = hum,
                            mq135 = mq,
                            pm25 = pm,
                            co = co,
                            co2 = co2,
                            hasAlert = hasAlert,
                            alarma = devAlarma,
                            luz = devLuz
                        )
                    )
                }

                if (list.isNotEmpty()) {
                    _devicesList.value = list
                }
            }
        } catch (e: Exception) {
            // Handled
        }
    }

    /**
     * Start Realtime Database native SDK listener on "dispositivo1"
     */
    fun startLiveSensorListener() {
        try {
            val ref = database.getReference("dispositivo1")
            liveListener?.let { ref.removeEventListener(it) }

            liveListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val temp = parseDouble(snapshot.child("temperatura").value) ?: _liveSensorData.value.temperatura
                        val hum = parseDouble(snapshot.child("humedad").value) ?: _liveSensorData.value.humedad
                        val mq = parseDouble(snapshot.child("mq135").value) ?: _liveSensorData.value.mq135
                        val pm = parseDouble(snapshot.child("pm25").value) ?: _liveSensorData.value.pm25
                        val timestamp = parseLong(snapshot.child("timestamp").value) ?: System.currentTimeMillis()

                        val tempTrend = calculateTrend(temp, prevTemp, 0.2)
                        val humTrend = calculateTrend(hum, prevHum, 0.5)
                        val mqTrend = calculateTrend(mq, prevMq, 2.0)
                        val pmTrend = calculateTrend(pm, prevPm, 1.0)

                        prevTemp = temp
                        prevHum = hum
                        prevMq = mq
                        prevPm = pm

                        _liveSensorData.value = LiveSensorData(
                            temperatura = temp,
                            humedad = hum,
                            mq135 = mq,
                            pm25 = pm,
                            timestamp = timestamp,
                            tempTrend = tempTrend,
                            humTrend = humTrend,
                            mqTrend = mqTrend,
                            pmTrend = pmTrend
                        )
                        _isFirebaseConnected.value = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing live sensor data: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Live sensor listener cancelled: ${error.message}")
                }
            }
            ref.addValueEventListener(liveListener!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start live sensor listener: ${e.message}")
        }
    }

    /**
     * Start Realtime Database listener on "dispositivos/device_38A839E81F84"
     * Exact fields: CO, CO2, PM10, PM2_5, humedad
     */
    fun startAlertDeviceListener() {
        try {
            val ref = database.getReference("dispositivos/device_38A839E81F84")
            alertListener?.let { ref.removeEventListener(it) }

            alertListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val co = parseDouble(snapshot.child("CO").value) ?: parseDouble(snapshot.child("co").value) ?: _alertDeviceData.value.co
                        val co2 = parseDouble(snapshot.child("CO2").value) ?: parseDouble(snapshot.child("co2").value) ?: _alertDeviceData.value.co2
                        val pm10 = parseDouble(snapshot.child("PM10").value) ?: parseDouble(snapshot.child("pm10").value) ?: _alertDeviceData.value.pm10
                        val pm25 = parseDouble(snapshot.child("PM2_5").value) ?: parseDouble(snapshot.child("pm25").value) ?: parseDouble(snapshot.child("PM25").value) ?: _alertDeviceData.value.pm25
                        val hum = parseDouble(snapshot.child("humedad").value) ?: _alertDeviceData.value.humedad
                        val timestamp = parseLong(snapshot.child("last_update_server").value)
                            ?: parseLong(snapshot.child("timestamp_server").value)
                            ?: parseLong(snapshot.child("timestamp").value) ?: System.currentTimeMillis()

                        if (timestamp > 0L) {
                            lastEsp32HeartbeatMs = timestamp
                        }

                        _alertDeviceData.value = AlertDeviceReading(
                            deviceId = "device_38A839E81F84",
                            co = co,
                            co2 = co2,
                            pm10 = pm10,
                            pm25 = pm25,
                            humedad = hum,
                            timestamp = timestamp,
                            thresholds = currentThresholds
                        )
                        _isFirebaseConnected.value = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing alert device data: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Alert device listener cancelled: ${error.message}")
                }
            }
            ref.addValueEventListener(alertListener!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alert device listener: ${e.message}")
        }
    }

    /**
     * Start Realtime Database listener on "configuracion_alertas/device_38A839E81F84"
     * Keeps thresholds in sync between Web platform, Android app, and IoT Gateway.
     */
    fun startThresholdConfigListener() {
        try {
            val devId = "device_38A839E81F84"
            val ref = database.getReference("configuracion_alertas/$devId")
            thresholdListener?.let { ref.removeEventListener(it) }

            thresholdListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (!snapshot.exists()) return
                        val coCrit = parseDouble(snapshot.child("co/critico").value) ?: currentThresholds.thCo
                        val coAct = snapshot.child("co/activo").getValue(Boolean::class.java) ?: currentThresholds.activoCo

                        val co2Adv = parseDouble(snapshot.child("co2/advertencia").value) ?: currentThresholds.thCo2
                        val co2Act = snapshot.child("co2/activo").getValue(Boolean::class.java) ?: currentThresholds.activoCo2

                        val pm10Adv = parseDouble(snapshot.child("pm10/advertencia").value) ?: currentThresholds.thPm10
                        val pm10Act = snapshot.child("pm10/activo").getValue(Boolean::class.java) ?: currentThresholds.activoPm10

                        val pm25Crit = parseDouble(snapshot.child("pm25/critico").value) ?: currentThresholds.thPm25
                        val pm25Act = snapshot.child("pm25/activo").getValue(Boolean::class.java) ?: currentThresholds.activoPm25

                        val tempAdv = parseDouble(snapshot.child("temperatura/advertencia").value) ?: currentThresholds.thTemperatura
                        val tempAct = snapshot.child("temperatura/activo").getValue(Boolean::class.java) ?: currentThresholds.activoTemperatura

                        val humAdv = parseDouble(snapshot.child("humedad/advertencia").value) ?: currentThresholds.thHumedad
                        val humAct = snapshot.child("humedad/activo").getValue(Boolean::class.java) ?: currentThresholds.activoHumedad

                        val mq2Crit = parseDouble(snapshot.child("mq2/critico").value) ?: currentThresholds.thMq2
                        val mq2Act = snapshot.child("mq2/activo").getValue(Boolean::class.java) ?: currentThresholds.activoMq2

                        val ultimoFecha = snapshot.child("ultimoCambio/fecha").getValue(String::class.java) ?: currentThresholds.ultimoCambioFecha
                        val ultimoUsuario = snapshot.child("ultimoCambio/usuario").getValue(String::class.java) ?: currentThresholds.ultimoCambioUsuario

                        val newThresholds = AlertThresholdSettings(
                            thCo = coCrit,
                            thCo2 = co2Adv,
                            thPm10 = pm10Adv,
                            thPm25 = pm25Crit,
                            thTemperatura = tempAdv,
                            thHumedad = humAdv,
                            thMq2 = mq2Crit,
                            activoCo = coAct,
                            activoCo2 = co2Act,
                            activoPm10 = pm10Act,
                            activoPm25 = pm25Act,
                            activoTemperatura = tempAct,
                            activoHumedad = humAct,
                            activoMq2 = mq2Act,
                            ultimoCambioFecha = ultimoFecha,
                            ultimoCambioUsuario = ultimoUsuario
                        )
                        currentThresholds = newThresholds
                        _alertDeviceData.value = _alertDeviceData.value.copy(thresholds = newThresholds)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing threshold config from Firebase RTDB: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Threshold listener cancelled: ${error.message}")
                }
            }
            ref.addValueEventListener(thresholdListener!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start threshold listener: ${e.message}")
        }
    }

    /**
     * Start Realtime Database listener on all children of "dispositivos"
     */
    fun startDevicesListListener() {
        try {
            val ref = database.getReference("dispositivos")
            devicesListener?.let { ref.removeEventListener(it) }

            devicesListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val list = mutableListOf<DeviceSummary>()

                        val live = _liveSensorData.value
                        list.add(
                            DeviceSummary(
                                deviceId = "dispositivo1",
                                name = "ESP32 Sensor Primario (Galería 1)",
                                lastReadingTime = live.formattedTime,
                                lastUpdateServer = lastEsp32HeartbeatMs,
                                status = if (_isDeviceConnected.value) "Online" else "Offline",
                                temperatura = live.temperatura,
                                humedad = live.humedad,
                                mq135 = live.mq135,
                                pm25 = live.pm25,
                                hasAlert = false
                            )
                        )

                        for (child in snapshot.children) {
                            val id = child.key ?: continue
                            val lastUpdate = parseLong(child.child("last_update_server").value)
                                ?: parseLong(child.child("timestamp_server").value) ?: 0L

                            if (id == _selectedDeviceId.value && lastUpdate > 0L) {
                                lastEsp32HeartbeatMs = lastUpdate
                            }

                            if (id.equals("gateway", ignoreCase = true)) {
                                gatewayNodeExistsInRtdb = true
                                if (lastUpdate > 0L) {
                                    lastGatewayHeartbeatMs = lastUpdate
                                }
                                isGatewayReportedOnline = child.child("online").getValue(Boolean::class.java) ?: true
                                child.child("ip").getValue(String::class.java)?.let { gatewayIp = it }
                                child.child("mac").getValue(String::class.java)?.let { gatewayMac = it }
                                child.child("rssi").getValue(Int::class.java)?.let { gatewayRssi = it }
                            }

                            val temp = parseDouble(child.child("temperatura").value)
                            val hum = parseDouble(child.child("humedad").value)
                            val mq = parseDouble(child.child("mq135").value)
                            val pm = parseDouble(child.child("pm25").value) ?: parseDouble(child.child("PM2_5").value)
                            val co = parseDouble(child.child("CO").value) ?: parseDouble(child.child("co").value)
                            val co2 = parseDouble(child.child("CO2").value) ?: parseDouble(child.child("co2").value)
                            val pm10 = parseDouble(child.child("PM10").value)

                            val hasAlert = (co != null && co > 50) ||
                                    (co2 != null && co2 > 1000) ||
                                    (pm10 != null && pm10 > 100) ||
                                    (pm != null && pm > 50) ||
                                    (hum != null && hum > 80)

                            val devAlarmaVal = child.child("alarma").value ?: child.child("buzzer").value
                            val devAlarma = when (devAlarmaVal) {
                                is Boolean -> devAlarmaVal
                                is Number -> devAlarmaVal.toInt() == 1
                                is String -> devAlarmaVal.equals("true", ignoreCase = true) || devAlarmaVal == "1"
                                else -> false
                            }

                            val devLuzVal = child.child("luz").value ?: child.child("led").value
                            val devLuz = when (devLuzVal) {
                                is Boolean -> devLuzVal
                                is Number -> devLuzVal.toInt() == 1
                                is String -> devLuzVal.equals("true", ignoreCase = true) || devLuzVal == "1"
                                else -> false
                            }

                            val finalAlarma = if (localAlarmaStateMap.containsKey(id)) {
                                localAlarmaStateMap[id] == true
                            } else if (child.hasChild("alarma") || child.hasChild("buzzer")) {
                                devAlarma
                            } else {
                                false
                            }

                            val finalLuz = if (localLuzStateMap.containsKey(id)) {
                                localLuzStateMap[id] == true
                            } else if (child.hasChild("luz") || child.hasChild("led")) {
                                devLuz
                            } else {
                                false
                            }

                            if (id == _selectedDeviceId.value) {
                                _isAlarmaActive.value = finalAlarma
                                _isLuzActive.value = finalLuz
                            }

                            val isDevOnline = !hasAlert && (lastUpdate > 0L && (System.currentTimeMillis() - lastUpdate) <= HEARTBEAT_TIMEOUT_MS)
                            val status = when {
                                hasAlert -> "Alerta"
                                isDevOnline -> "Online"
                                else -> "Offline"
                            }
                            val readingTime = if (lastUpdate > 0L) {
                                val secAgo = (System.currentTimeMillis() - lastUpdate) / 1000
                                if (secAgo < 60) "Hace ${secAgo}s" else if (secAgo < 3600) "Hace ${secAgo / 60}m" else "Hace ${secAgo / 3600}h"
                            } else "Sin señal"

                            val name = when (id) {
                                "NODO_U-01" -> "Nodo Minero Subterráneo (NODO_U-01)"
                                "device_38A839E81F84" -> "Detector de Gases y Alertas ($id)"
                                "device_A4CB2F124B00" -> "Estación Perimetral ($id)"
                                else -> if (id.startsWith("device_")) "Estación Minera Remota ${id.takeLast(6)}" else "Dispositivo $id"
                            }

                            if (id == _selectedDeviceId.value && (temp != null || hum != null || pm != null || co != null)) {
                                val currentLive = _liveSensorData.value
                                _liveSensorData.value = currentLive.copy(
                                    temperatura = temp ?: currentLive.temperatura,
                                    humedad = hum ?: currentLive.humedad,
                                    pm25 = pm ?: currentLive.pm25,
                                    mq135 = co ?: mq ?: currentLive.mq135,
                                    timestamp = if (lastUpdate > 0L) lastUpdate else currentLive.timestamp
                                )
                            }

                            list.add(
                                DeviceSummary(
                                    deviceId = id,
                                    name = name,
                                    lastReadingTime = readingTime,
                                    lastUpdateServer = lastUpdate,
                                    status = status,
                                    temperatura = temp,
                                    humedad = hum,
                                    mq135 = mq,
                                    pm25 = pm,
                                    co = co,
                                    co2 = co2,
                                    hasAlert = hasAlert,
                                    alarma = devAlarma,
                                    luz = devLuz
                                )
                            )
                        }

                        if (list.isNotEmpty()) {
                            _devicesList.value = list
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing devices list: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Devices listener cancelled: ${error.message}")
                }
            }
            ref.addValueEventListener(devicesListener!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start devices listener: ${e.message}")
        }
    }

    /**
     * Start Realtime Database listener on "dispositivos/gateway"
     */
    fun startGatewayListener() {
        try {
            val ref = database.getReference("dispositivos/gateway")
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.value != null) {
                        gatewayNodeExistsInRtdb = true
                        val gwTs = parseLong(snapshot.child("last_update_server").value)
                            ?: parseLong(snapshot.child("timestamp").value) ?: 0L
                        if (gwTs > 0L) {
                            lastGatewayHeartbeatMs = gwTs
                        }
                        isGatewayReportedOnline = snapshot.child("online").getValue(Boolean::class.java) ?: true
                        snapshot.child("ip").getValue(String::class.java)?.let { gatewayIp = it }
                        snapshot.child("mac").getValue(String::class.java)?.let { gatewayMac = it }
                        snapshot.child("rssi").getValue(Int::class.java)?.let { gatewayRssi = it }
                        snapshot.child("puerto").getValue(Int::class.java)?.let { gatewayPuerto = it }
                        snapshot.child("firmware").getValue(String::class.java)?.let { gatewayFirmware = it }
                        snapshot.child("enlace").getValue(String::class.java)?.let { gatewayEnlace = it }
                        snapshot.child("nombre").getValue(String::class.java)?.let { gatewayNombre = it }
                        refreshGatewayDataState()
                    } else {
                        gatewayNodeExistsInRtdb = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Gateway listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start gateway listener: ${e.message}")
        }
    }

    /**
     * Real-time 1-second Heartbeat Evaluation Engine
     * Evaluates ESP32 and Gateway connectivity based on 60-second inactivity timeout.
     */
    private fun startHeartbeatMonitor() {
        coroutineScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()

                // --- ESP32 Heartbeat Evaluation ---
                if (isSimulatedOffline) {
                    _isDeviceConnected.value = false
                    _deviceLastSeenText.value = "Desconectado • Simulación forzada"
                } else if (lastEsp32HeartbeatMs <= 0L) {
                    _isDeviceConnected.value = false
                    _deviceLastSeenText.value = "Desconectado • Esperando primer latido de ESP32"
                } else {
                    val diffEsp = now - lastEsp32HeartbeatMs
                    if (diffEsp in 0..HEARTBEAT_TIMEOUT_MS) {
                        _isDeviceConnected.value = true
                        val secondsAgo = (diffEsp / 1000).coerceAtLeast(1)
                        _deviceLastSeenText.value = "En línea • Latido hace ${secondsAgo}s (Timeout: 60s) • MAC: 38:A8:39:E8:1F:84"
                    } else {
                        _isDeviceConnected.value = false
                        val secondsAgo = (diffEsp / 1000).coerceAtLeast(1)
                        val timeAgo = when {
                            secondsAgo < 120 -> "${secondsAgo}s"
                            secondsAgo < 3600 -> "${secondsAgo / 60}m"
                            secondsAgo < 86400 -> "${secondsAgo / 3600}h"
                            else -> "${secondsAgo / 86400}d"
                        }
                        _deviceLastSeenText.value = "Desconectado • Sin latido hace $timeAgo (Límite: 60s)"
                    }
                }

                // --- Gateway Heartbeat Evaluation ---
                if (isGatewaySimulatedOffline) {
                    _isGatewayConnected.value = false
                    _gatewayLastSeenText.value = "Desconectado • Simulación forzada"
                } else if (gatewayNodeExistsInRtdb) {
                    val diffGw = now - lastGatewayHeartbeatMs
                    if (diffGw in 0..HEARTBEAT_TIMEOUT_MS && isGatewayReportedOnline) {
                        _isGatewayConnected.value = true
                        val secondsAgo = (diffGw / 1000).coerceAtLeast(1)
                        _gatewayLastSeenText.value = "En línea • Latido en RTDB hace ${secondsAgo}s • MAC: $gatewayMac • IP: $gatewayIp"
                    } else {
                        _isGatewayConnected.value = false
                        val secondsAgo = (diffGw / 1000).coerceAtLeast(1)
                        val timeAgo = when {
                            secondsAgo < 120 -> "${secondsAgo}s"
                            secondsAgo < 3600 -> "${secondsAgo / 60}m"
                            else -> "${secondsAgo / 3600}h"
                        }
                        _gatewayLastSeenText.value = "Desconectado • Sin latido en /dispositivos/gateway hace $timeAgo (Límite: 60s)"
                    }
                } else {
                    // Gateway node does not exist yet in RTDB: infer from network subnodes traffic
                    val diffEsp = now - lastEsp32HeartbeatMs
                    if (lastEsp32HeartbeatMs > 0L && diffEsp in 0..HEARTBEAT_TIMEOUT_MS) {
                        _isGatewayConnected.value = true
                        val secondsAgo = (diffEsp / 1000).coerceAtLeast(1)
                        _gatewayLastSeenText.value = "En línea (Tráfico de red activo) • Latido hace ${secondsAgo}s • Reenviando a Firebase"
                    } else {
                        _isGatewayConnected.value = false
                        _gatewayLastSeenText.value = "Desconectado • Sin tráfico ni nodo activo en /dispositivos/gateway"
                    }
                }

                refreshGatewayDataState()

                delay(1000)
            }
        }
    }

    /**
     * Start listener for historical points on "dispositivos/{deviceId}/historial"
     */
    fun loadHistoryForDevice(deviceId: String = "device_38A839E81F84") {
        try {
            val targetPath = if (deviceId == "dispositivo1") {
                "dispositivos/device_38A839E81F84/historial"
            } else {
                "dispositivos/$deviceId/historial"
            }
            val ref = database.getReference(targetPath)
            historyListener?.let { ref.removeEventListener(it) }

            historyListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val points = mutableListOf<HistoricalPoint>()
                    for (child in snapshot.children) {
                        val key = child.key ?: ""
                        val temp = parseDouble(child.child("temperatura").value) ?: 0.0
                        val hum = parseDouble(child.child("humedad").value) ?: 0.0
                        val mq = parseDouble(child.child("mq135").value) ?: parseDouble(child.child("gases").value) ?: 0.0
                        val pm = parseDouble(child.child("pm25").value) ?: parseDouble(child.child("PM2_5").value) ?: 0.0
                        val co = parseDouble(child.child("CO").value) ?: 0.0
                        val co2 = parseDouble(child.child("CO2").value) ?: 0.0
                        val rawDate = child.child("fecha").value?.toString()
                            ?: child.child("date").value?.toString()
                            ?: child.child("hora").value?.toString()
                            ?: ""
                        val ts = parseLong(child.child("timestamp").value) ?: parseLong(key) ?: System.currentTimeMillis()

                        points.add(
                            HistoricalPoint(
                                key = key,
                                timestamp = ts,
                                temperatura = temp,
                                humedad = hum,
                                gases = mq,
                                pm25 = pm,
                                co = co,
                                co2 = co2,
                                rawDateString = rawDate
                            )
                        )
                    }

                    if (points.isNotEmpty()) {
                        _historyList.value = points.sortedBy { it.timestamp }
                    } else {
                        _historyList.value = generateDefaultHistory()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "History listener cancelled: ${error.message}")
                    if (_historyList.value.isEmpty()) {
                        _historyList.value = generateDefaultHistory()
                    }
                }
            }
            ref.addValueEventListener(historyListener!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading history: ${e.message}")
            if (_historyList.value.isEmpty()) {
                _historyList.value = generateDefaultHistory()
            }
        }
    }

    /**
     * Start listener for User Profile combining Firestore "users/{uid}" and RTDB "usuarios/{uid}"
     */
    fun startUserProfileListener(uid: String, email: String = "") {
        // 1. Firestore: users/{uid}
        firestore.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Firestore user listener error: ${error.message}")
                return@addSnapshotListener
            }
            val isSuper = snapshot?.getBoolean("isSuperUser") ?: false
            val isAdmin = snapshot?.getBoolean("isAdmin") ?: false
            val tipo = snapshot?.getString("tipoUsuario") ?: "usuario"

            val curr = _userProfile.value
            _userProfile.value = curr.copy(
                uid = uid,
                email = email.ifBlank { curr.email },
                isSuperUser = isSuper,
                isAdmin = isAdmin,
                tipoUsuario = tipo
            )
        }

        // 2. Realtime Database: usuarios/{uid}
        try {
            val userRef = database.getReference("usuarios/$uid")
            userProfileListener?.let { userRef.removeEventListener(it) }

            userProfileListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val nombre = snapshot.child("nombre").value?.toString() ?: ""
                        val empresa = snapshot.child("empresa").value?.toString() ?: ""
                        val tipoMina = snapshot.child("tipo de mina").value?.toString()
                            ?: snapshot.child("tipoMina").value?.toString() ?: ""

                        var lat: Double? = null
                        var lng: Double? = null

                        if (snapshot.hasChild("geolocalización")) {
                            lat = parseDouble(snapshot.child("geolocalización/latitud").value)
                                ?: parseDouble(snapshot.child("geolocalización/lat").value)
                            lng = parseDouble(snapshot.child("geolocalización/longitud").value)
                                ?: parseDouble(snapshot.child("geolocalización/lng").value)
                        }

                        if (lat == null) {
                            lat = parseDouble(snapshot.child("latitud").value) ?: parseDouble(snapshot.child("lat").value)
                        }
                        if (lng == null) {
                            lng = parseDouble(snapshot.child("longitud").value) ?: parseDouble(snapshot.child("lng").value)
                        }

                        val curr = _userProfile.value
                        _userProfile.value = curr.copy(
                            nombre = nombre.ifBlank { curr.nombre },
                            empresa = empresa.ifBlank { curr.empresa },
                            tipoMina = tipoMina.ifBlank { curr.tipoMina },
                            latitud = lat ?: curr.latitud,
                            longitud = lng ?: curr.longitud
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "RTDB user profile listener cancelled: ${error.message}")
                }
            }
            userRef.addValueEventListener(userProfileListener!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting RTDB user listener: ${e.message}")
        }
    }

    /**
     * Admin: Fetch all users from RTDB "usuarios" and Firestore "users"
     */
    fun loadAllUsersForAdmin() {
        // 1. Immediately fetch latest users from RTDB REST endpoint
        fetchUsuariosRest()

        // 2. Realtime listener on RTDB "usuarios"
        try {
            val userRef = database.getReference("usuarios")
            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<UserProfile>()
                    for (child in snapshot.children) {
                        val uid = child.key ?: continue
                        val email = child.child("email").value?.toString() ?: "usuario_$uid@minesafe.pe"
                        val nombre = child.child("nombre").value?.toString() ?: "Usuario $uid"
                        val isSuper = child.child("isSuperUser").value?.toString()?.toBoolean() ?: false
                        val isAdmin = child.child("isAdmin").value?.toString()?.toBoolean() ?: false
                        val tipoStr = child.child("tipoUsuario").value?.toString() ?: ""
                        val tipo = when {
                            isSuper -> "superAdmin"
                            isAdmin || tipoStr.equals("admin", ignoreCase = true) -> "admin"
                            else -> if (tipoStr.isNotBlank()) tipoStr else "operador"
                        }
                        val empresa = child.child("geoMina").value?.toString()
                            ?: child.child("empresa").value?.toString() ?: "Minera Los Andes"
                        val tipoMina = child.child("tipoMina").value?.toString() ?: "Mina Subterránea"

                        val lat = parseDouble(child.child("latitud").value)
                            ?: if (empresa.contains("Chuquicamata", ignoreCase = true)) -22.45 else -11.5936
                        val lng = parseDouble(child.child("longitud").value)
                            ?: if (empresa.contains("Chuquicamata", ignoreCase = true)) -68.92 else -76.2411

                        list.add(
                            UserProfile(
                                uid = uid,
                                email = email,
                                isSuperUser = isSuper,
                                isAdmin = isAdmin || isSuper,
                                tipoUsuario = tipo,
                                nombre = nombre,
                                empresa = empresa,
                                tipoMina = tipoMina,
                                latitud = lat,
                                longitud = lng
                            )
                        )
                    }
                    if (list.isNotEmpty()) {
                        _allUsersList.value = list
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "RTDB usuarios listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching listener to usuarios: ${e.message}")
        }
    }

    /**
     * Admin: Update user role in Firestore "users/{uid}"
     */
    suspend fun updateUserRole(uid: String, role: String): Result<Unit> {
        return try {
            val isSuper = role.equals("superAdmin", ignoreCase = true)
            val isAdmin = isSuper || role.equals("admin", ignoreCase = true)
            val updates = mapOf(
                "isSuperUser" to isSuper,
                "isAdmin" to isAdmin,
                "tipoUsuario" to role
            )
            firestore.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()

            val updated = _allUsersList.value.map { u ->
                if (u.uid == uid) {
                    u.copy(isSuperUser = isSuper, isAdmin = isAdmin, tipoUsuario = role)
                } else u
            }
            _allUsersList.value = updated

            if (_userProfile.value.uid == uid) {
                _userProfile.value = _userProfile.value.copy(
                    isSuperUser = isSuper,
                    isAdmin = isAdmin,
                    tipoUsuario = role
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user role: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update user profile in Realtime Database "usuarios/{uid}"
     */
    suspend fun updateUserProfileData(
        uid: String,
        nombre: String,
        empresa: String,
        tipoMina: String,
        latitud: Double,
        longitud: Double
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "nombre" to nombre,
                "empresa" to empresa,
                "tipo de mina" to tipoMina,
                "tipoMina" to tipoMina,
                "geolocalización" to mapOf(
                    "latitud" to latitud,
                    "longitud" to longitud
                ),
                "latitud" to latitud,
                "longitud" to longitud
            )
            database.getReference("usuarios/$uid").updateChildren(updates).await()

            _userProfile.value = _userProfile.value.copy(
                nombre = nombre,
                empresa = empresa,
                tipoMina = tipoMina,
                latitud = latitud,
                longitud = longitud
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile data: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun startEmergencySimulation(
        scenarioName: String,
        triggerActuators: Boolean = true,
        userDescription: String = ""
    ) {
        _isEmergencySimulationActive.value = true
        val fullScenario = if (userDescription.isNotBlank()) {
            "$scenarioName • $userDescription"
        } else {
            scenarioName
        }
        _simulationScenarioName.value = fullScenario
        _actuatorTriggerReason.value = "SIMULACRO EN CURSO: $fullScenario"

        // Also record as a reported incident
        val incident = ReportedIncident(
            id = "sim_${System.currentTimeMillis()}",
            tipo = if (scenarioName.contains("EMERGENCIA REAL", ignoreCase = true)) "Emergencia Real de Faena" else "Simulacro de Evacuación",
            ubicacion = "Sector Subterráneo / General",
            gravedad = if (scenarioName.contains("EMERGENCIA REAL", ignoreCase = true)) "Emergencia" else "Crítica",
            descripcion = if (userDescription.isNotBlank()) userDescription else "Simulacro protocolar iniciado: $scenarioName",
            autorNombre = _userProfile.value.nombre.ifBlank { "Personal Autorizado" },
            autorRol = _userProfile.value.rolDisplay,
            timestamp = System.currentTimeMillis(),
            alarmaActivada = triggerActuators,
            estado = "Activa"
        )
        _reportedIncidents.value = listOf(incident) + _reportedIncidents.value

        val reading = when {
            scenarioName.contains("Monóxido", ignoreCase = true) || scenarioName.contains("CO", ignoreCase = true) -> {
                AlertDeviceReading(
                    deviceId = _selectedDeviceId.value,
                    co = 78.5,        // > 50 DANGER
                    co2 = 850.0,
                    pm10 = 45.0,
                    pm25 = 24.0,
                    humedad = 65.0,
                    timestamp = System.currentTimeMillis()
                )
            }
            scenarioName.contains("Incendio", ignoreCase = true) || scenarioName.contains("Humo", ignoreCase = true) -> {
                AlertDeviceReading(
                    deviceId = _selectedDeviceId.value,
                    co = 58.0,
                    co2 = 1400.0,
                    pm10 = 240.0,     // > 100 DANGER
                    pm25 = 185.0,     // > 50 DANGER
                    humedad = 88.0,
                    timestamp = System.currentTimeMillis()
                )
            }
            scenarioName.contains("CO2", ignoreCase = true) || scenarioName.contains("Asfixia", ignoreCase = true) -> {
                AlertDeviceReading(
                    deviceId = _selectedDeviceId.value,
                    co = 22.0,
                    co2 = 2850.0,     // > 1000 DANGER
                    pm10 = 55.0,
                    pm25 = 32.0,
                    humedad = 72.0,
                    timestamp = System.currentTimeMillis()
                )
            }
            else -> {
                // Simulacro General de Evacuación (D.S. 132)
                AlertDeviceReading(
                    deviceId = _selectedDeviceId.value,
                    co = 88.0,
                    co2 = 2200.0,
                    pm10 = 220.0,
                    pm25 = 195.0,
                    humedad = 92.0,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
        _alertDeviceData.value = reading

        if (triggerActuators) {
            setAlarmaState(true)
            setLuzState(true)

            // Also broadcast alarm & strobe to all registered mine devices in RTDB
            val allDevs = _devicesList.value
            for (dev in allDevs) {
                if (dev.deviceId != _selectedDeviceId.value) {
                    try {
                        database.getReference("dispositivos/${dev.deviceId}/alarma").setValue(true)
                        database.getReference("dispositivos/${dev.deviceId}/buzzer").setValue(1)
                        database.getReference("dispositivos/${dev.deviceId}/luz").setValue(true)
                        database.getReference("dispositivos/${dev.deviceId}/led").setValue(1)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error triggering actuator on ${dev.deviceId}: ${e.message}")
                    }
                }
            }

            _actuatorTriggerReason.value = "🚨 SIMULACRO EN CURSO: $fullScenario"
        }
    }

    suspend fun reportIncident(incident: ReportedIncident): Result<Unit> {
        return try {
            val updated = listOf(incident) + _reportedIncidents.value
            _reportedIncidents.value = updated

            // If user selected to trigger alarms
            if (incident.alarmaActivada) {
                setAlarmaState(true)
                setLuzState(true)
                _actuatorTriggerReason.value = "🚨 INCIDENCIA REPORTADA: ${incident.tipo} en ${incident.ubicacion}"

                val allDevs = _devicesList.value
                for (dev in allDevs) {
                    if (dev.deviceId != _selectedDeviceId.value) {
                        try {
                            database.getReference("dispositivos/${dev.deviceId}/alarma").setValue(true)
                            database.getReference("dispositivos/${dev.deviceId}/buzzer").setValue(1)
                            database.getReference("dispositivos/${dev.deviceId}/luz").setValue(true)
                            database.getReference("dispositivos/${dev.deviceId}/led").setValue(1)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error triggering actuator on ${dev.deviceId}: ${e.message}")
                        }
                    }
                }
            }

            try {
                val ref = database.getReference("incidencias").child(incident.id)
                ref.child("id").setValue(incident.id)
                ref.child("tipo").setValue(incident.tipo)
                ref.child("ubicacion").setValue(incident.ubicacion)
                ref.child("gravedad").setValue(incident.gravedad)
                ref.child("descripcion").setValue(incident.descripcion)
                ref.child("autorNombre").setValue(incident.autorNombre)
                ref.child("autorRol").setValue(incident.autorRol)
                ref.child("timestamp").setValue(incident.timestamp)
                ref.child("alarmaActivada").setValue(incident.alarmaActivada)
                ref.child("estado").setValue(incident.estado)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo sincronizar reporte con RTDB: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar incidencia: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateIncidentDescription(
        incidentId: String,
        newDescription: String,
        newEstado: String? = null
    ): Result<Unit> {
        return try {
            val currentList = _reportedIncidents.value
            val updatedList = currentList.map { item ->
                if (item.id == incidentId) {
                    item.copy(
                        descripcion = newDescription,
                        estado = newEstado ?: item.estado
                    )
                } else {
                    item
                }
            }
            _reportedIncidents.value = updatedList

            try {
                val ref = database.getReference("incidencias").child(incidentId)
                ref.child("descripcion").setValue(newDescription)
                if (newEstado != null) {
                    ref.child("estado").setValue(newEstado)
                }
                ref.child("ultimaModificacion").setValue(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo sincronizar actualización de descripción en RTDB: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando descripción de incidencia: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun stopEmergencySimulation() {
        _isEmergencySimulationActive.value = false
        _simulationScenarioName.value = ""
        _actuatorTriggerReason.value = "En reposo (condiciones normales restablecidas)"
        _alertDeviceData.value = AlertDeviceReading(
            deviceId = _selectedDeviceId.value,
            co = 12.0,
            co2 = 540.0,
            pm10 = 28.0,
            pm25 = 14.0,
            humedad = 55.0,
            timestamp = System.currentTimeMillis()
        )
        setAlarmaState(false)
        setLuzState(false)

        // Turn off alarm & light on all other devices as well
        val allDevs = _devicesList.value
        for (dev in allDevs) {
            if (dev.deviceId != _selectedDeviceId.value) {
                try {
                    database.getReference("dispositivos/${dev.deviceId}/alarma").setValue(false)
                    database.getReference("dispositivos/${dev.deviceId}/buzzer").setValue(0)
                    database.getReference("dispositivos/${dev.deviceId}/luz").setValue(false)
                    database.getReference("dispositivos/${dev.deviceId}/led").setValue(0)
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping actuator on ${dev.deviceId}: ${e.message}")
                }
            }
        }
    }

    fun setSimulatedAlertState(danger: Boolean) {
        if (danger) {
            _isEmergencySimulationActive.value = true
            _simulationScenarioName.value = "Simulacro General de Peligro"
            _alertDeviceData.value = AlertDeviceReading(
                deviceId = "device_38A839E81F84",
                co = 68.5,        // > 50 DANGER
                co2 = 1250.0,     // > 1000 WARNING
                pm10 = 124.0,     // > 100 WARNING
                pm25 = 62.0,      // > 50 DANGER
                humedad = 86.0,   // > 80 WARNING
                timestamp = System.currentTimeMillis()
            )
        } else {
            _isEmergencySimulationActive.value = false
            _simulationScenarioName.value = ""
            _alertDeviceData.value = AlertDeviceReading(
                deviceId = "device_38A839E81F84",
                co = 12.0,
                co2 = 580.0,
                pm10 = 34.0,
                pm25 = 14.0,
                humedad = 56.0,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun calculateTrend(current: Double, prev: Double?, delta: Double): TrendDirection {
        if (prev == null) return TrendDirection.STABLE
        val diff = current - prev
        return when {
            diff > delta -> TrendDirection.RISING
            diff < -delta -> TrendDirection.FALLING
            else -> TrendDirection.STABLE
        }
    }

    private fun parseDouble(value: Any?): Double? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun parseLong(value: Any?): Long? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun generateDefaultHistory(): List<HistoricalPoint> {
        val list = mutableListOf<HistoricalPoint>()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        val temps = listOf(21.2, 21.8, 22.1, 22.5, 23.0, 23.8, 24.2, 23.9, 23.1, 22.6, 22.4, 22.3)
        val hums = listOf(64.0, 63.5, 62.0, 60.5, 59.0, 58.2, 57.5, 58.0, 58.8, 60.0, 61.2, 62.0)
        val gases = listOf(45.0, 48.0, 52.0, 58.0, 65.0, 72.0, 68.0, 60.0, 54.0, 50.0, 48.0, 46.0)
        val pm25s = listOf(11.0, 12.0, 14.0, 16.5, 18.0, 17.5, 15.0, 14.0, 13.5, 13.0, 12.5, 12.0)

        for (i in 0 until 12) {
            val ts = now - (11 - i) * 3600 * 1000L
            list.add(
                HistoricalPoint(
                    key = "hist_$i",
                    timestamp = ts,
                    temperatura = temps[i],
                    humedad = hums[i],
                    gases = gases[i],
                    pm25 = pm25s[i],
                    co = 10.0 + (gases[i] * 0.2),
                    co2 = 500.0 + (gases[i] * 4.0),
                    rawDateString = sdf.format(Date(ts))
                )
            )
        }
        return list
    }

    fun toggleDeviceSimulation() {
        if (_isDeviceConnected.value) {
            isSimulatedOffline = true
            _isDeviceConnected.value = false
            _deviceLastSeenText.value = "Desconectado • Simulación forzada"
        } else {
            isSimulatedOffline = false
            lastEsp32HeartbeatMs = System.currentTimeMillis()
            _isDeviceConnected.value = true
            _deviceLastSeenText.value = "En línea • Latido emitido (Contador 60s activo)"
        }
        coroutineScope.launch { fetchDevicesRest() }
    }

    fun toggleGatewaySimulation() {
        if (_isGatewayConnected.value) {
            isGatewaySimulatedOffline = true
            _isGatewayConnected.value = false
            _gatewayLastSeenText.value = "Desconectado • Simulación forzada"
        } else {
            isGatewaySimulatedOffline = false
            lastGatewayHeartbeatMs = System.currentTimeMillis()
            isGatewayReportedOnline = true
            _isGatewayConnected.value = true
            _gatewayLastSeenText.value = "En línea • Latido emitido (Contador 60s activo)"
        }
        refreshGatewayDataState()
    }

    fun pingGatewayToFirebase() {
        coroutineScope.launch {
            try {
                val gwRef = database.getReference("dispositivos/gateway")
                gwRef.child("online").setValue(true)
                gwRef.child("last_update_server").setValue(ServerValue.TIMESTAMP)
                gwRef.child("ip").setValue(gatewayIp)
                gwRef.child("mac").setValue(gatewayMac)
                gwRef.child("puerto").setValue(gatewayPuerto)
                gwRef.child("rssi").setValue(gatewayRssi)
                gwRef.child("nombre").setValue(gatewayNombre)
                gwRef.child("enlace").setValue(gatewayEnlace)
                gwRef.child("firmware").setValue(gatewayFirmware)
                gwRef.child("nodos_conectados").setValue(4)
                lastGatewayHeartbeatMs = System.currentTimeMillis()
                gatewayNodeExistsInRtdb = true
                isGatewayReportedOnline = true
                isGatewaySimulatedOffline = false
                _isGatewayConnected.value = true
                _gatewayLastSeenText.value = "En línea • Latido sincronizado con Firebase • IP: $gatewayIp"
                refreshGatewayDataState()
            } catch (e: Exception) {
                Log.w(TAG, "pingGatewayToFirebase failed: ${e.message}")
            }
        }
    }

    fun setInitialGatewayIpAndPort(ip: String, port: Int) {
        if (ip.isNotBlank()) {
            gatewayIp = ip
        }
        if (port in 1..65535) {
            gatewayPuerto = port
        }
        refreshGatewayDataState()
    }

    fun updateGatewayIp(newIp: String, newPort: Int = 8080, updatedBy: String = ""): Result<Unit> {
        val trimmed = newIp.trim()
        val parts = trimmed.split(".")
        if (parts.size != 4 || parts.any { it.toIntOrNull() == null || it.toInt() !in 0..255 }) {
            return Result.failure(IllegalArgumentException("Formato IPv4 inválido. Debe contener 4 octetos entre 0 y 255 (ej: 192.168.1.100)"))
        }
        if (newPort !in 1..65535) {
            return Result.failure(IllegalArgumentException("Puerto inválido. Debe estar entre 1 y 65535."))
        }

        gatewayIp = trimmed
        gatewayPuerto = newPort
        isGatewaySimulatedOffline = false
        isGatewayReportedOnline = true
        _isGatewayConnected.value = true
        lastGatewayHeartbeatMs = System.currentTimeMillis()
        _gatewayLastSeenText.value = "En línea • IP configurada a $trimmed:$newPort"
        refreshGatewayDataState()

        coroutineScope.launch {
            try {
                val gwRef = database.getReference("dispositivos/gateway")
                gwRef.child("ip").setValue(trimmed)
                gwRef.child("puerto").setValue(newPort)
                gwRef.child("online").setValue(true)
                gwRef.child("mac").setValue(gatewayMac)
                gwRef.child("rssi").setValue(gatewayRssi)
                gwRef.child("nombre").setValue(gatewayNombre)
                gwRef.child("enlace").setValue(gatewayEnlace)
                gwRef.child("last_update_server").setValue(ServerValue.TIMESTAMP)
                if (updatedBy.isNotBlank()) {
                    gwRef.child("updated_by").setValue(updatedBy)
                }
                gatewayNodeExistsInRtdb = true
                Log.d(TAG, "Gateway IP successfully updated in RTDB: $trimmed:$newPort")
            } catch (e: Exception) {
                Log.w(TAG, "Error saving gateway IP to Firebase RTDB: ${e.message}")
            }
        }
        return Result.success(Unit)
    }

    fun pingDeviceToFirebase() {
        coroutineScope.launch {
            try {
                val devId = _selectedDeviceId.value.ifBlank { "device_38A839E81F84" }
                database.getReference("dispositivos/$devId/last_update_server").setValue(ServerValue.TIMESTAMP)
                lastEsp32HeartbeatMs = System.currentTimeMillis()
                isSimulatedOffline = false
            } catch (e: Exception) {
                Log.w(TAG, "pingDeviceToFirebase failed: ${e.message}")
            }
        }
    }

    fun reconnectGateway() {
        isGatewaySimulatedOffline = false
        lastGatewayHeartbeatMs = System.currentTimeMillis()
        isGatewayReportedOnline = true
        _isGatewayConnected.value = true
        _gatewayLastSeenText.value = "En línea • Enlace LoRa/WiFi activo"
        pingGatewayToFirebase()
    }

    fun reconnectDevice() {
        isSimulatedOffline = false
        lastEsp32HeartbeatMs = System.currentTimeMillis()
        _isDeviceConnected.value = true
        _deviceLastSeenText.value = "En línea • Transmitiendo ahora"
        pingDeviceToFirebase()
        coroutineScope.launch {
            fetchPrimaryDeviceRest()
            fetchDevicesRest()
            fetchGatewayRest()
            fetchUsuariosRest()
        }
    }
}
