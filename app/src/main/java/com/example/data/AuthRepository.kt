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

    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
            if (auth.currentUser == null && _authState.value !is AuthState.Authenticated) {
                _authState.value = AuthState.Unauthenticated()
            }
        }
    }

    suspend fun login(email: String, pass: String): Result<FirebaseUser?> {
        _authState.value = AuthState.Loading
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            _currentUser.value = result.user
            Result.success(result.user)
        } catch (e: Exception) {
            val friendlyMsg = mapAuthError(e)
            _authState.value = AuthState.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun register(email: String, pass: String): Result<FirebaseUser?> {
        _authState.value = AuthState.Loading
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            _currentUser.value = result.user
            Result.success(result.user)
        } catch (e: Exception) {
            val friendlyMsg = mapAuthError(e)
            _authState.value = AuthState.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMsg = mapAuthError(e)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    fun logout() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            // ignore
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
