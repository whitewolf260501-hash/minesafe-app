package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.BorderAccentGreen
import com.example.ui.theme.BorderAccentOrange
import com.example.ui.theme.BorderAccentPurple
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber

@Composable
fun WelcomeHeroBanner(
    userProfile: UserProfile,
    totalUsersCount: Int,
    onlineDevicesCount: Int,
    totalDevicesCount: Int,
    activeDeviceName: String,
    activeDeviceBattery: Int,
    activeAlertsCount: Int,
    onRoleChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val roleColor = when (userProfile.rolEfectivo) {
        "superAdmin" -> Color(0xFFA855F7) // Purple
        "admin" -> Color(0xFF06B6D4)      // Cyan
        else -> Color(0xFFF59E0B)         // Amber
    }

    val roleIcon: ImageVector = when (userProfile.rolEfectivo) {
        "superAdmin" -> Icons.Default.WorkspacePremium
        "admin" -> Icons.Default.AdminPanelSettings
        else -> Icons.Default.Engineering
    }

    val roleTitle = when (userProfile.rolEfectivo) {
        "superAdmin" -> "Superadmin"
        "admin" -> "Admin"
        else -> "Operador"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Welcome Card with Official Role Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("welcome_hero_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, roleColor.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bienvenido, ${userProfile.nombre.ifBlank { "Manuel Crisostomo" }} 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${userProfile.empresa.ifBlank { "Minera Los Andes" }} • ${userProfile.tipoMina.ifBlank { "Mina Subterránea" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    // Role Badge with theme colors & official icon
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = roleColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, roleColor),
                        modifier = Modifier.testTag("badge_user_role")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = roleIcon,
                                contentDescription = roleTitle,
                                tint = roleColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "($roleTitle)",
                                color = roleColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast Role Switcher (Superadmin | Admin | Operador)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Probar rol:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )

                    RoleChipButton(
                        label = "Superadmin",
                        isSelected = userProfile.rolEfectivo == "superAdmin",
                        color = Color(0xFFA855F7),
                        onClick = { onRoleChange("superAdmin") }
                    )
                    RoleChipButton(
                        label = "Admin",
                        isSelected = userProfile.rolEfectivo == "admin",
                        color = Color(0xFF06B6D4),
                        onClick = { onRoleChange("admin") }
                    )
                    RoleChipButton(
                        label = "Operador",
                        isSelected = userProfile.rolEfectivo == "usuario",
                        color = Color(0xFFF59E0B),
                        onClick = { onRoleChange("usuario") }
                    )
                }
            }
        }

        // 4 Quick Stat Cards (Inspired directly by the Dashboard UI layout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Total Usuarios
            QuickStatCard(
                title = "Total Usuarios",
                value = "$totalUsersCount",
                subtext = "Personal en faena",
                icon = Icons.Default.Group,
                accentColor = BorderAccentBlue,
                modifier = Modifier.weight(1f),
                testTag = "stat_card_total_users"
            )

            // Card 2: Sensores IoT Online
            QuickStatCard(
                title = "Sensores IoT",
                value = "$onlineDevicesCount / $totalDevicesCount",
                subtext = "Enlace activo",
                icon = Icons.Default.Sensors,
                accentColor = StatusSafeGreen,
                modifier = Modifier.weight(1f),
                testTag = "stat_card_sensors_online"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Dispositivo Activo
            QuickStatCard(
                title = "Dispositivo Activo",
                value = activeDeviceName,
                subtext = "🔋 $activeDeviceBattery% • Monitoreo",
                icon = Icons.Default.Devices,
                accentColor = AccentTeal,
                modifier = Modifier.weight(1f),
                testTag = "stat_card_active_device"
            )

            // Card 4: Alertas de Faena
            val isSafe = activeAlertsCount == 0
            QuickStatCard(
                title = "Alertas de Faena",
                value = if (isSafe) "0 Críticas" else "$activeAlertsCount Alerta(s)",
                subtext = if (isSafe) "Normativa DS 132 OK" else "Acción requerida",
                icon = if (isSafe) Icons.Default.Shield else Icons.Default.Warning,
                accentColor = if (isSafe) StatusSafeGreen else StatusDangerRed,
                modifier = Modifier.weight(1f),
                testTag = "stat_card_alerts"
            )
        }
    }
}

@Composable
fun QuickStatCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RoleChipButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
