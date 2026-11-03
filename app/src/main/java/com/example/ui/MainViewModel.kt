package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.AuthState
import com.example.data.FirebaseRepository
import com.example.model.AlertDeviceReading
import com.example.model.AlertThresholdSettings
import com.example.model.DeviceSummary
import com.example.model.GatewayData
import com.example.model.GatewayInfo
import com.example.model.HistoricalPoint
import com.example.model.LiveSensorData
import com.example.data.AiAnalysisRepository
import com.example.model.AiAnalyzeRequest
import com.example.model.AiAnalyzeResponse
import com.example.model.AiAnomaly
import com.example.model.AiHealthResponse
import com.example.model.AiMetricTrend
import com.example.model.AiObservation
import com.example.model.MiningCriticalSensorsData
import com.example.model.ReportedIncident
import com.example.model.SafetyAlert
import com.example.model.AlertSeverity
import com.example.model.UserProfile
import com.example.ui.navigation.ScreenDestination
import com.example.util.EmergencyAlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MineSafe_VM"
    private val authRepo = AuthRepository()
    private val firebaseRepo = FirebaseRepository()

    // Navigation state
    private val _currentScreen = MutableStateFlow(ScreenDestination.DASHBOARD)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Auth State
    val authState: StateFlow<AuthState> = authRepo.authState
    val currentUser = authRepo.currentUser

    // Is Logged In gate
    private val _isLoggedIn = MutableStateFlow(authRepo.currentUser.value != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Live Data from Realtime Database
    val isFirebaseConnected: StateFlow<Boolean> = firebaseRepo.isFirebaseConnected
    val connectionStatusMessage: StateFlow<String> = firebaseRepo.connectionStatusMessage
    val isDeviceConnected: StateFlow<Boolean> = firebaseRepo.isDeviceConnected
    val deviceLastSeenText: StateFlow<String> = firebaseRepo.deviceLastSeenText
    val isGatewayConnected: StateFlow<Boolean> = firebaseRepo.isGatewayConnected
    val gatewayLastSeenText: StateFlow<String> = firebaseRepo.gatewayLastSeenText
    val gatewayData: StateFlow<GatewayData> = firebaseRepo.gatewayData
    val gatewayInfo: StateFlow<GatewayData> get() = gatewayData
    val liveSensorData: StateFlow<LiveSensorData> = firebaseRepo.liveSensorData
    val alertDeviceData: StateFlow<AlertDeviceReading> = firebaseRepo.alertDeviceData
    val devicesList: StateFlow<List<DeviceSummary>> = firebaseRepo.devicesList
    val userProfile: StateFlow<UserProfile> = firebaseRepo.userProfile
    val historyList: StateFlow<List<HistoricalPoint>> = firebaseRepo.historyList
    val allUsersList: StateFlow<List<UserProfile>> = firebaseRepo.allUsersList

    // Remote Actuator Controls (Alarma & Luz)
    val selectedDeviceId: StateFlow<String> = firebaseRepo.selectedDeviceId
    val isAlarmaActive: StateFlow<Boolean> = firebaseRepo.isAlarmaActive
    val isLuzActive: StateFlow<Boolean> = firebaseRepo.isLuzActive
    val actuatorTriggerReason: StateFlow<String> = firebaseRepo.actuatorTriggerReason

    // Emergency Simulation / Evacuation Drill Controls (Admin & SuperAdmin only)
    val isEmergencySimulationActive: StateFlow<Boolean> = firebaseRepo.isEmergencySimulationActive
    val simulationScenarioName: StateFlow<String> = firebaseRepo.simulationScenarioName
    val reportedIncidents: StateFlow<List<ReportedIncident>> = firebaseRepo.reportedIncidents

    // SharedPreferences for Device Remembering & Auto-login
    private val prefs = application.getSharedPreferences("minesafe_device_prefs", Context.MODE_PRIVATE)

    private val _rememberDevice = MutableStateFlow(prefs.getBoolean("remember_device", true))
    val rememberDevice: StateFlow<Boolean> = _rememberDevice.asStateFlow()

    private val _autoLogin = MutableStateFlow(prefs.getBoolean("auto_login", true))
    val autoLogin: StateFlow<Boolean> = _autoLogin.asStateFlow()

    private val _savedEmail = MutableStateFlow(prefs.getString("saved_email", "") ?: "")
    val savedEmail: StateFlow<String> = _savedEmail.asStateFlow()

    private val _savedPassword = MutableStateFlow(prefs.getString("saved_password", "") ?: "")
    val savedPassword: StateFlow<String> = _savedPassword.asStateFlow()

    // Chart selections
    private val _selectedDeviceForChart = MutableStateFlow("device_38A839E81F84")
    val selectedDeviceForChart: StateFlow<String> = _selectedDeviceForChart.asStateFlow()

    private val _selectedMetricForChart = MutableStateFlow("temperatura") // "temperatura", "humedad", "gases", "pm25"
    val selectedMetricForChart: StateFlow<String> = _selectedMetricForChart.asStateFlow()

    // Operation status feedback message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // MineSafe IA Analysis Engine State
    private val aiRepo = AiAnalysisRepository()

    private val _isAiOnline = MutableStateFlow(false)
    val isAiOnline: StateFlow<Boolean> = _isAiOnline.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    private val _aiAnalysisResult = MutableStateFlow<AiAnalyzeResponse?>(null)
    val aiAnalysisResult: StateFlow<AiAnalyzeResponse?> = _aiAnalysisResult.asStateFlow()

    private val _aiAnalysisError = MutableStateFlow<String?>(null)
    val aiAnalysisError: StateFlow<String?> = _aiAnalysisError.asStateFlow()

    private val _aiAutoAnalyze = MutableStateFlow(true)
    val aiAutoAnalyze: StateFlow<Boolean> = _aiAutoAnalyze.asStateFlow()

    private val _aiSelectedDeviceId = MutableStateFlow("device_38A839E81F84")
    val aiSelectedDeviceId: StateFlow<String> = _aiSelectedDeviceId.asStateFlow()

    private val _aiBaseUrl = MutableStateFlow(prefs.getString("saved_ai_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000")
    val aiBaseUrl: StateFlow<String> = _aiBaseUrl.asStateFlow()

    private val _aiApiKey = MutableStateFlow(prefs.getString("saved_ai_key", "minesafe_secret_token_chile") ?: "minesafe_secret_token_chile")
    val aiApiKey: StateFlow<String> = _aiApiKey.asStateFlow()

    // Dynamic Alert Threshold Settings
    private val _alertThresholds = MutableStateFlow(
        AlertThresholdSettings(
            thCo = prefs.getFloat("th_co", 50.0f).toDouble(),
            thCo2 = prefs.getFloat("th_co2", 1000.0f).toDouble(),
            thPm10 = prefs.getFloat("th_pm10", 100.0f).toDouble(),
            thPm25 = prefs.getFloat("th_pm25", 50.0f).toDouble(),
            thHumedad = prefs.getFloat("th_hum", 80.0f).toDouble(),
            thTemperatura = prefs.getFloat("th_temp", 28.0f).toDouble(),
            thMq2 = prefs.getFloat("th_mq2", 400.0f).toDouble(),
            activoCo = prefs.getBoolean("th_act_co", true),
            activoCo2 = prefs.getBoolean("th_act_co2", true),
            activoPm10 = prefs.getBoolean("th_act_pm10", true),
            activoPm25 = prefs.getBoolean("th_act_pm25", true),
            activoHumedad = prefs.getBoolean("th_act_hum", true),
            activoTemperatura = prefs.getBoolean("th_act_temp", true),
            activoMq2 = prefs.getBoolean("th_act_mq2", true),
            ultimoCambioFecha = prefs.getString("th_last_fecha", "Valores de referencia de fábrica") ?: "Valores de referencia de fábrica",
            ultimoCambioUsuario = prefs.getString("th_last_usuario", "Sistema") ?: "Sistema"
        )
    )
    val alertThresholds: StateFlow<AlertThresholdSettings> = _alertThresholds.asStateFlow()

    fun updateAlertThresholds(newSettings: AlertThresholdSettings, userNote: String = "") {
        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val authorName = userProfile.value.nombre.ifBlank { "Personal Autorizado" }
        val updated = newSettings.copy(
            ultimoCambioFecha = dateStr,
            ultimoCambioUsuario = authorName
        )
        _alertThresholds.value = updated

        // Persist locally
        prefs.edit()
            .putFloat("th_co", updated.thCo.toFloat())
            .putFloat("th_co2", updated.thCo2.toFloat())
            .putFloat("th_pm10", updated.thPm10.toFloat())
            .putFloat("th_pm25", updated.thPm25.toFloat())
            .putFloat("th_hum", updated.thHumedad.toFloat())
            .putFloat("th_temp", updated.thTemperatura.toFloat())
            .putFloat("th_mq2", updated.thMq2.toFloat())
            .putBoolean("th_act_co", updated.activoCo)
            .putBoolean("th_act_co2", updated.activoCo2)
            .putBoolean("th_act_pm10", updated.activoPm10)
            .putBoolean("th_act_pm25", updated.activoPm25)
            .putBoolean("th_act_hum", updated.activoHumedad)
            .putBoolean("th_act_temp", updated.activoTemperatura)
            .putBoolean("th_act_mq2", updated.activoMq2)
            .putString("th_last_fecha", dateStr)
            .putString("th_last_usuario", authorName)
            .apply()

        // Sync with Firebase Repository
        firebaseRepo.updateAlertThresholds(updated)
        _userMessage.value = "Umbrales actualizados exitosamente en Firebase"
    }

    fun restoreDefaultAlertThresholds() {
        val defaults = AlertThresholdSettings()
        updateAlertThresholds(defaults, "Restauración a valores de fábrica")
        _userMessage.value = "Valores de referencia de fábrica restaurados"
    }

    init {
        // Initialize thresholds in repo
        firebaseRepo.updateAlertThresholds(_alertThresholds.value)
        // Configure AI repository base URL
        aiRepo.setBaseUrl(_aiBaseUrl.value)

        // Initialize gateway IP and port from device preferences if configured
        val savedGwIp = prefs.getString("saved_gateway_ip", "192.168.1.100") ?: "192.168.1.100"
        val savedGwPort = prefs.getInt("saved_gateway_port", 8080)
        firebaseRepo.setInitialGatewayIpAndPort(savedGwIp, savedGwPort)

        // Start listening to Firebase Realtime Database
        firebaseRepo.startLiveSensorListener()
        firebaseRepo.startAlertDeviceListener()
        firebaseRepo.startDevicesListListener()
        firebaseRepo.loadHistoryForDevice(_selectedDeviceForChart.value)

        // Observe auth to initialize user profile
        viewModelScope.launch {
            authRepo.currentUser.collectLatest { user ->
                if (user != null) {
                    firebaseRepo.startUserProfileListener(user.uid, user.email ?: "")
                }
            }
        }

        // Continuous Emergency Alarm & Siren Manager:
        // Runs repeating vibration and siren audio loop continuously while
        // an alarm is active OR an emergency simulation (simulacro) is in progress.
        viewModelScope.launch {
            combine(
                isAlarmaActive,
                isEmergencySimulationActive
            ) { alarma, simulacro ->
                alarma || simulacro
            }.collectLatest { shouldSound ->
                val context = getApplication<Application>().applicationContext
                if (shouldSound) {
                    EmergencyAlarmManager.start(context)
                } else {
                    EmergencyAlarmManager.stop(context)
                }
            }
        }

        // Auto-login if user selected 'Recordar en este dispositivo' and 'Auto-login'
        val savedEm = prefs.getString("saved_email", "") ?: ""
        val savedPw = prefs.getString("saved_password", "") ?: ""
        val autoLog = prefs.getBoolean("auto_login", true)
        val remDev = prefs.getBoolean("remember_device", true)

        if (remDev && autoLog && savedEm.isNotBlank() && savedPw.length >= 6) {
            viewModelScope.launch {
                login(email = savedEm, pass = savedPw, remember = true, autoLog = true) {
                    Log.d(TAG, "Inicio de sesión automático completado con éxito para: $savedEm")
                }
            }
        }
    }

    fun navigateTo(destination: ScreenDestination) {
        // Role check
        if (destination.adminOnly && !userProfile.value.canAccessAdminPanel) {
            _userMessage.value = "Acceso restringido: requiere rol Administrador o SuperAdmin."
            return
        }
        if (destination == ScreenDestination.ADMIN) {
            firebaseRepo.loadAllUsersForAdmin()
        }
        _currentScreen.value = destination
    }

    fun login(
        email: String,
        pass: String,
        remember: Boolean = true,
        autoLog: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = authRepo.login(email, pass)
            res.onSuccess { user ->
                val profile = firebaseRepo.bindUserProfileByEmail(email, user?.uid ?: "")
                _isLoggedIn.value = true
                authRepo.setAuthenticatedState(user, profile)

                if (remember) {
                    prefs.edit()
                        .putBoolean("remember_device", true)
                        .putBoolean("auto_login", autoLog)
                        .putString("saved_email", email.trim())
                        .putString("saved_password", pass)
                        .apply()
                    _rememberDevice.value = true
                    _autoLogin.value = autoLog
                    _savedEmail.value = email.trim()
                    _savedPassword.value = pass
                } else {
                    prefs.edit()
                        .putBoolean("remember_device", false)
                        .putBoolean("auto_login", false)
                        .remove("saved_email")
                        .remove("saved_password")
                        .apply()
                    _rememberDevice.value = false
                    _autoLogin.value = false
                    _savedEmail.value = ""
                    _savedPassword.value = ""
                }

                _userMessage.value = "Sesión iniciada: ${profile.nombre} (${profile.rolDisplay})"
                onSuccess()
            }.onFailure {
                _userMessage.value = it.message ?: "Error al iniciar sesión."
            }
        }
    }

    fun register(
        email: String,
        pass: String,
        remember: Boolean = true,
        autoLog: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = authRepo.register(email, pass)
            res.onSuccess { user ->
                val profile = firebaseRepo.bindUserProfileByEmail(email, user?.uid ?: "")
                _isLoggedIn.value = true
                authRepo.setAuthenticatedState(user, profile)
                firebaseRepo.updateUserProfileData(
                    uid = profile.uid,
                    nombre = profile.nombre,
                    empresa = profile.empresa,
                    tipoMina = profile.tipoMina,
                    latitud = profile.latitud,
                    longitud = profile.longitud
                )

                if (remember) {
                    prefs.edit()
                        .putBoolean("remember_device", true)
                        .putBoolean("auto_login", autoLog)
                        .putString("saved_email", email.trim())
                        .putString("saved_password", pass)
                        .apply()
                    _rememberDevice.value = true
                    _autoLogin.value = autoLog
                    _savedEmail.value = email.trim()
                    _savedPassword.value = pass
                }

                _userMessage.value = "Usuario registrado con éxito: ${profile.nombre} (${profile.rolDisplay})"
                onSuccess()
            }.onFailure {
                _userMessage.value = it.message ?: "Error al registrarse."
            }
        }
    }

    fun forgetSavedDevice() {
        prefs.edit()
            .putBoolean("remember_device", false)
            .putBoolean("auto_login", false)
            .remove("saved_email")
            .remove("saved_password")
            .apply()
        _rememberDevice.value = false
        _autoLogin.value = false
        _savedEmail.value = ""
        _savedPassword.value = ""
        _userMessage.value = "Credenciales eliminadas de este dispositivo."
    }

    fun setAutoLoginPreference(enabled: Boolean) {
        prefs.edit().putBoolean("auto_login", enabled).apply()
        _autoLogin.value = enabled
        _userMessage.value = if (enabled) "Inicio automático activado en este dispositivo" else "Inicio automático desactivado"
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val res = authRepo.resetPassword(email)
            res.onSuccess {
                _userMessage.value = "Enlace de recuperación enviado a $email"
            }.onFailure {
                _userMessage.value = it.message ?: "Error al enviar correo de recuperación."
            }
        }
    }

    fun logout() {
        authRepo.logout()
        _isLoggedIn.value = false
        _currentScreen.value = ScreenDestination.DASHBOARD
        _userMessage.value = "Sesión cerrada correctamente."
    }

    /**
     * Demo / Fast access for instant reviewer testing with pre-set roles from minefase-fc5f5
     */
    fun enterAsRole(role: String) {
        val isSuper = role.equals("superAdmin", ignoreCase = true)
        val isAdmin = isSuper || role.equals("admin", ignoreCase = true)

        val profile = when (role) {
            "superAdmin" -> UserProfile(
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
            "admin" -> UserProfile(
                uid = "S427GEJa97Nl6B1Gyo3vnQtpSqV2",
                email = "white.wolf260501@gmail.com",
                isSuperUser = false,
                isAdmin = true,
                tipoUsuario = "admin",
                nombre = "PEPE LOTA",
                empresa = "Minera Los Andes",
                tipoMina = "Mina Subterránea",
                latitud = -11.5936,
                longitud = -76.2411
            )
            else -> UserProfile(
                uid = "avZLQk44v4evq0VsxO8QJDX8yRP2",
                email = "geencuve@gmail.com",
                isSuperUser = false,
                isAdmin = false,
                tipoUsuario = "usuario",
                nombre = "Gerardo Cuellar Velasquez",
                empresa = "Mina San José",
                tipoMina = "Mina Subterránea",
                latitud = -27.16,
                longitud = -70.49
            )
        }
        authRepo.setAuthenticatedState(null, profile)
        _isLoggedIn.value = true
        firebaseRepo.startUserProfileListener(profile.uid, profile.email)
        _userMessage.value = "Conectado como: ${profile.nombre} (${profile.rolDisplay})"
    }

    fun toggleDeviceSimulation() {
        firebaseRepo.toggleDeviceSimulation()
        val isNowConnected = firebaseRepo.isDeviceConnected.value
        _userMessage.value = if (isNowConnected) "Sensor ESP32: Conectado y transmitiendo" else "Sensor ESP32: Desconectado (Sin señal)"
    }

    fun toggleGatewaySimulation() {
        firebaseRepo.toggleGatewaySimulation()
        val isNowConnected = firebaseRepo.isGatewayConnected.value
        _userMessage.value = if (isNowConnected) "Gateway Bocamina: Conectado a la red de mina" else "Gateway Bocamina: Desconectado (Pérdida de enlace)"
    }

    fun pingGatewayToFirebase() {
        firebaseRepo.pingGatewayToFirebase()
        _userMessage.value = "📡 Heartbeat emitido al nodo /dispositivos/gateway en Firebase"
    }

    fun pingGateway() {
        pingGatewayToFirebase()
    }

    fun getCriticalSensorsDataForDevice(deviceId: String): MiningCriticalSensorsData {
        val live = liveSensorData.value
        val alert = alertDeviceData.value
        val dev = devicesList.value.find { it.deviceId == deviceId }
        val gw = gatewayData.value

        return when {
            deviceId == "gateway" -> MiningCriticalSensorsData(
                deviceId = "gateway",
                deviceName = "Gateway Bocamina LoRa/WiFi",
                sector = "Bocamina Exterior - Caseta Redes",
                status = if (isGatewayConnected.value) "Online" else "Offline",
                bateria = 100,
                enlace = gw.enlace,
                temperatura = 27.2,
                humedad = 45.0,
                co = 8.0,
                co2 = 480.0,
                pm25 = 18.0,
                metanoCH4 = 0.0,
                ventilacion = 3.2,
                ruido = 58.0,
                oxigenoO2 = 21.0
            )
            deviceId == "NODO_U-01" -> MiningCriticalSensorsData(
                deviceId = "NODO_U-01",
                deviceName = "Nodo Minero Subterráneo (NODO_U-01)",
                sector = "Frente de Avance - Rampa 3",
                status = dev?.status ?: "Online",
                bateria = 82,
                enlace = "LoRaWAN 915MHz • RSSI -72 dBm",
                temperatura = dev?.temperatura ?: 19.8,
                humedad = dev?.humedad ?: 49.0,
                co = dev?.co ?: 91.2,
                co2 = dev?.co2 ?: 912.0,
                pm25 = dev?.pm25 ?: 180.0,
                metanoCH4 = 1.2,
                ventilacion = 0.8,
                ruido = 84.0,
                oxigenoO2 = 20.4
            )
            deviceId == "device_38A839E81F84" -> MiningCriticalSensorsData(
                deviceId = "device_38A839E81F84",
                deviceName = "Detector Chuquicamata (Fase 1)",
                sector = "Tajo Abierto - Banco 4 Sur",
                status = dev?.status ?: "Online",
                bateria = 88,
                enlace = "WiFi Faena 5.8GHz • RSSI -58 dBm",
                temperatura = dev?.temperatura ?: live.temperatura,
                humedad = dev?.humedad ?: live.humedad,
                co = dev?.co ?: alert.co,
                co2 = dev?.co2 ?: alert.co2,
                pm25 = dev?.pm25 ?: alert.pm25,
                metanoCH4 = 0.8,
                ventilacion = 1.6,
                ruido = 76.0,
                oxigenoO2 = 20.8
            )
            deviceId == "device_A4CB2F124B00" -> MiningCriticalSensorsData(
                deviceId = "device_A4CB2F124B00",
                deviceName = "Estación Refugio San José",
                sector = "Subterráneo - Refugio de Emergencia",
                status = dev?.status ?: "Online",
                bateria = 100,
                enlace = "Ethernet Minera • Respaldo UPS",
                temperatura = dev?.temperatura ?: 22.0,
                humedad = dev?.humedad ?: 64.0,
                co = dev?.co ?: 12.0,
                co2 = dev?.co2 ?: 540.0,
                pm25 = dev?.pm25 ?: 28.0,
                metanoCH4 = 0.1,
                ventilacion = 2.4,
                ruido = 62.0,
                oxigenoO2 = 21.0
            )
            else -> MiningCriticalSensorsData(
                deviceId = deviceId.ifBlank { "dispositivo1" },
                deviceName = dev?.name ?: "ESP32 Sensor Primario (Galería 1)",
                sector = "Galería 1 - Socavón Principal",
                status = if (isDeviceConnected.value) "Online" else "Offline",
                bateria = 96,
                enlace = "LoRaWAN 915MHz • RSSI -64 dBm",
                temperatura = live.temperatura,
                humedad = live.humedad,
                co = if (alert.co > 0) alert.co else live.mq135 / 20.0,
                co2 = alert.co2,
                pm25 = live.pm25,
                metanoCH4 = 0.3,
                ventilacion = 2.1,
                ruido = 68.0,
                oxigenoO2 = 20.9
            )
        }
    }

    fun updateGatewayIp(newIp: String, newPort: Int = 8080): Boolean {
        val profile = userProfile.value
        if (!profile.canAccessAdminPanel) {
            _userMessage.value = "⛔ Permiso denegado: Se requiere rol de Administrador o Super Admin para editar la IP del Gateway."
            return false
        }

        val res = firebaseRepo.updateGatewayIp(newIp, newPort, profile.email)
        return res.fold(
            onSuccess = {
                prefs.edit()
                    .putString("saved_gateway_ip", newIp.trim())
                    .putInt("saved_gateway_port", newPort)
                    .apply()
                _userMessage.value = "✅ IP del Gateway actualizada a ${newIp.trim()}:$newPort por ${profile.nombre}"
                true
            },
            onFailure = {
                _userMessage.value = "❌ ${it.message ?: "Error al actualizar IP"}"
                false
            }
        )
    }

    fun pingDeviceToFirebase() {
        firebaseRepo.pingDeviceToFirebase()
        _userMessage.value = "📡 Heartbeat emitido al sensor ESP32 en Firebase"
    }

    fun setSelectedDevice(deviceId: String) {
        firebaseRepo.setSelectedDevice(deviceId)
    }

    fun toggleAlarma(deviceId: String? = null) {
        viewModelScope.launch {
            val current = isAlarmaActive.value
            val nextState = !current
            val dev = deviceId ?: selectedDeviceId.value
            firebaseRepo.setAlarmaState(nextState, dev)
            val context = getApplication<Application>().applicationContext
            if (nextState) {
                EmergencyAlarmManager.start(context)
                _userMessage.value = "🚨 Alarma sonora y vibración de emergencia ACTIVADAS ($dev)"
            } else {
                EmergencyAlarmManager.stop(context)
                _userMessage.value = "🔕 Alarma sonora APAGADA / Silenciada ($dev)"
            }
        }
    }

    fun toggleLuz(deviceId: String? = null) {
        viewModelScope.launch {
            val current = isLuzActive.value
            val nextState = !current
            val dev = deviceId ?: selectedDeviceId.value
            firebaseRepo.setLuzState(nextState, dev)
            if (nextState) {
                _userMessage.value = "💡 Luz de emergencia ENCENDIDA ($dev)"
            } else {
                _userMessage.value = "🌑 Luz de emergencia APAGADA ($dev)"
            }
        }
    }

    fun refreshData() {
        firebaseRepo.reconnectDevice()
        _userMessage.value = "Sincronizando estado con Firebase RTDB..."
    }

    fun selectChartDevice(deviceId: String) {
        _selectedDeviceForChart.value = deviceId
        firebaseRepo.loadHistoryForDevice(deviceId)
    }

    fun selectChartMetric(metric: String) {
        _selectedMetricForChart.value = metric
    }

    fun setTestingRole(newRole: String) {
        firebaseRepo.setTestingRole(newRole)
        val roleLabel = when (newRole.lowercase()) {
            "superadmin" -> "Superadmin (Corona 👑 marco púrpura)"
            "admin" -> "Admin (Escudo 🛡️ marco cian)"
            else -> "Operador (Casco 👷 marco ámbar)"
        }
        _userMessage.value = "Rol activo cambiado a: $roleLabel"
    }

    fun updateUserRole(uid: String, newRole: String) {
        viewModelScope.launch {
            val res = firebaseRepo.updateUserRole(uid, newRole)
            res.onSuccess {
                _userMessage.value = "Rol de usuario actualizado a $newRole en Firestore."
            }.onFailure {
                _userMessage.value = "Error al actualizar rol: ${it.message}"
            }
        }
    }

    fun updateUserProfile(
        nombre: String,
        empresa: String,
        tipoMina: String,
        latitud: Double,
        longitud: Double
    ) {
        viewModelScope.launch {
            val uid = userProfile.value.uid
            val res = firebaseRepo.updateUserProfileData(uid, nombre, empresa, tipoMina, latitud, longitud)
            res.onSuccess {
                _userMessage.value = "Perfil y geolocalización actualizados."
            }.onFailure {
                _userMessage.value = "Error al guardar perfil: ${it.message}"
            }
        }
    }

    fun startEmergencySimulation(
        scenarioName: String,
        triggerActuators: Boolean = true,
        userDescription: String = ""
    ) {
        val profile = userProfile.value
        if (!profile.canAccessAdminPanel) {
            _userMessage.value = "⛔ Acceso denegado: Solo Administradores o Super Admins pueden iniciar simulacros de evacuación."
            return
        }

        viewModelScope.launch {
            firebaseRepo.startEmergencySimulation(scenarioName, triggerActuators, userDescription)
            val context = getApplication<Application>().applicationContext
            if (triggerActuators) {
                EmergencyAlarmManager.start(context)
            }
            val details = if (userDescription.isNotBlank()) " ($userDescription)" else ""
            _userMessage.value = "🚨 SIMULACRO INICIADO: $scenarioName$details (Autorizado por ${profile.nombre})"
        }
    }

    fun reportIncident(
        tipo: String,
        ubicacion: String,
        gravedad: String,
        descripcion: String,
        activarAlarma: Boolean
    ) {
        val profile = userProfile.value
        val incident = ReportedIncident(
            id = "inc_${System.currentTimeMillis()}",
            tipo = tipo,
            ubicacion = ubicacion,
            gravedad = gravedad,
            descripcion = descripcion,
            autorNombre = profile.nombre.ifBlank { "Personal de Turno" },
            autorRol = profile.rolDisplay,
            timestamp = System.currentTimeMillis(),
            alarmaActivada = activarAlarma,
            estado = "Activa"
        )

        viewModelScope.launch {
            val res = firebaseRepo.reportIncident(incident)
            res.onSuccess {
                if (activarAlarma) {
                    val context = getApplication<Application>().applicationContext
                    EmergencyAlarmManager.start(context)
                }
                _userMessage.value = "📢 Incidencia registrada: $tipo en $ubicacion."
            }.onFailure {
                _userMessage.value = "Error al registrar la incidencia: ${it.message}"
            }
        }
    }

    fun updateIncidentDescription(
        incidentId: String,
        newDescription: String,
        newEstado: String? = null
    ) {
        viewModelScope.launch {
            val res = firebaseRepo.updateIncidentDescription(incidentId, newDescription, newEstado)
            res.onSuccess {
                _userMessage.value = "📝 Descripción de la alerta actualizada correctamente."
            }.onFailure {
                _userMessage.value = "Error al actualizar la descripción: ${it.message}"
            }
        }
    }

    fun describeSafetyAlert(
        alert: SafetyAlert,
        descripcion: String,
        ubicacion: String = "Sector Subterráneo / General",
        activarAlarma: Boolean = false
    ) {
        val tipo = "${alert.title} (${alert.metricName})"
        val gravedad = if (alert.severity == AlertSeverity.DANGER) "Emergencia" else "Media"
        reportIncident(
            tipo = tipo,
            ubicacion = ubicacion,
            gravedad = gravedad,
            descripcion = descripcion,
            activarAlarma = activarAlarma
        )
    }

    fun stopEmergencySimulation() {
        val profile = userProfile.value
        if (!profile.canAccessAdminPanel) {
            _userMessage.value = "⛔ Acceso denegado: Solo Administradores o Super Admins pueden finalizar simulacros."
            return
        }

        viewModelScope.launch {
            firebaseRepo.stopEmergencySimulation()
            val context = getApplication<Application>().applicationContext
            EmergencyAlarmManager.stop(context)
            _userMessage.value = "✅ Simulacro finalizado. Condiciones seguras restablecidas."
        }
    }

    fun simulateAlertTest(triggerDanger: Boolean) {
        val profile = userProfile.value
        if (!profile.canAccessAdminPanel) {
            _userMessage.value = "⛔ Permiso restringido: La simulación de riesgos está reservada para Admins y Super Admins."
            return
        }

        if (triggerDanger) {
            startEmergencySimulation("Simulacro General de Peligro", triggerActuators = true)
        } else {
            stopEmergencySimulation()
        }
    }

    fun setAiSelectedDeviceId(devId: String) {
        _aiSelectedDeviceId.value = devId
        if (_aiAutoAnalyze.value) {
            runAiAnalysis(devId, forceDemoIfOffline = true)
        }
    }

    fun setAiAutoAnalyze(enabled: Boolean) {
        _aiAutoAnalyze.value = enabled
        if (enabled) {
            runAiAnalysis(forceDemoIfOffline = true)
        }
    }

    fun updateAiBaseUrl(url: String) {
        val clean = url.trim().removeSuffix("/")
        _aiBaseUrl.value = clean
        prefs.edit().putString("saved_ai_url", clean).apply()
        aiRepo.setBaseUrl(clean)
        checkAiHealth()
    }

    fun updateAiApiKey(key: String) {
        val clean = key.trim()
        _aiApiKey.value = clean
        prefs.edit().putString("saved_ai_key", clean).apply()
    }

    fun checkAiHealth() {
        viewModelScope.launch {
            val res = aiRepo.checkHealth()
            res.onSuccess {
                _isAiOnline.value = true
                _aiAnalysisError.value = null
            }.onFailure {
                _isAiOnline.value = false
            }
        }
    }

    fun runAiAnalysis(deviceId: String? = null, forceDemoIfOffline: Boolean = true) {
        val targetDev = deviceId ?: _aiSelectedDeviceId.value
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            _aiAnalysisError.value = null

            val healthRes = aiRepo.checkHealth()
            _isAiOnline.value = healthRes.isSuccess

            val currentHistory = historyList.value
            val observations = if (currentHistory.size >= 5) {
                aiRepo.convertHistoricalPointsToObservations(currentHistory)
            } else {
                generateStandardAiTestSeries()
            }

            if (_isAiOnline.value) {
                val analyzeRes = aiRepo.analyzeSeries(targetDev, observations, _aiApiKey.value)
                analyzeRes.onSuccess { response ->
                    _aiAnalysisResult.value = response
                    _userMessage.value = "Análisis de IA completado (${response.observations_count} observaciones)"
                }.onFailure { err ->
                    _aiAnalysisError.value = err.message
                    if (forceDemoIfOffline || _aiAnalysisResult.value == null) {
                        _aiAnalysisResult.value = calculateLocalAiStatistics(targetDev, observations)
                    }
                }
            } else {
                _aiAnalysisError.value = "No se pudo conectar con el backend de IA: Failed to connect to ${aiRepo.getBaseUrl()}. Asegúrese de iniciar uvicorn en Windows."
                if (forceDemoIfOffline || _aiAnalysisResult.value == null) {
                    _aiAnalysisResult.value = calculateLocalAiStatistics(targetDev, observations)
                }
            }
            _isAiAnalyzing.value = false
        }
    }

    private fun generateStandardAiTestSeries(): List<AiObservation> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val now = System.currentTimeMillis()
        val list = mutableListOf<AiObservation>()
        val baseTemps = listOf(22.1, 22.3, 22.5, 22.8, 23.0, 23.2, 23.5, 23.9, 24.1, 24.4, 24.6, 25.0)
        val baseHums = listOf(58.0, 58.2, 57.9, 58.4, 59.0, 58.8, 59.5, 60.1, 60.0, 60.5, 61.0, 61.2)
        val baseMq = listOf(410.0, 415.0, 412.0, 420.0, 425.0, 430.0, 435.0, 442.0, 450.0, 460.0, 475.0, 510.0)
        val basePm = listOf(14.0, 15.0, 14.5, 16.0, 15.5, 17.0, 18.0, 19.5, 21.0, 22.0, 24.0, 26.5)

        for (i in 0 until 12) {
            val t = now - (11 - i) * 600_000L
            list.add(
                AiObservation(
                    timestamp = sdf.format(java.util.Date(t)),
                    pm25_raw = basePm[i],
                    mq2_raw = baseMq[i],
                    temperature_c = baseTemps[i],
                    humidity_pct = baseHums[i],
                    unit = "raw"
                )
            )
        }
        return list
    }

    private fun calculateLocalAiStatistics(deviceId: String, observations: List<AiObservation>): AiAnalyzeResponse {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val sorted = observations.sortedBy { it.timestamp }
        val trends = mutableListOf<AiMetricTrend>()
        val anomalies = mutableListOf<AiAnomaly>()

        val metrics = listOf(
            Triple("mq2_raw", "raw ADC / unit from payload") { o: AiObservation -> o.mq2_raw },
            Triple("pm25_raw", "raw ADC / unit from payload") { o: AiObservation -> o.pm25_raw },
            Triple("temperature_c", "°C") { o: AiObservation -> o.temperature_c },
            Triple("humidity_pct", "%") { o: AiObservation -> o.humidity_pct }
        )

        for ((metricKey, unitLabel, extractor) in metrics) {
            val validPoints = sorted.mapNotNull { obs ->
                val v = extractor(obs)
                if (v != null && !v.isNaN() && !v.isInfinite()) {
                    Pair(obs.timestamp, v)
                } else null
            }

            if (validPoints.size < 2) {
                trends.add(
                    AiMetricTrend(
                        metric = metricKey,
                        first_value = validPoints.firstOrNull()?.second ?: 0.0,
                        last_value = validPoints.lastOrNull()?.second ?: 0.0,
                        slope_per_hour = null,
                        direction = "insufficient_data",
                        unit = unitLabel
                    )
                )
                continue
            }

            val t0 = try { sdf.parse(validPoints.first().first)?.time ?: 0L } catch (e: Exception) { 0L }
            val xs = validPoints.map { (ts, _) ->
                val t = try { sdf.parse(ts)?.time ?: t0 } catch (e: Exception) { t0 }
                (t - t0).toDouble() / 3600000.0
            }
            val ys = validPoints.map { it.second }

            val meanX = xs.average()
            val meanY = ys.average()

            var num = 0.0
            var den = 0.0
            for (i in xs.indices) {
                val dx = xs[i] - meanX
                val dy = ys[i] - meanY
                num += dx * dy
                den += dx * dx
            }
            val slope = if (den != 0.0) num / den else 0.0
            val tolerance = maxOf(Math.abs(meanY) * 0.01, 1e-9)
            val direction = if (Math.abs(ys.last() - ys.first()) <= tolerance) {
                "stable"
            } else if (slope > 0) {
                "increasing"
            } else {
                "decreasing"
            }

            trends.add(
                AiMetricTrend(
                    metric = metricKey,
                    first_value = Math.round(ys.first() * 1000.0) / 1000.0,
                    last_value = Math.round(ys.last() * 1000.0) / 1000.0,
                    slope_per_hour = Math.round(slope * 1000000.0) / 1000000.0,
                    direction = direction,
                    unit = unitLabel
                )
            )

            if (validPoints.size >= 5) {
                val sortedYs = ys.sorted()
                val center = if (sortedYs.size % 2 == 1) {
                    sortedYs[sortedYs.size / 2]
                } else {
                    (sortedYs[sortedYs.size / 2 - 1] + sortedYs[sortedYs.size / 2]) / 2.0
                }

                val deviations = ys.map { Math.abs(it - center) }.sorted()
                val mad = if (deviations.size % 2 == 1) {
                    deviations[deviations.size / 2]
                } else {
                    (deviations[deviations.size / 2 - 1] + deviations[deviations.size / 2]) / 2.0
                }

                if (mad > 0.0) {
                    for ((ts, v) in validPoints) {
                        val robustZ = 0.6745 * (v - center) / mad
                        if (Math.abs(robustZ) >= 3.5) {
                            anomalies.add(
                                AiAnomaly(
                                    timestamp = ts,
                                    metric = metricKey,
                                    value = v,
                                    median = center,
                                    robust_z_score = Math.round(robustZ * 1000.0) / 1000.0,
                                    unit = "raw",
                                    explanation = "Valor atípico estadístico respecto de esta ventana; requiere revisión humana."
                                )
                            )
                        }
                    }
                }
            }
        }

        return AiAnalyzeResponse(
            device_id = deviceId,
            generated_at = sdf.format(java.util.Date()),
            window_start = sorted.firstOrNull()?.timestamp ?: "",
            window_end = sorted.lastOrNull()?.timestamp ?: "",
            observations_count = sorted.size,
            analysis_method = "robust_median_mad_and_linear_trend_v1",
            trends = trends,
            anomalies = anomalies.sortedByDescending { Math.abs(it.robust_z_score) },
            limitations = listOf(
                "La detección estadística no está validada como alarma de seguridad ni como predicción de incidentes.",
                "No se aplican umbrales normativos: faltan calibración confirmada y configuración aprobada por variable.",
                "Las anomalías dependen de la ventana y pueden no detectarse si hay pocos datos o poca variación.",
                "El análisis no sustituye alarmas físicas, procedimientos de evacuación ni evaluación de prevención de riesgos.",
                "Hay observaciones marcadas como raw; no interpretar los valores como ppm o µg/m³."
            )
        )
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            val context = getApplication<Application>().applicationContext
            EmergencyAlarmManager.stop(context)
        } catch (e: Exception) {
            // Ignore on cleanup
        }
    }
}
