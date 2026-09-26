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
import com.example.model.DeviceSummary
import com.example.model.HistoricalPoint
import com.example.model.LiveSensorData
import com.example.model.UserProfile
import com.example.ui.navigation.ScreenDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    // Live Data from Realtime Database
    val isFirebaseConnected: StateFlow<Boolean> = firebaseRepo.isFirebaseConnected
    val connectionStatusMessage: StateFlow<String> = firebaseRepo.connectionStatusMessage
    val liveSensorData: StateFlow<LiveSensorData> = firebaseRepo.liveSensorData
    val alertDeviceData: StateFlow<AlertDeviceReading> = firebaseRepo.alertDeviceData
    val devicesList: StateFlow<List<DeviceSummary>> = firebaseRepo.devicesList
    val userProfile: StateFlow<UserProfile> = firebaseRepo.userProfile
    val historyList: StateFlow<List<HistoricalPoint>> = firebaseRepo.historyList
    val allUsersList: StateFlow<List<UserProfile>> = firebaseRepo.allUsersList

    // Chart selections
    private val _selectedDeviceForChart = MutableStateFlow("device_38A839E81F84")
    val selectedDeviceForChart: StateFlow<String> = _selectedDeviceForChart.asStateFlow()

    private val _selectedMetricForChart = MutableStateFlow("temperatura") // "temperatura", "humedad", "gases", "pm25"
    val selectedMetricForChart: StateFlow<String> = _selectedMetricForChart.asStateFlow()

    // Operation status feedback message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
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

        // Check alerts to trigger haptic feedback if dangerous levels are reached
        viewModelScope.launch {
            alertDeviceData.collectLatest { alertReading ->
                if (!alertReading.isAllSafe) {
                    triggerAlertVibration()
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

    fun login(email: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = authRepo.login(email, pass)
            res.onSuccess {
                _userMessage.value = "Sesión iniciada correctamente."
                onSuccess()
            }.onFailure {
                _userMessage.value = it.message ?: "Error al iniciar sesión."
            }
        }
    }

    fun register(email: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = authRepo.register(email, pass)
            res.onSuccess { user ->
                if (user != null) {
                    // Initialize base user profile
                    firebaseRepo.updateUserProfileData(
                        uid = user.uid,
                        nombre = email.substringBefore("@"),
                        empresa = "Minera Los Andes",
                        tipoMina = "Mina Subterránea",
                        latitud = -11.5936,
                        longitud = -76.2411
                    )
                }
                _userMessage.value = "Usuario registrado con éxito."
                onSuccess()
            }.onFailure {
                _userMessage.value = it.message ?: "Error al registrarse."
            }
        }
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
        _currentScreen.value = ScreenDestination.DASHBOARD
        _userMessage.value = "Sesión cerrada."
    }

    /**
     * Demo / Fast access for instant reviewer testing with pre-set roles
     */
    fun enterAsRole(role: String) {
        val dummyUid = "demo_${role.lowercase()}"
        val email = "$role@minesafe.pe".lowercase()
        val isSuper = role.equals("superAdmin", ignoreCase = true)
        val isAdmin = isSuper || role.equals("admin", ignoreCase = true)

        val profile = UserProfile(
            uid = dummyUid,
            email = email,
            isSuperUser = isSuper,
            isAdmin = isAdmin,
            tipoUsuario = role,
            nombre = when (role) {
                "superAdmin" -> "Ing. Manuel Crisóstomo (SuperAdmin)"
                "admin" -> "Ing. Roberto Alarcón (Admin Mina)"
                else -> "Carlos Mendoza (Operador)"
            },
            empresa = "Minera Los Andes S.A.C.",
            tipoMina = "Mina Subterránea Nivel -400",
            latitud = -11.5936,
            longitud = -76.2411
        )
        authRepo.setAuthenticatedState(null, profile)
        firebaseRepo.startUserProfileListener(dummyUid, email)
        _userMessage.value = "Modo: ${profile.rolDisplay}"
    }

    fun selectChartDevice(deviceId: String) {
        _selectedDeviceForChart.value = deviceId
        firebaseRepo.loadHistoryForDevice(deviceId)
    }

    fun selectChartMetric(metric: String) {
        _selectedMetricForChart.value = metric
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

    fun simulateAlertTest(triggerDanger: Boolean) {
        firebaseRepo.setSimulatedAlertState(triggerDanger)
        _userMessage.value = if (triggerDanger) {
            "Simulación de gases peligrosos activada"
        } else {
            "Simulación de condiciones normales restablecida"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun triggerAlertVibration() {
        try {
            val context = getApplication<Application>().applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 150), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator exception: ${e.message}")
        }
    }
}
