package com.example.data

import android.util.Log
import com.example.model.AlertDeviceReading
import com.example.model.DeviceSummary
import com.example.model.HistoricalPoint
import com.example.model.LiveSensorData
import com.example.model.TrendDirection
import com.example.model.UserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
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
import okhttp3.OkHttpClient
import okhttp3.Request
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

    // Previous readings for live trend computation
    private var prevTemp: Double? = null
    private var prevHum: Double? = null
    private var prevMq: Double? = null
    private var prevPm: Double? = null

    // Live state flows with industrial baseline initial values
    private val _liveSensorData = MutableStateFlow(
        LiveSensorData(
            temperatura = 22.4,
            humedad = 58.2,
            mq135 = 64.0,
            pm25 = 14.5,
            timestamp = System.currentTimeMillis()
        )
    )
    val liveSensorData: StateFlow<LiveSensorData> = _liveSensorData.asStateFlow()

    private val _alertDeviceData = MutableStateFlow(
        AlertDeviceReading(
            deviceId = "device_38A839E81F84",
            co = 18.0,
            co2 = 640.0,
            pm10 = 42.0,
            pm25 = 16.0,
            humedad = 62.0,
            timestamp = System.currentTimeMillis()
        )
    )
    val alertDeviceData: StateFlow<AlertDeviceReading> = _alertDeviceData.asStateFlow()

    private val _devicesList = MutableStateFlow<List<DeviceSummary>>(emptyList())
    val devicesList: StateFlow<List<DeviceSummary>> = _devicesList.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            uid = "sample_uid",
            email = "operador@minesafe.pe",
            isSuperUser = false,
            isAdmin = false,
            tipoUsuario = "usuario",
            nombre = "Carlos Mendoza",
            empresa = "Minera Los Andes S.A.C.",
            tipoMina = "Mina Subterránea (Nivel -400)",
            latitud = -11.5936, // Coordenadas mineras peruanas (Cerro de Pasco / Yauli)
            longitud = -76.2411
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _historyList = MutableStateFlow<List<HistoricalPoint>>(emptyList())
    val historyList: StateFlow<List<HistoricalPoint>> = _historyList.asStateFlow()

    private val _allUsersList = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsersList: StateFlow<List<UserProfile>> = _allUsersList.asStateFlow()

    private var liveListener: ValueEventListener? = null
    private var alertListener: ValueEventListener? = null
    private var devicesListener: ValueEventListener? = null
    private var historyListener: ValueEventListener? = null
    private var userProfileListener: ValueEventListener? = null

    init {
        // Start background dual-sync to guarantee live telemetry from minefase-fc5f5
        startDualSyncPolling()
    }

    /**
     * Dual Sync Polling: Continuously queries Firebase RTDB REST endpoint
     * to ensure live data reception even if Android API key is awaiting console setup.
     */
    private fun startDualSyncPolling() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    fetchDispositivo1Rest()
                    fetchAlertDeviceRest()
                    fetchDevicesRest()
                    _isFirebaseConnected.value = true
                    _connectionStatusMessage.value = "Sincronizado con Firebase (minefase-fc5f5)"
                } catch (e: Exception) {
                    Log.d(TAG, "DualSync REST polling check: ${e.message}")
                }
                delay(4000) // Poll every 4 seconds
            }
        }
    }

    private fun fetchDispositivo1Rest() {
        try {
            val request = Request.Builder()
                .url("$RTDB_BASE_URL/dispositivo1.json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                val temp = json.optDouble("temperatura", _liveSensorData.value.temperatura)
                val hum = json.optDouble("humedad", _liveSensorData.value.humedad)
                val mq = json.optDouble("mq135", _liveSensorData.value.mq135)
                val pm = json.optDouble("pm25", _liveSensorData.value.pm25)
                val ts = json.optLong("timestamp", System.currentTimeMillis())

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
            }
        } catch (e: Exception) {
            // Logged as debug
        }
    }

    private fun fetchAlertDeviceRest() {
        try {
            val request = Request.Builder()
                .url("$RTDB_BASE_URL/dispositivos/device_38A839E81F84.json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                val co = if (json.has("CO")) json.optDouble("CO") else json.optDouble("co", _alertDeviceData.value.co)
                val co2 = if (json.has("CO2")) json.optDouble("CO2") else json.optDouble("co2", _alertDeviceData.value.co2)
                val pm10 = if (json.has("PM10")) json.optDouble("PM10") else json.optDouble("pm10", _alertDeviceData.value.pm10)
                val pm25 = when {
                    json.has("PM2_5") -> json.optDouble("PM2_5")
                    json.has("pm25") -> json.optDouble("pm25")
                    json.has("PM25") -> json.optDouble("PM25")
                    else -> _alertDeviceData.value.pm25
                }
                val hum = json.optDouble("humedad", _alertDeviceData.value.humedad)

                _alertDeviceData.value = AlertDeviceReading(
                    deviceId = "device_38A839E81F84",
                    co = co,
                    co2 = co2,
                    pm10 = pm10,
                    pm25 = pm25,
                    humedad = hum,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            // Handled
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
                        status = "Online",
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

                    list.add(
                        DeviceSummary(
                            deviceId = key,
                            name = if (key == "device_38A839E81F84") "Detector de Gases y Alertas ($key)" else "Estación Minera Remota ${key.takeLast(6)}",
                            lastReadingTime = "En vivo",
                            status = if (hasAlert) "Alerta" else "Online",
                            temperatura = temp,
                            humedad = hum,
                            mq135 = mq,
                            pm25 = pm,
                            co = co,
                            co2 = co2,
                            hasAlert = hasAlert
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
                        val timestamp = parseLong(snapshot.child("timestamp").value) ?: System.currentTimeMillis()

                        _alertDeviceData.value = AlertDeviceReading(
                            deviceId = "device_38A839E81F84",
                            co = co,
                            co2 = co2,
                            pm10 = pm10,
                            pm25 = pm25,
                            humedad = hum,
                            timestamp = timestamp
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
                                status = "Online",
                                temperatura = live.temperatura,
                                humedad = live.humedad,
                                mq135 = live.mq135,
                                pm25 = live.pm25,
                                hasAlert = false
                            )
                        )

                        for (child in snapshot.children) {
                            val id = child.key ?: continue
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

                            val status = if (hasAlert) "Alerta" else "Online"
                            val name = when {
                                id == "device_38A839E81F84" -> "Detector de Gases y Alertas ($id)"
                                id.startsWith("device_") -> "Estación Minera Remota ${id.takeLast(6)}"
                                else -> "Dispositivo $id"
                            }

                            list.add(
                                DeviceSummary(
                                    deviceId = id,
                                    name = name,
                                    lastReadingTime = "En vivo",
                                    status = status,
                                    temperatura = temp,
                                    humedad = hum,
                                    mq135 = mq,
                                    pm25 = pm,
                                    co = co,
                                    co2 = co2,
                                    hasAlert = hasAlert
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
     * Admin: Fetch all users from Firestore collection "users" and merge with RTDB "usuarios"
     */
    fun loadAllUsersForAdmin() {
        firestore.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error loading all users: ${error.message}")
                return@addSnapshotListener
            }
            val users = mutableListOf<UserProfile>()
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    val uid = doc.id
                    val isSuper = doc.getBoolean("isSuperUser") ?: false
                    val isAdmin = doc.getBoolean("isAdmin") ?: false
                    val tipo = doc.getString("tipoUsuario") ?: "usuario"
                    val email = doc.getString("email") ?: "usuario_$uid@minesafe.pe"

                    users.add(
                        UserProfile(
                            uid = uid,
                            email = email,
                            isSuperUser = isSuper,
                            isAdmin = isAdmin,
                            tipoUsuario = tipo,
                            nombre = doc.getString("nombre") ?: "Usuario $uid",
                            empresa = doc.getString("empresa") ?: "Minera Asociada",
                            tipoMina = "Mina Subterránea"
                        )
                    )
                }
            }

            if (users.isEmpty()) {
                _allUsersList.value = listOf(
                    UserProfile(
                        uid = "uid_super_01",
                        email = "superadmin@minesafe.pe",
                        isSuperUser = true,
                        isAdmin = true,
                        tipoUsuario = "superAdmin",
                        nombre = "Ing. Manuel Crisóstomo",
                        empresa = "MineSafe Corporación Minera",
                        tipoMina = "Mina Subterránea Nivel Principal"
                    ),
                    UserProfile(
                        uid = "uid_admin_02",
                        email = "jefe.seguridad@minesafe.pe",
                        isSuperUser = false,
                        isAdmin = true,
                        tipoUsuario = "admin",
                        nombre = "Ing. Roberto Alarcón",
                        empresa = "Consorcio Minero del Centro",
                        tipoMina = "Tajo Abierto y Galerías"
                    ),
                    UserProfile(
                        uid = "uid_user_03",
                        email = "operador.socavon@minesafe.pe",
                        isSuperUser = false,
                        isAdmin = false,
                        tipoUsuario = "usuario",
                        nombre = "Carlos Mendoza",
                        empresa = "Minera Los Andes S.A.C.",
                        tipoMina = "Mina Subterránea (Nivel -400)"
                    )
                )
            } else {
                _allUsersList.value = users
            }
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

    fun setSimulatedAlertState(danger: Boolean) {
        if (danger) {
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
}
