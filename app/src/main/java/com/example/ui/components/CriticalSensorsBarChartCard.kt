package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceSummary
import com.example.model.MetricStatus
import com.example.model.MiningCriticalSensorsData
import com.example.model.SensorMetricDefinition
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber

@Composable
fun CriticalSensorsBarChartCard(
    criticalData: MiningCriticalSensorsData,
    devicesList: List<DeviceSummary>,
    selectedDeviceId: String,
    onSelectDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMetricForTooltip by remember { mutableStateOf<SensorMetricDefinition?>(null) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    val metrics = remember(criticalData) { criticalData.toMetricList() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_critical_sensors_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrimaryTealDark.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Title & Full Dropdown Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NIVELES DE SENSORES EN TIEMPO REAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentTeal,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "9 Variables Críticas (Norma DS 132)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Dropdown menu button for full equipment selection
                Box {
                    OutlinedButton(
                        onClick = { isDropdownOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_dropdown_device")
                    ) {
                        Text(
                            text = "Cambiar Equipo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownOpen,
                        onDismissRequest = { isDropdownOpen = false }
                    ) {
                        devicesList.forEach { dev ->
                            val isSelected = dev.deviceId == selectedDeviceId
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = dev.name.ifBlank { dev.deviceId },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (isSelected) AccentTeal else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "ID: ${dev.deviceId} • ${dev.status}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectDevice(dev.deviceId)
                                    isDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Access Buttons Row (Botones de acceso rápido entre nodos y gateways)
            Text(
                text = "Acceso rápido a equipos:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val quickOptions = listOf(
                "dispositivo1" to "ESP32 Galería 1",
                "NODO_U-01" to "NODO_U-01 Avance",
                "device_38A839E81F84" to "ESP32 Chuqui",
                "device_A4CB2F124B00" to "ESP32 San José",
                "gateway" to "Gateway Bocamina"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickOptions.forEach { (id, label) ->
                    val isSelected = selectedDeviceId == id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryTealDark else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryTealDark else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .clickable { onSelectDevice(id) }
                            .testTag("quick_device_$id")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isSelected) Color.White else AccentTeal,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Equipo Indicators Banner (Estado de enlace, sector y batería restante)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (criticalData.status == "Online") StatusSafeGreen else StatusDangerRed,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = criticalData.deviceName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(12.dp), tint = AccentTeal)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = criticalData.sector, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(12.dp), tint = StatusSafeGreen)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "${criticalData.bateria}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusSafeGreen)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // The 9 Critical Variables Bar Chart
            // Horizontal scrollable container to comfortably fit all 9 bars with exact readings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                metrics.forEach { metric ->
                    SingleSensorVerticalBar(
                        metric = metric,
                        onClick = { selectedMetricForTooltip = metric }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend & Safe Limit note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = StatusSafeGreen, label = "Seguro")
                    LegendItem(color = StatusWarningAmber, label = "Alerta")
                    LegendItem(color = StatusDangerRed, label = "Crítico")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Límite DS 132",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Interactive Tooltip Dialog on bar tap/click
    if (selectedMetricForTooltip != null) {
        val metric = selectedMetricForTooltip!!
        val statusText = when (metric.status) {
            MetricStatus.SAFE -> "SEGURO (Dentro de la norma)"
            MetricStatus.WARNING -> "ALERTA PREVENTIVA"
            MetricStatus.CRITICAL -> "NIVEL CRÍTICO PELIGROSO"
        }
        val statusColor = when (metric.status) {
            MetricStatus.SAFE -> StatusSafeGreen
            MetricStatus.WARNING -> StatusWarningAmber
            MetricStatus.CRITICAL -> StatusDangerRed
        }

        AlertDialog(
            onDismissRequest = { selectedMetricForTooltip = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = metric.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = metric.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = metric.regulatoryStandard,
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentTeal,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Medición actual:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text(
                            text = "${metric.formattedValue} ${metric.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (metric.isMinThreshold) "Límite reglamentario mínimo:" else "Umbral máximo permisible:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${metric.threshold} ${metric.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = metric.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMetricForTooltip = null }) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun SingleSensorVerticalBar(
    metric: SensorMetricDefinition,
    onClick: () -> Unit
) {
    val statusColor = when (metric.status) {
        MetricStatus.SAFE -> StatusSafeGreen
        MetricStatus.WARNING -> StatusWarningAmber
        MetricStatus.CRITICAL -> StatusDangerRed
    }

    val animatedProgress by animateFloatAsState(
        targetValue = metric.progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "bar_progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .fillMaxHeight()
            .clickable { onClick() }
            .testTag("sensor_bar_${metric.id}")
    ) {
        // Direct value & unit above the bar
        Text(
            text = "${metric.formattedValue}\n${metric.unit}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
            modifier = Modifier.height(26.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical Bar Container with Safe Limit Line Marker
        Box(
            modifier = Modifier
                .width(24.dp)
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Safe Limit Line marker across the bar container
            val thresholdYFraction = 1f - metric.thresholdFraction.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(1f - thresholdYFraction)
                    .align(Alignment.BottomCenter)
            ) {
                // Dashed/solid horizontal line at the threshold height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White)
                        .align(Alignment.TopCenter)
                )
            }

            // The animated filled bar with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedProgress.coerceIn(0.05f, 1f))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                statusColor,
                                statusColor.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Emoji & Short Label below bar
        Text(
            text = metric.emoji,
            fontSize = 13.sp
        )
        Text(
            text = metric.name.take(6),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
