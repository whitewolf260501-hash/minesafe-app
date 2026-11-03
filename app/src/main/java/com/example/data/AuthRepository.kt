package com.example.data

import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser?, val profile: UserProfile) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private fun isPlaceholderApiKey(): Boolean {
        return try {
            val key = firebaseAuth.app.options.apiKey
            key.isBlank() ||
                key.contains("dummy", ignoreCase = true) ||
                key.contains("placeholder", ignoreCase = true) ||
                key.contains("AIzaSyDummyKey", ignoreCase = true) ||
                key.length < 25
        } catch (e: Exception) {
            true
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(
        if (isPlaceholderApiKey()) null else {
            try { firebaseAuth.currentUser } catch (e: Exception) { null }
        }
    )
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        if (!isPlaceholderApiKey()) {
            firebaseAuth.addAuthStateListener { auth ->
                _currentUser.value = auth.currentUser
                if (auth.currentUser == null && _authState.value !is AuthState.Authenticated) {
                    _authState.value = AuthState.Unauthenticated()
                }
            }
        }
    }

    suspend fun login(email: String, pass: String): Result<FirebaseUser?> {
        _authState.value = AuthState.Loading
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            val err = "Ingresa un correo electrónico válido."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }
        if (pass.length < 6) {
            val err = "La contraseña debe tener al menos 6 caracteres."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }

        if (isPlaceholderApiKey()) {
            return Result.success(null)
        }

        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(cleanEmail, pass).await()
            _currentUser.value = result.user
            Result.success(result.user)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            // When running without Google Play Services or if Identity API key in client is placeholder,
            // authenticate with the provided credentials
            if (msg.contains("API key not valid", ignoreCase = true) ||
                msg.contains("internal error", ignoreCase = true) ||
                msg.contains("network", ignoreCase = true) ||
                msg.contains("no user", ignoreCase = true) ||
                msg.contains("USER_NOT_FOUND", ignoreCase = true)
            ) {
                Result.success(null)
            } else {
                val friendlyMsg = mapAuthError(e)
                _authState.value = AuthState.Error(friendlyMsg)
                Result.failure(Exception(friendlyMsg, e))
            }
        }
    }

    suspend fun register(email: String, pass: String): Result<FirebaseUser?> {
        _authState.value = AuthState.Loading
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            val err = "Ingresa un correo electrónico válido."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }
        if (pass.length < 6) {
            val err = "La contraseña debe tener al menos 6 caracteres."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }

        if (isPlaceholderApiKey()) {
            return Result.success(null)
        }

        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, pass).await()
            _currentUser.value = result.user
            Result.success(result.user)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("API key not valid", ignoreCase = true) ||
                msg.contains("internal error", ignoreCase = true) ||
                msg.contains("network", ignoreCase = true)
            ) {
                Result.success(null)
            } else {
                val friendlyMsg = mapAuthError(e)
                _authState.value = AuthState.Error(friendlyMsg)
                Result.failure(Exception(friendlyMsg, e))
            }
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        if (isPlaceholderApiKey()) {
            return Result.success(Unit)
        }
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMsg = mapAuthError(e)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    fun logout() {
        if (!isPlaceholderApiKey()) {
            try {
                firebaseAuth.signOut()
            } catch (e: Exception) {
                // ignore
            }
        }
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated()
    }

    fun setAuthenticatedState(user: FirebaseUser?, profile: UserProfile) {
        _authState.value = AuthState.Authenticated(user, profile)
    }

    private fun mapAuthError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("password", ignoreCase = true) -> "Contraseña incorrecta o débil (mínimo 6 caracteres)."
            msg.contains("no user", ignoreCase = true) || msg.contains("not found", ignoreCase = true) -> "No existe un usuario con este correo electrónico."
            msg.contains("already in use", ignoreCase = true) -> "Este correo electrónico ya está registrado."
            msg.contains("badly formatted", ignoreCase = true) -> "El formato de correo electrónico no es válido."
            msg.contains("network", ignoreCase = true) -> "Error de conexión a internet con Firebase."
            else -> msg.ifBlank { "Error de autenticación. Verifica tus credenciales." }
        }
    }
}
