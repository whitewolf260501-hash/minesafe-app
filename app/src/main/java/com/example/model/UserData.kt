package com.example.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    // Firestore "users/{uid}" fields:
    val isSuperUser: Boolean = false,
    val isAdmin: Boolean = false,
    val tipoUsuario: String = "usuario", // "superAdmin" | "admin" | "usuario"
    // RTDB "usuarios/{uid}" extended fields:
    val nombre: String = "",
    val empresa: String = "",
    val tipoMina: String = "", // Subterránea, Tajo abierto, Aluvial, etc.
    val latitud: Double = -12.046374, // Default sample / fallback coordinates (ej. mina andina)
    val longitud: Double = -77.042793
) {
    /**
     * Regla exacta especificada:
     * Si isSuperUser es true o tipoUsuario es "superAdmin" -> rol "superAdmin"
     * Si isAdmin es true o tipoUsuario es "admin" -> rol "admin"
     * Si no, "usuario"
     */
    val rolEfectivo: String
        get() = when {
            isSuperUser || tipoUsuario.equals("superAdmin", ignoreCase = true) -> "superAdmin"
            isAdmin || tipoUsuario.equals("admin", ignoreCase = true) -> "admin"
            else -> "usuario"
        }

    val canAccessAdminPanel: Boolean
        get() = rolEfectivo in listOf("admin", "superAdmin")

    val rolDisplay: String
        get() = when (rolEfectivo) {
            "superAdmin" -> "Super Administrador"
            "admin" -> "Administrador de Mina"
            else -> "Operador / Usuario"
        }
}
