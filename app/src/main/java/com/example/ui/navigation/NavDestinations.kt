package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class ScreenDestination(
    val title: String,
    val icon: ImageVector,
    val adminOnly: Boolean = false
) {
    DASHBOARD("Inicio", Icons.Default.Dashboard),
    DEVICES("Dispositivos", Icons.Default.Devices),
    ALERTS("Alertas", Icons.Default.Warning),
    ALERT_CONFIG("Config. Umbrales", Icons.Default.Tune),
    CHARTS("Gráficos", Icons.Default.Analytics),
    MAP("Mapa Mina", Icons.Default.Map),
    HISTORY("Historial", Icons.Default.History),
    PROFILE("Mi Perfil", Icons.Default.Person),
    ADMIN("Gestión Usuarios", Icons.Default.AdminPanelSettings, adminOnly = true)
}
