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
    val longitud: Double = -77.042793,
    val rut: String = "",
    val cargo: String = "",
    val sectorTurno: String = "",
    val isOnline: Boolean = true
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
            else -> "Operador Minero"
        }

    val rutDisplay: String
        get() = if (rut.isNotBlank()) rut else when (rolEfectivo) {
            "superAdmin" -> "16.845.920-K"
            "admin" -> "18.320.145-2"
            else -> "19.782.604-5"
        }

    val cargoDisplay: String
        get() = if (cargo.isNotBlank()) cargo else when (rolEfectivo) {
            "superAdmin" -> "Superintendente General de Faena"
            "admin" -> "Jefe de Prevención y Seguridad DS 132"
            else -> "Operador Técnico de Monitoreo"
        }

    val sectorTurnoDisplay: String
        get() = if (sectorTurno.isNotBlank()) sectorTurno else when (rolEfectivo) {
            "superAdmin" -> "Turno Continuo • Centro de Control"
            "admin" -> "Turno A (Día) • Bocamina & Galerías"
            else -> "Turno B (Rotativo) • Galería 1 Frente Avance"
        }
}

