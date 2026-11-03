package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceSummary
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber

@Composable
fun ActuatorControlCard(
    selectedDeviceId: String,
    devicesList: List<DeviceSummary>,
    isAlarmaActive: Boolean,
    isLuzActive: Boolean,
    isAutoTriggered: Boolean,
    triggerReason: String,
    onSelectDevice: (String) -> Unit,
    onToggleAlarma: () -> Unit,
    onToggleLuz: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSimulationDialog: (() -> Unit)? = null,
    gatewayIp: String = "192.168.1.100",
    onOpenChangeGatewayIp: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_alert")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val alarmaBgColor by animateColorAsState(
        targetValue = if (isAlarmaActive) StatusDangerRed else MaterialTheme.colorScheme.surface,
        label = "alarma_bg"
    )

    val luzBgColor by animateColorAsState(
        targetValue = if (isLuzActive) StatusWarningAmber else MaterialTheme.colorScheme.surface,
        label = "luz_bg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_actuadores_emergencia"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isAlarmaActive || isLuzActive) 2.dp else 1.dp,
            color = when {
                isAlarmaActive -> StatusDangerRed
                isLuzActive -> StatusWarningAmber
                isAutoTriggered -> StatusWarningAmber.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Device info & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isAlarmaActive || isAutoTriggered) StatusDangerRed.copy(alpha = 0.15f)
                                else PrimaryTealDark.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAlarmaActive || isAutoTriggered) Icons.Default.Campaign else Icons.Default.Sensors,
                            contentDescription = "Actuadores",
                            tint = if (isAlarmaActive || isAutoTriggered) StatusDangerRed else PrimaryTealDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CONTROL DE ACTUADORES REMOTO",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryTealDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Alarma Sonora y Luz de Emergencia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                    }
                }

                // Status chip
                val statusText = when {
                    selectedDeviceId == "gateway" -> "📡 GATEWAY DE ENLACE"
                    isAlarmaActive && isLuzActive -> "🚨 ALARMA Y BALIZA ACTIVAS"
                    isAlarmaActive -> "🚨 SIRENA ACTIVADA"
                    isLuzActive -> "💡 BALIZA ENCENDIDA"
                    isAutoTriggered -> "⚠️ CONDICIONES DE ALERTA"
                    else -> "🟢 EN REPOSO"
                }
                val statusColor = when {
                    selectedDeviceId == "gateway" -> PrimaryTealDark
                    isAlarmaActive -> StatusDangerRed
                    isLuzActive -> StatusWarningAmber
                    isAutoTriggered -> StatusWarningAmber
                    else -> StatusSafeGreen
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .then(if ((isAlarmaActive || isAutoTriggered) && selectedDeviceId != "gateway") Modifier.alpha(pulseAlpha) else Modifier)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explanation notice: automatic trigger vs manual trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (selectedDeviceId == "gateway") PrimaryTealDark.copy(alpha = 0.1f)
                        else if (isAlarmaActive) StatusDangerRed.copy(alpha = 0.12f)
                        else if (isLuzActive || isAutoTriggered) StatusWarningAmber.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (selectedDeviceId == "gateway") Icons.Default.CloudDone
                        else if (isAlarmaActive) Icons.Default.Campaign
                        else if (isAutoTriggered) Icons.Default.Warning
                        else Icons.Default.Info,
                        contentDescription = "Estado",
                        tint = if (selectedDeviceId == "gateway") PrimaryTealDark
                        else if (isAlarmaActive) StatusDangerRed
                        else if (isAutoTriggered) StatusWarningAmber
                        else PrimaryTealDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            selectedDeviceId == "gateway" -> "El Concentrador Gateway no posee actuadores ni sensores directos; reenvía los datos de los nodos a Firebase."
                            isAlarmaActive -> "🚨 Sirena acústica y vibración constante en curso. Pulsa 'APAGAR ALARMA' para silenciar el sistema."
                            isLuzActive -> "💡 Luz y baliza luminosa encendidas en el socavón. Pulsa 'APAGAR LUZ' para apagar."
                            isAutoTriggered -> "⚠️ Sensores detectan concentración de riesgo. Puedes encender la sirena o la baliza manualmente con los botones."
                            else -> "Sistema en reposo operativo. Pulsa 'ACTIVAR ALARMA' o 'ENCENDER LUZ' para operar los actuadores del nodo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }

            // Target Device Selector
            if (devicesList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dispositivo:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(devicesList) { dev ->
                            val isSelected = dev.deviceId == selectedDeviceId
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectDevice(dev.deviceId) },
                                label = {
                                    Text(
                                        text = when {
                                            dev.deviceId == "gateway" -> "Gateway LoRa/WiFi"
                                            dev.deviceId == "NODO_U-01" -> "NODO_U-01 (Principal)"
                                            else -> dev.deviceId
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTealDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedDeviceId == "gateway") {
                // Gateway notice instead of Siren/Light buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1E2E), RoundedCornerShape(12.dp))
                        .border(1.dp, PrimaryTealDark.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PrimaryTealDark.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = PrimaryTealDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CONCENTRADOR GATEWAY SELECCIONADO",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "IP: $gatewayIp • Puente LoRa/WiFi hacia Firebase",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Este equipo no cuenta con actuadores (sirena sonora o baliza) ni sensores ambientales propios. Comunica los nodos subterráneos con la nube. Para operar sirenas o luces, selecciona un nodo sensor (ej. NODO_U-01).",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onOpenChangeGatewayIp != null) {
                                Button(
                                    onClick = onOpenChangeGatewayIp,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryTealDark,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cambiar IP ($gatewayIp)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { onSelectDevice("NODO_U-01") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ir a NODO_U-01", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // THE TWO MAIN ACTION BUTTONS: ALARMA & LUZ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // BUTTON 1: ALARMA / SIRENA (Buzzer)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_toggle_alarm"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAlarmaActive) StatusDangerRed.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(
                            width = if (isAlarmaActive) 2.dp else 1.dp,
                            color = if (isAlarmaActive) StatusDangerRed else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = if (isAlarmaActive) StatusDangerRed else StatusDangerRed.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                                    .then(if (isAlarmaActive) Modifier.alpha(pulseAlpha) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAlarmaActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = "Alarma Sonora",
                                    tint = if (isAlarmaActive) Color.White else StatusDangerRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "ALARMA SONORA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isAlarmaActive) StatusDangerRed else MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (isAlarmaActive) "ACTIVADA (SONANDO)" else "En Reposo",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isAlarmaActive) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (isAlarmaActive) StatusDangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = onToggleAlarma,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("button_toggle_alarm"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAlarmaActive) StatusDangerRed else PrimaryTealDark,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isAlarmaActive) "APAGAR ALARMA" else "ACTIVAR ALARMA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // BUTTON 2: LUZ DE DISPOSITIVO (Foco / Linterna / LED)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_toggle_light"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLuzActive) StatusWarningAmber.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(
                            width = if (isLuzActive) 2.dp else 1.dp,
                            color = if (isLuzActive) StatusWarningAmber else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = if (isLuzActive) StatusWarningAmber else StatusWarningAmber.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                                    .then(if (isLuzActive) Modifier.alpha(pulseAlpha) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isLuzActive) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                    contentDescription = "Luz de Emergencia",
                                    tint = if (isLuzActive) Color.Black else StatusWarningAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "LUZ DE EMERGENCIA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isLuzActive) StatusWarningAmber else MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (isLuzActive) "ENCENDIDA (ILUMINANDO)" else "Apagada",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isLuzActive) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (isLuzActive) StatusWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = onToggleLuz,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("button_toggle_light"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLuzActive) StatusWarningAmber else PrimaryTealDark,
                                    contentColor = if (isLuzActive) Color.Black else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isLuzActive) "APAGAR LUZ" else "ENCENDER LUZ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (onOpenSimulationDialog != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenSimulationDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_open_simulation_dialog"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color(0xFFF97316)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFF97316)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Centro de Simulacro y Protocolo de Evacuación (DS 132)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
