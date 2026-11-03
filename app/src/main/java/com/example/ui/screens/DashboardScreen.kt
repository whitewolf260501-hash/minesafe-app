package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.ui.MainViewModel
import com.example.ui.components.ActuatorControlCard
import com.example.ui.components.BorderCard
import com.example.model.ReportedIncident
import com.example.ui.components.ChangeGatewayIpDialog
import com.example.ui.components.DescribeIncidentDialog
import com.example.ui.components.EmergencySimulationDialog
import com.example.ui.components.GatewayDetailCard
import com.example.ui.components.MetricCard
import com.example.ui.components.ReportIncidentDialog
import com.example.ui.components.WelcomeHeroBanner
import com.example.ui.navigation.ScreenDestination
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.BorderAccentGreen
import com.example.ui.theme.BorderAccentOrange
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (ScreenDestination) -> Unit
) {
    val liveData by viewModel.liveSensorData.collectAsState()
    val alertData by viewModel.alertDeviceData.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDeviceConnected by viewModel.isDeviceConnected.collectAsState()
    val deviceLastSeenText by viewModel.deviceLastSeenText.collectAsState()
    val isGatewayConnected by viewModel.isGatewayConnected.collectAsState()
    val gatewayLastSeenText by viewModel.gatewayLastSeenText.collectAsState()
    val gatewayInfo by viewModel.gatewayInfo.collectAsState()
    val devicesList by viewModel.devicesList.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()
    val allUsers by viewModel.allUsersList.collectAsState()
    val isAlarmaActive by viewModel.isAlarmaActive.collectAsState()
    val isLuzActive by viewModel.isLuzActive.collectAsState()
    val actuatorTriggerReason by viewModel.actuatorTriggerReason.collectAsState()
    val isEmergencySimulationActive by viewModel.isEmergencySimulationActive.collectAsState()
    val simulationScenarioName by viewModel.simulationScenarioName.collectAsState()
    val reportedIncidents by viewModel.reportedIncidents.collectAsState()
    val isAutoTriggered = !alertData.isAllSafe || isEmergencySimulationActive
    var showSimulationDialog by remember { mutableStateOf(false) }
    var showChangeGatewayIpDialog by remember { mutableStateOf(false) }
    var showReportIncidentDialog by remember { mutableStateOf(false) }
    var incidentToDescribe by remember { mutableStateOf<ReportedIncident?>(null) }

    val criticalSensorsData = remember(selectedDeviceId, liveData, alertData, isGatewayConnected, isDeviceConnected, devicesList) {
        viewModel.getCriticalSensorsDataForDevice(selectedDeviceId)
    }

    if (showChangeGatewayIpDialog) {
        ChangeGatewayIpDialog(
            currentIp = gatewayInfo.ip,
            onSaveIp = { newIp -> viewModel.updateGatewayIp(newIp) },
            onDismiss = { showChangeGatewayIpDialog = false }
        )
    }

    if (showReportIncidentDialog) {
        ReportIncidentDialog(
            onDismiss = { showReportIncidentDialog = false },
            onSubmit = { tipo, ubicacion, gravedad, descripcion, activarAlarma ->
                viewModel.reportIncident(tipo, ubicacion, gravedad, descripcion, activarAlarma)
            }
        )
    }

    if (incidentToDescribe != null) {
        val incident = incidentToDescribe!!
        DescribeIncidentDialog(
            itemTitle = incident.tipo,
            itemLocation = incident.ubicacion,
            severity = incident.gravedad,
            initialDescription = incident.descripcion,
            initialStatus = incident.estado,
            authorName = incident.autorNombre,
            formattedDate = incident.formattedDate,
            onDismiss = { incidentToDescribe = null },
            onSave = { newDesc, newStatus ->
                viewModel.updateIncidentDescription(incident.id, newDesc, newStatus)
                incidentToDescribe = null
            }
        )
    }

    if (showSimulationDialog) {
        EmergencySimulationDialog(
            currentUserProfile = userProfile,
            isSimulationActive = isEmergencySimulationActive,
            activeScenarioName = simulationScenarioName,
            onStartSimulation = { scenario, triggerActuators, userDescription ->
                viewModel.startEmergencySimulation(scenario, triggerActuators, userDescription)
            },
            onStopSimulation = {
                viewModel.stopEmergencySimulation()
            },
            onDismiss = { showSimulationDialog = false },
            onDescribeSimulation = {
                val simIncident = reportedIncidents.firstOrNull { it.id.startsWith("sim_") }
                    ?: ReportedIncident(
                        id = "sim_${System.currentTimeMillis()}",
                        tipo = simulationScenarioName.ifBlank { "Simulacro de Evacuación" },
                        ubicacion = "Sector Subterráneo / General",
                        gravedad = "Crítica",
                        descripcion = "Simulacro en curso: $simulationScenarioName",
                        autorNombre = userProfile.nombre,
                        autorRol = userProfile.rolDisplay,
                        timestamp = System.currentTimeMillis()
                    )
                incidentToDescribe = simIncident
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Hero Banner with Official Role Badges and 4 Quick Stat Cards
        item {
            val totalUsersCount = if (allUsers.isNotEmpty()) allUsers.size else 5
            val onlineDevs = devicesList.count { it.status == "Online" } + (if (isGatewayConnected) 1 else 0)
            val totalDevs = (if (devicesList.isNotEmpty()) devicesList.size else 4) + 1

            WelcomeHeroBanner(
                userProfile = userProfile,
                totalUsersCount = totalUsersCount,
                onlineDevicesCount = onlineDevs,
                totalDevicesCount = totalDevs,
                activeDeviceName = criticalSensorsData.deviceName,
                activeDeviceBattery = criticalSensorsData.bateria,
                activeAlertsCount = alertData.activeAlerts.size,
                onRoleChange = { newRole -> viewModel.setTestingRole(newRole) }
            )
        }

        // 2. Shortcut to Devices Window (Telemetría de 9 Variables Críticas & Concentrador Gateway)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(ScreenDestination.DEVICES) }
                    .testTag("card_shortcut_to_devices"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PrimaryTealDark.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryTealDark.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MONITOREO DE DISPOSITIVOS Y 9 VARIABLES",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Gráfico de sensores y selector de equipos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "En ventana Dispositivos • Norma DS 132 e Historial",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Button(
                        onClick = { onNavigate(ScreenDestination.DEVICES) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTealDark),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Ver Dispositivos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }


        // Quick Report Incident or Drill Action Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_reportar_incidencia_banner"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, StatusDangerRed.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(StatusDangerRed.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = StatusDangerRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GESTIÓN DE INCIDENCIAS Y SIMULACROS",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusDangerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Reportar Evento o Simulacro",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Ingresa la descripción detallada para la brigada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showReportIncidentDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Incidencia", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showSimulationDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Simulacro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Panel Unificado de Conectividad IoT & Enlace Firebase
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_iot_unified_status"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (isDeviceConnected) StatusSafeGreen.copy(alpha = 0.4f) else PrimaryTealDark.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header: Faena & Alerta Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONECTIVIDAD IOT & ENLACE DE TELEMETRÍA",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryTealDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userProfile.empresa.ifBlank { "Minera Los Andes" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Text(
                                text = userProfile.tipoMina.ifBlank { "Mina Subterránea" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (alertData.isAllSafe) StatusSafeGreen.copy(alpha = 0.12f) else StatusDangerRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (alertData.isAllSafe) "SEGURO" else "${alertData.activeAlerts.size} ALERTA(S)",
                                color = if (alertData.isAllSafe) StatusSafeGreen else StatusDangerRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3 Compact Status Badges: ESP32 Sensor | Gateway | Firebase RTDB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge 1: ESP32 Sensor
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDeviceConnected) StatusSafeGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isDeviceConnected) StatusSafeGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isDeviceConnected) StatusSafeGreen else StatusDangerRed, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sensor ESP32",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isDeviceConnected) "En Línea" else "En Espera",
                                    fontSize = 10.sp,
                                    color = if (isDeviceConnected) StatusSafeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Badge 2: Gateway / Modo Directo
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isGatewayConnected) AccentTeal.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isGatewayConnected) AccentTeal.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isGatewayConnected) AccentTeal else Color(0xFF64748B), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gateway / Enlace",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isGatewayConnected) "En Línea" else "Modo Directo",
                                    fontSize = 10.sp,
                                    color = if (isGatewayConnected) AccentTeal else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Badge 3: Firebase RTDB
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryTealDark.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, PrimaryTealDark.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(StatusSafeGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Firebase RTDB",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Conectado",
                                    fontSize = 10.sp,
                                    color = StatusSafeGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isDeviceConnected)
                            "Telemetría en vivo activa desde ESP32 • $deviceLastSeenText"
                        else
                            "Sin señal reciente del sensor (WiFi 5V en espera). Puedes activar 'Simular Telemetría' para pruebas completas en vivo sin hardware.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons in a clean horizontal row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.toggleDeviceSimulation() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDeviceConnected) PrimaryTealDark else Color(0xFF0284C7)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDeviceConnected) "Pausar Simulación" else "Simular Telemetría",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onNavigate(ScreenDestination.ALERT_CONFIG) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Config. Umbrales",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { onNavigate(ScreenDestination.DEVICES) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Detalle Dispositivos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshData() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sincronizar",
                                tint = PrimaryTealDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Active Emergency Simulation Banner (if running)
        if (isEmergencySimulationActive) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_simulacro_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusDangerRed),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🚨 SIMULACRO DE EVACUACIÓN EN CURSO",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = simulationScenarioName.ifBlank { "Simulacro Minero DS 132" },
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val simIncident = reportedIncidents.firstOrNull { it.id.startsWith("sim_") }
                                        ?: ReportedIncident(
                                            id = "sim_${System.currentTimeMillis()}",
                                            tipo = simulationScenarioName.ifBlank { "Simulacro de Evacuación" },
                                            ubicacion = "Sector Subterráneo / General",
                                            gravedad = "Crítica",
                                            descripcion = "Simulacro en curso: $simulationScenarioName",
                                            autorNombre = userProfile.nombre,
                                            autorRol = userProfile.rolDisplay,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    incidentToDescribe = simIncident
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                            ) {
                                Text("📝 Describir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showSimulationDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
                            ) {
                                Text("Ver Protocolo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (userProfile.canAccessAdminPanel) {
                                Button(
                                    onClick = { viewModel.stopEmergencySimulation() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = StatusDangerRed
                                    )
                                ) {
                                    Text("Finalizar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Remote Actuators Control Card (Alarma y Luz)
        item {
            ActuatorControlCard(
                selectedDeviceId = selectedDeviceId,
                devicesList = devicesList,
                isAlarmaActive = isAlarmaActive,
                isLuzActive = isLuzActive,
                isAutoTriggered = isAutoTriggered,
                triggerReason = actuatorTriggerReason,
                gatewayIp = gatewayInfo.ip,
                onOpenChangeGatewayIp = { showChangeGatewayIpDialog = true },
                onSelectDevice = { viewModel.setSelectedDevice(it) },
                onToggleAlarma = { viewModel.toggleAlarma(selectedDeviceId) },
                onToggleLuz = { viewModel.toggleLuz(selectedDeviceId) },
                onOpenSimulationDialog = { showSimulationDialog = true }
            )
        }

        // Section Title: Variables Ambientales en Tiempo Real & Selector de Nodo
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_device_telemetry_selector"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PrimaryTealDark.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
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
                                    .background(PrimaryTealDark.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = PrimaryTealDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DISPOSITIVO DE MONITOREO ACTIVO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryTealDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                val currentDev = devicesList.find { it.deviceId == selectedDeviceId }
                                Text(
                                    text = currentDev?.name ?: "Dispositivo $selectedDeviceId",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        val currentDev = devicesList.find { it.deviceId == selectedDeviceId }
                        val isDevOnline = currentDev?.status == "Online"
                        val badgeColor = if (isDevOnline) StatusSafeGreen else StatusDangerRed
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(badgeColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isDevOnline) "EN LÍNEA" else "DESCONECTADO",
                                    color = badgeColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Elige el nodo para ver sus lecturas y variables ambientales en vivo:",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal list of chips for all registered devices
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(devicesList) { dev ->
                            val isSelected = dev.deviceId == selectedDeviceId
                            val isOnline = dev.status == "Online"
                            val dotColor = if (isOnline) StatusSafeGreen else StatusDangerRed

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PrimaryTealDark else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PrimaryTealDark else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.setSelectedDevice(dev.deviceId) }
                                    .testTag("chip_device_${dev.deviceId}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(if (isSelected) Color.White else dotColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (dev.deviceId == "gateway") {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else PrimaryTealDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Column {
                                        val displayName = when {
                                            dev.deviceId == "gateway" -> "Gateway LoRa/WiFi"
                                            dev.deviceId == "NODO_U-01" -> "NODO_U-01 (Principal)"
                                            else -> dev.deviceId
                                        }
                                        Text(
                                            text = displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        val subtitleText = when {
                                            dev.deviceId == "gateway" -> "IP: ${gatewayInfo.ip}"
                                            dev.lastReadingTime.isNotBlank() -> dev.lastReadingTime
                                            else -> ""
                                        }
                                        if (subtitleText.isNotBlank()) {
                                            Text(
                                                text = subtitleText,
                                                fontSize = 9.sp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Variables Ambientales o Concentrador Gateway
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedDeviceId == "gateway") "CONCENTRADOR GATEWAY DE COMUNICACIONES" else "VARIABLES AMBIENTALES EN VIVO",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryTealDark,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selectedDeviceId == "gateway") "IP: ${gatewayInfo.ip}" else "Nodo: $selectedDeviceId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (selectedDeviceId == "gateway") {
            // Render the dedicated Gateway Detail Card with IP configuration
            item {
                GatewayDetailCard(
                    gatewayInfo = gatewayInfo,
                    isGatewayConnected = isGatewayConnected,
                    gatewayLastSeenText = gatewayLastSeenText,
                    onOpenChangeIpDialog = { showChangeGatewayIpDialog = true },
                    onPingGateway = { viewModel.pingGateway() }
                )
            }
        } else {
            // 4 Live Metric Cards with Navy-Teal Gradient (#0B3550 to #146A82)
            // 1. TEMPERATURA
            item {
                MetricCard(
                    titleUppercase = "TEMPERATURA",
                    value = String.format(Locale.US, "%.1f", liveData.temperatura),
                    unit = "°C",
                    icon = Icons.Default.DeviceThermostat,
                    trend = liveData.tempTrend,
                    lastUpdate = liveData.formattedTime,
                    testTag = "metric_temperatura"
                )
            }

            // 2. HUMEDAD
            item {
                MetricCard(
                    titleUppercase = "HUMEDAD",
                    value = String.format(Locale.US, "%.1f", liveData.humedad),
                    unit = "%",
                    icon = Icons.Default.Opacity,
                    trend = liveData.humTrend,
                    lastUpdate = liveData.formattedTime,
                    testTag = "metric_humedad"
                )
            }

            // 3. MQ135 (GASES)
            item {
                MetricCard(
                    titleUppercase = "MQ135 (GASES)",
                    value = String.format(Locale.US, "%.0f", liveData.mq135),
                    unit = "ppm",
                    icon = Icons.Default.Air,
                    trend = liveData.mqTrend,
                    lastUpdate = liveData.formattedTime,
                    testTag = "metric_mq135"
                )
            }

            // 4. PM2.5
            item {
                MetricCard(
                    titleUppercase = "PM2.5",
                    value = String.format(Locale.US, "%.1f", liveData.pm25),
                    unit = "µg/m³",
                    icon = Icons.Default.Sensors,
                    trend = liveData.pmTrend,
                    lastUpdate = liveData.formattedTime,
                    testTag = "metric_pm25"
                )
            }
        }

        // Section Title: Resumen Operativo (Tarjetas con borde de 4dp según reglas)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ESTADO DEL SISTEMA MINERO",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryTealDark,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Border card 1: Blue border (#2563EB)
        item {
            BorderCard(
                borderColor = BorderAccentBlue,
                title = "Flujo de Ventilación y Oxigenación",
                subtitle = "Monitoreo continuo activo. Nivel de CO2 en device_38A839E81F84: ${alertData.co2.toInt()} ppm (Umbral: 1000 ppm).",
                testTag = "border_card_ventilation"
            )
        }

        // Border card 2: Orange border (#F97316)
        item {
            BorderCard(
                borderColor = BorderAccentOrange,
                title = "Control de Polvo Respirable (PM10 y PM2.5)",
                subtitle = "Polvo PM10: ${alertData.pm10.toInt()} µg/m³ (Límite: 100) | PM2.5: ${alertData.pm25.toInt()} µg/m³ (Límite: 50).",
                testTag = "border_card_dust"
            )
        }

        // Border card 3: Green border (#16A34A)
        item {
            BorderCard(
                borderColor = BorderAccentGreen,
                title = "Integración IoT en Tiempo Real",
                subtitle = "Sincronizado con proyecto Firebase 'minefase-fc5f5'. Compatible con dashboard web y sensores ESP32.",
                testTag = "border_card_sync"
            )
        }

        // Quick Navigation Actions
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onNavigate(ScreenDestination.ALERTS) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTealDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Alertas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onNavigate(ScreenDestination.CHARTS) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gráficos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onNavigate(ScreenDestination.MAP) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mapa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
