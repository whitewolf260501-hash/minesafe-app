package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertThresholdSettings
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber

@Composable
fun AlertConfigScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val thresholds by viewModel.alertThresholds.collectAsState()
    val liveData by viewModel.liveSensorData.collectAsState()
    val alertData by viewModel.alertDeviceData.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()

    // Local editable fields
    var thCoText by remember(thresholds) { mutableStateOf(thresholds.thCo.toString()) }
    var thCo2Text by remember(thresholds) { mutableStateOf(thresholds.thCo2.toString()) }
    var thPm10Text by remember(thresholds) { mutableStateOf(thresholds.thPm10.toString()) }
    var thPm25Text by remember(thresholds) { mutableStateOf(thresholds.thPm25.toString()) }
    var thHumText by remember(thresholds) { mutableStateOf(thresholds.thHumedad.toString()) }
    var thTempText by remember(thresholds) { mutableStateOf(thresholds.thTemperatura.toString()) }
    var thMq2Text by remember(thresholds) { mutableStateOf(thresholds.thMq2.toString()) }

    var actCo by remember(thresholds) { mutableStateOf(thresholds.activoCo) }
    var actCo2 by remember(thresholds) { mutableStateOf(thresholds.activoCo2) }
    var actPm10 by remember(thresholds) { mutableStateOf(thresholds.activoPm10) }
    var actPm25 by remember(thresholds) { mutableStateOf(thresholds.activoPm25) }
    var actHum by remember(thresholds) { mutableStateOf(thresholds.activoHumedad) }
    var actTemp by remember(thresholds) { mutableStateOf(thresholds.activoTemperatura) }
    var actMq2 by remember(thresholds) { mutableStateOf(thresholds.activoMq2) }

    var showRestoreDialog by remember { mutableStateOf(false) }

    val canEdit = userProfile.canAccessAdminPanel || userProfile.isAdmin || userProfile.isSuperUser

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = StatusWarningAmber) },
            title = { Text("¿Restaurar valores de referencia?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Esta acción restablecerá los límites de alerta a los valores iniciales de fábrica de referencia (CO: 50, CO₂: 1000, PM2.5: 50, PM10: 100, Hum: 80%, Temp: 28°C, MQ-2: 400). ¿Desea continuar?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDefaultAlertThresholds()
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_alert_config"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(PrimaryTealDark.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Configuración",
                                    tint = PrimaryTealDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CONFIGURACIÓN DE ALERTAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryTealDark,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Umbrales Ambientales Dinámicos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E1B4B),
                            border = BorderStroke(1.dp, Color(0xFF4338CA))
                        ) {
                            Text(
                                text = "Firebase RTDB",
                                color = Color(0xFFA5B4FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ajuste los límites de alerta para el dispositivo $selectedDeviceId sin requerir reflashear el firmware del ESP32. Los cambios se sincronizan en la nube y se aplican inmediatamente al motor de detección.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. Normativa D.S. N° 132 / SERNAGEOMIN Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261805)),
                border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Normativa",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AVISO DE CUMPLIMIENTO (D.S. N° 132 / SERNAGEOMIN)",
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Los umbrales configurados aquí son límites operacionales de referencia de la plataforma IoT. Las mediciones de los sensores analógicos MQ-2 y PM2.5 se procesan en valores ADC crudos y no deben interpretarse como concentraciones físicas certificadas (Art. 136 del D.S. N° 132).",
                            color = Color(0xFFFEF3C7),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 3. Tarjeta PM2.5 (Partículas Respirables)
        item {
            ThresholdConfigItemCard(
                title = "Partículas Finas PM2.5",
                subtitle = "Sensor de polvo respirable óptico",
                unit = "µg/m³ (o ADC)",
                icon = Icons.Default.Air,
                iconTint = Color(0xFFC084FC),
                currentValueText = "${alertData.pm25} µg/m³",
                thresholdText = thPm25Text,
                onThresholdChange = { thPm25Text = it },
                isActive = actPm25,
                onToggleActive = { actPm25 = it },
                enabled = canEdit
            )
        }

        // 4. Tarjeta Gas MQ-2
        item {
            ThresholdConfigItemCard(
                title = "Gases Combustibles / MQ-2",
                subtitle = "Sensor analógico de gases inflamables y humo",
                unit = "ADC raw",
                icon = Icons.Default.Warning,
                iconTint = Color(0xFFF59E0B),
                currentValueText = "${liveData.mq135} ADC",
                thresholdText = thMq2Text,
                onThresholdChange = { thMq2Text = it },
                isActive = actMq2,
                onToggleActive = { actMq2 = it },
                enabled = canEdit
            )
        }

        // 5. Tarjeta Temperatura
        item {
            ThresholdConfigItemCard(
                title = "Temperatura en Socavón",
                subtitle = "Límite térmico de confort y ventilación",
                unit = "°C",
                icon = Icons.Default.DeviceThermostat,
                iconTint = Color(0xFFFB923C),
                currentValueText = "${liveData.temperatura} °C",
                thresholdText = thTempText,
                onThresholdChange = { thTempText = it },
                isActive = actTemp,
                onToggleActive = { actTemp = it },
                enabled = canEdit
            )
        }

        // 6. Tarjeta Humedad
        item {
            ThresholdConfigItemCard(
                title = "Humedad Relativa",
                subtitle = "Riesgo de condensación y desprendimiento",
                unit = "%",
                icon = Icons.Default.Opacity,
                iconTint = Color(0xFF38BDF8),
                currentValueText = "${liveData.humedad} %",
                thresholdText = thHumText,
                onThresholdChange = { thHumText = it },
                isActive = actHum,
                onToggleActive = { actHum = it },
                enabled = canEdit
            )
        }

        // 7. Tarjeta Monóxido de Carbono (CO)
        item {
            ThresholdConfigItemCard(
                title = "Monóxido de Carbono (CO)",
                subtitle = "Límite crítico tóxico (ppm)",
                unit = "ppm",
                icon = Icons.Default.Warning,
                iconTint = StatusDangerRed,
                currentValueText = "${alertData.co} ppm",
                thresholdText = thCoText,
                onThresholdChange = { thCoText = it },
                isActive = actCo,
                onToggleActive = { actCo = it },
                enabled = canEdit
            )
        }

        // 8. Tarjeta Dióxido de Carbono (CO2)
        item {
            ThresholdConfigItemCard(
                title = "Dióxido de Carbono (CO₂)",
                subtitle = "Calidad del aire y deficiencia de ventilación",
                unit = "ppm",
                icon = Icons.Default.Sensors,
                iconTint = Color(0xFFFACC15),
                currentValueText = "${alertData.co2} ppm",
                thresholdText = thCo2Text,
                onThresholdChange = { thCo2Text = it },
                isActive = actCo2,
                onToggleActive = { actCo2 = it },
                enabled = canEdit
            )
        }

        // 9. Tarjeta PM10
        item {
            ThresholdConfigItemCard(
                title = "Polvo en Suspensión PM10",
                subtitle = "Partículas sedimentables en faena",
                unit = "µg/m³",
                icon = Icons.Default.Air,
                iconTint = Color(0xFFA78BFA),
                currentValueText = "${alertData.pm10} µg/m³",
                thresholdText = thPm10Text,
                onThresholdChange = { thPm10Text = it },
                isActive = actPm10,
                onToggleActive = { actPm10 = it },
                enabled = canEdit
            )
        }

        // 10. Audit and Save Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Auditoría de Cambios:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Última modificación: ${thresholds.ultimoCambioFecha} por ${thresholds.ultimoCambioUsuario}",
                        fontSize = 11.5.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            enabled = canEdit
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar Fábrica", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = {
                                val newSettings = AlertThresholdSettings(
                                    thCo = thCoText.toDoubleOrNull() ?: thresholds.thCo,
                                    thCo2 = thCo2Text.toDoubleOrNull() ?: thresholds.thCo2,
                                    thPm10 = thPm10Text.toDoubleOrNull() ?: thresholds.thPm10,
                                    thPm25 = thPm25Text.toDoubleOrNull() ?: thresholds.thPm25,
                                    thHumedad = thHumText.toDoubleOrNull() ?: thresholds.thHumedad,
                                    thTemperatura = thTempText.toDoubleOrNull() ?: thresholds.thTemperatura,
                                    thMq2 = thMq2Text.toDoubleOrNull() ?: thresholds.thMq2,
                                    activoCo = actCo,
                                    activoCo2 = actCo2,
                                    activoPm10 = actPm10,
                                    activoPm25 = actPm25,
                                    activoHumedad = actHum,
                                    activoTemperatura = actTemp,
                                    activoMq2 = actMq2
                                )
                                viewModel.updateAlertThresholds(newSettings, "Ajuste desde ventana móvil")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_guardar_umbrales"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTealDark),
                            enabled = canEdit
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar Umbrales", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ThresholdConfigItemCard(
    title: String,
    subtitle: String,
    unit: String,
    icon: ImageVector,
    iconTint: Color,
    currentValueText: String,
    thresholdText: String,
    onThresholdChange: (String) -> Unit,
    isActive: Boolean,
    onToggleActive: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF334155) else Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(iconTint.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text(text = subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = onToggleActive,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryTealDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Medición en Vivo:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentValueText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Límite Alerta:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = onThresholdChange,
                        modifier = Modifier
                            .width(105.dp)
                            .height(50.dp),
                        enabled = enabled && isActive,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryTealDark,
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        trailingIcon = {
                            Text(
                                text = unit.take(4),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}
