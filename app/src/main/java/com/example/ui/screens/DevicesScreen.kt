package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceSummary
import com.example.ui.MainViewModel
import com.example.ui.components.CriticalSensorsBarChartCard
import com.example.ui.components.EditGatewayIpDialog
import com.example.ui.components.GatewayDeviceCard
import com.example.ui.components.ShiftTemporalTrendCard
import com.example.ui.components.UsersScrollableBoxCard
import com.example.ui.navigation.ScreenDestination
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber
import java.util.Locale

@Composable
fun DevicesScreen(
    viewModel: MainViewModel,
    onNavigateToCharts: (String) -> Unit
) {
    val devices by viewModel.devicesList.collectAsState()
    val isDeviceConnected by viewModel.isDeviceConnected.collectAsState()
    val deviceLastSeenText by viewModel.deviceLastSeenText.collectAsState()
    val gatewayData by viewModel.gatewayData.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()
    val liveData by viewModel.liveSensorData.collectAsState()
    val alertData by viewModel.alertDeviceData.collectAsState()
    val isGatewayConnected by viewModel.isGatewayConnected.collectAsState()
    val allUsers by viewModel.allUsersList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("TODOS") } // "TODOS", "TELEMETRIA", "GATEWAY", "SENSORES", "PERSONAL"
    var showEditGatewayIpDialog by remember { mutableStateOf(false) }

    val criticalSensorsData = remember(selectedDeviceId, liveData, alertData, isGatewayConnected, isDeviceConnected, devices) {
        viewModel.getCriticalSensorsDataForDevice(selectedDeviceId)
    }

    if (showEditGatewayIpDialog) {
        EditGatewayIpDialog(
            currentIp = gatewayData.ip,
            currentPort = gatewayData.puerto,
            userProfile = userProfile,
            onDismiss = { showEditGatewayIpDialog = false },
            onSave = { newIp, newPort ->
                viewModel.updateGatewayIp(newIp, newPort)
                showEditGatewayIpDialog = false
            },
            onElevateRoleToAdmin = {
                viewModel.setTestingRole("admin")
            }
        )
    }

    val matchesGateway = remember(searchQuery, gatewayData) {
        if (searchQuery.isBlank()) true
        else {
            val q = searchQuery.trim()
            gatewayData.deviceId.contains(q, ignoreCase = true) ||
                    gatewayData.nombre.contains(q, ignoreCase = true) ||
                    gatewayData.ip.contains(q, ignoreCase = true) ||
                    gatewayData.mac.contains(q, ignoreCase = true) ||
                    "gateway".contains(q, ignoreCase = true) ||
                    "concentrador".contains(q, ignoreCase = true) ||
                    "bocamina".contains(q, ignoreCase = true)
        }
    }

    val filteredDevices = remember(devices, searchQuery) {
        if (searchQuery.isBlank()) devices
        else devices.filter {
            it.deviceId.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalDevicesCount = (if (matchesGateway) 1 else 0) + filteredDevices.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("screen_devices")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar Gateway o dispositivo por ID, IP, nombre...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_devices_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips / tabs: Todos, Telemetría, Gateway, Sensores, Personal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == "TODOS",
                onClick = { selectedTab = "TODOS" },
                label = { Text("Todos ($totalDevicesCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                    selectedLabelColor = AccentTeal
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("chip_tab_todos")
            )

            FilterChip(
                selected = selectedTab == "TELEMETRIA",
                onClick = { selectedTab = "TELEMETRIA" },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("9 Variables Críticas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                    selectedLabelColor = AccentTeal
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("chip_tab_telemetria")
            )

            FilterChip(
                selected = selectedTab == "GATEWAY",
                onClick = { selectedTab = "GATEWAY" },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gateway (1)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                    selectedLabelColor = AccentTeal
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("chip_tab_gateway")
            )

            FilterChip(
                selected = selectedTab == "SENSORES",
                onClick = { selectedTab = "SENSORES" },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sensores (${filteredDevices.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                    selectedLabelColor = AccentTeal
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("chip_tab_sensores")
            )

            FilterChip(
                selected = selectedTab == "PERSONAL",
                onClick = { selectedTab = "PERSONAL" },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Personal (${if (allUsers.isNotEmpty()) allUsers.size else 5})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                    selectedLabelColor = AccentTeal
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("chip_tab_personal")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // A. Section: 9 Variables Críticas en Tiempo Real & Tendencia Temporal (Shown in TODOS and TELEMETRIA)
            if (selectedTab == "TODOS" || selectedTab == "TELEMETRIA") {
                item(key = "card_critical_sensors_chart") {
                    CriticalSensorsBarChartCard(
                        criticalData = criticalSensorsData,
                        devicesList = devices,
                        selectedDeviceId = selectedDeviceId,
                        onSelectDevice = { viewModel.setSelectedDevice(it) }
                    )
                }

                item(key = "card_shift_temporal_trend") {
                    ShiftTemporalTrendCard()
                }
            }

            // 1. Gateway Device Section (Shown in TODOS and GATEWAY tabs)
            if ((selectedTab == "TODOS" || selectedTab == "GATEWAY") && matchesGateway) {
                item(key = "section_gateway_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DISPOSITIVO GATEWAY CONCENTRADOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentTeal,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ruta: /dispositivos/gateway",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                item(key = "card_gateway_device") {
                    GatewayDeviceCard(
                        gatewayData = gatewayData,
                        userProfile = userProfile,
                        onEditIpClick = { showEditGatewayIpDialog = true },
                        onPingClick = { viewModel.pingGatewayToFirebase() },
                        onToggleSimulation = { viewModel.toggleGatewaySimulation() }
                    )
                }

                if (selectedTab == "GATEWAY") {
                    item(key = "gateway_architecture_info") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = AccentTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Arquitectura de Comunicaciones Mineras",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "El Concentrador Gateway Bocamina actúa como puente maestro en la mina. Recibe paquetes de sensores subterráneos por LoRaWAN 915MHz y transmite a Firebase RTDB vía Ethernet/WiFi a la IP configurada (${gatewayData.ip}). Como Administrador o Super Administrador puedes reconfigurar esta dirección IP en cualquier momento.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hardware Sensor Status Banner (Shown in TODOS and SENSORES tabs)
            if (selectedTab == "TODOS" || selectedTab == "SENSORES") {
                item(key = "card_esp32_sensor_status") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDeviceConnected) StatusSafeGreen.copy(alpha = 0.08f) else StatusDangerRed.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDeviceConnected) StatusSafeGreen.copy(alpha = 0.35f) else StatusDangerRed.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isDeviceConnected) StatusSafeGreen else StatusDangerRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isDeviceConnected) "ESP32 Principal: Conectado" else "ESP32 Principal: Desconectado",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDeviceConnected) StatusSafeGreen else StatusDangerRed
                                )
                                Text(
                                    text = "$deviceLastSeenText • RTDB minefase-fc5f5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.toggleDeviceSimulation() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isDeviceConnected) "Simular Off" else "Simular On",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item(key = "section_sensors_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NODOS SENSORES CONECTADOS (${filteredDevices.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ruta: /dispositivos",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentTeal
                        )
                    }
                }

                if (filteredDevices.isEmpty()) {
                    item(key = "sensors_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron nodos sensores con el filtro actual.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(filteredDevices, key = { it.deviceId }) { device ->
                        DeviceItemCard(
                            device = device,
                            onViewCharts = {
                                viewModel.selectChartDevice(device.deviceId)
                                onNavigateToCharts(device.deviceId)
                            },
                            onToggleAlarma = { viewModel.toggleAlarma(device.deviceId) },
                            onToggleLuz = { viewModel.toggleLuz(device.deviceId) }
                        )
                    }
                }
            }

            // Section: Control de Personal en Faena (Shown in TODOS and PERSONAL tabs)
            if (selectedTab == "TODOS" || selectedTab == "PERSONAL") {
                item(key = "section_personal_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONTROL DE PERSONAL Y TURNOS MINEROS",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentTeal,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dotación activa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                item(key = "card_users_scrollable") {
                    UsersScrollableBoxCard(
                        usersList = allUsers
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceItemCard(
    device: DeviceSummary,
    onViewCharts: () -> Unit,
    onToggleAlarma: () -> Unit,
    onToggleLuz: () -> Unit
) {
    val statusColor = when (device.status) {
        "Alerta" -> StatusDangerRed
        "Offline" -> StatusWarningAmber
        else -> StatusSafeGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${device.deviceId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Device ID, Name & Status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryTealDark.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Sensor",
                            tint = PrimaryTealDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ID: ${device.deviceId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status pill
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = device.status.uppercase(Locale.getDefault()),
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (device.temperatura != null) {
                    MiniTelemetryPill(
                        label = "TEMP",
                        value = "${String.format(Locale.US, "%.1f", device.temperatura)}°C",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (device.humedad != null) {
                    MiniTelemetryPill(
                        label = "HUMEDAD",
                        value = "${String.format(Locale.US, "%.1f", device.humedad)}%",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (device.mq135 != null) {
                    MiniTelemetryPill(
                        label = "GASES MQ",
                        value = "${device.mq135.toInt()} ppm",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (device.co != null) {
                    MiniTelemetryPill(
                        label = "CO",
                        value = "${device.co.toInt()} ppm",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (device.co2 != null) {
                    MiniTelemetryPill(
                        label = "CO2",
                        value = "${device.co2.toInt()} ppm",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actuator Controls for this specific device
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleAlarma,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_alarma_${device.deviceId}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.alarma) StatusDangerRed else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (device.alarma) Color.White else StatusDangerRed
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (device.alarma) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (device.alarma) "ALARMA ON" else "Alarma",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onToggleLuz,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_luz_${device.deviceId}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.luz) StatusWarningAmber else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (device.luz) Color.Black else StatusWarningAmber
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (device.luz) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (device.luz) "LUZ ON" else "Luz",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Última lectura: ${device.lastReadingTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                OutlinedButton(
                    onClick = onViewCharts,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_charts_${device.deviceId}")
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Historial y Gráficos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun MiniTelemetryPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        }
    }
}
