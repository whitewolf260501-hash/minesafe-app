package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import com.example.model.AlertSeverity
import com.example.model.SafetyAlert
import com.example.model.ReportedIncident
import com.example.ui.MainViewModel
import com.example.ui.components.ActuatorControlCard
import com.example.ui.components.AlertActiveCard
import com.example.ui.components.AllSafeBanner
import com.example.ui.components.DescribeIncidentDialog
import com.example.ui.components.EmergencySimulationCenterCard
import com.example.ui.components.ReportIncidentDialog
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.navigation.ScreenDestination
import java.util.Locale

@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    val alertReading by viewModel.alertDeviceData.collectAsState()
    val activeAlerts = alertReading.activeAlerts
    val devicesList by viewModel.devicesList.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()
    val isAlarmaActive by viewModel.isAlarmaActive.collectAsState()
    val isLuzActive by viewModel.isLuzActive.collectAsState()
    val actuatorTriggerReason by viewModel.actuatorTriggerReason.collectAsState()
    val currentUserProfile by viewModel.userProfile.collectAsState()
    val isSimulationActive by viewModel.isEmergencySimulationActive.collectAsState()
    val activeScenarioName by viewModel.simulationScenarioName.collectAsState()
    val reportedIncidents by viewModel.reportedIncidents.collectAsState()
    val isAutoTriggered = !alertReading.isAllSafe || isSimulationActive

    var showReportIncidentDialog by remember { mutableStateOf(false) }
    var incidentToDescribe by remember { mutableStateOf<ReportedIncident?>(null) }
    var alertToDescribe by remember { mutableStateOf<SafetyAlert?>(null) }

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

    if (alertToDescribe != null) {
        val alert = alertToDescribe!!
        DescribeIncidentDialog(
            itemTitle = alert.title,
            itemLocation = "Faena Minera - Sensor Fase 1 (${alert.metricName})",
            severity = if (alert.severity == AlertSeverity.DANGER) "Emergencia" else "Media",
            initialDescription = alert.description,
            initialStatus = "Activa",
            authorName = currentUserProfile.nombre.ifBlank { "Personal de Turno" },
            formattedDate = "Ahora",
            onDismiss = { alertToDescribe = null },
            onSave = { newDesc, newStatus ->
                viewModel.describeSafetyAlert(alert, newDesc)
                alertToDescribe = null
            }
        )
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_alerts"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Monitor Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    .size(36.dp)
                                    .background(PrimaryTealDark.copy(alpha = 0.12f), CircleShape),
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
                                    text = "PANEL DE CONTROL DE RIESGOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryTealDark,
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = alertReading.deviceId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (alertReading.isAllSafe) StatusSafeGreen.copy(alpha = 0.15f) else StatusDangerRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (alertReading.isAllSafe) "ATMÓSFERA SEGURA" else "ALERTA ACTIVA",
                                color = if (alertReading.isAllSafe) StatusSafeGreen else StatusDangerRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Evaluación automática continua según los estándares internacionales y límites de ventilación configurados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.navigateTo(ScreenDestination.ALERT_CONFIG) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTealDark),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_ir_configurar_umbrales")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Configurar Umbrales", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Button to Report Incident / Emergency with description
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_reportar_incidencia_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, StatusDangerRed.copy(alpha = 0.4f))
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
                                .size(38.dp)
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
                                text = "¿DETECTASTE UN RIESGO O ANOMALÍA?",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusDangerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Reportar Incidencia en Faena",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "Describe lo sucedido para alertar al turno y brigada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showReportIncidentDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDangerRed),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Reportar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Alerts Section
        item {
            Text(
                text = "ESTADO DE ALERTAS EN TIEMPO REAL",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryTealDark,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        if (alertReading.isAllSafe) {
            item {
                AllSafeBanner()
            }
        } else {
            items(activeAlerts, key = { it.id }) { alert ->
                AlertActiveCard(
                    alert = alert,
                    testTag = "alert_card_${alert.id}",
                    onDescribeAlert = { alertToDescribe = alert }
                )
            }
        }

        // Reported Incidents by Mining Personnel Section
        if (reportedIncidents.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INCIDENCIAS REPORTADAS EN FAENA",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryTealDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${reportedIncidents.size} registro(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(reportedIncidents, key = { it.id }) { incident ->
                ReportedIncidentCard(
                    incident = incident,
                    onSelectToDescribe = { incidentToDescribe = it }
                )
            }
        }

        // Threshold & Parameter Rules Gauges
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "MONITOREO DE UMBRALES NORMATIVOS",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryTealDark,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Gauges for the 5 monitored variables
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // 1. CO (Threshold: 50 ppm)
                    ThresholdGaugeRow(
                        name = "Monóxido de Carbono (CO)",
                        value = alertReading.co,
                        unit = "ppm",
                        threshold = 50.0,
                        maxScale = 100.0,
                        isDanger = alertReading.co > 50
                    )

                    // 2. CO2 (Threshold: 1000 ppm)
                    ThresholdGaugeRow(
                        name = "Dióxido de Carbono (CO2)",
                        value = alertReading.co2,
                        unit = "ppm",
                        threshold = 1000.0,
                        maxScale = 2000.0,
                        isDanger = alertReading.co2 > 1000
                    )

                    // 3. PM10 (Threshold: 100 µg/m³)
                    ThresholdGaugeRow(
                        name = "Polvo Grueso (PM10)",
                        value = alertReading.pm10,
                        unit = "µg/m³",
                        threshold = 100.0,
                        maxScale = 200.0,
                        isDanger = alertReading.pm10 > 100
                    )

                    // 4. PM2.5 (Threshold: 50 µg/m³)
                    ThresholdGaugeRow(
                        name = "Partículas Finas (PM2.5)",
                        value = alertReading.pm25,
                        unit = "µg/m³",
                        threshold = 50.0,
                        maxScale = 100.0,
                        isDanger = alertReading.pm25 > 50
                    )

                    // 5. Humedad (Threshold: 80%)
                    ThresholdGaugeRow(
                        name = "Humedad Relativa",
                        value = alertReading.humedad,
                        unit = "%",
                        threshold = 80.0,
                        maxScale = 100.0,
                        isDanger = alertReading.humedad > 80
                    )
                }
            }
        }

        // Live Actuators Controls for Emergency Protocol
        item {
            ActuatorControlCard(
                selectedDeviceId = selectedDeviceId,
                devicesList = devicesList,
                isAlarmaActive = isAlarmaActive,
                isLuzActive = isLuzActive,
                isAutoTriggered = isAutoTriggered,
                triggerReason = actuatorTriggerReason,
                onSelectDevice = { viewModel.setSelectedDevice(it) },
                onToggleAlarma = { viewModel.toggleAlarma(selectedDeviceId) },
                onToggleLuz = { viewModel.toggleLuz(selectedDeviceId) }
            )
        }

        // Testing / Drill Simulation Controls (Admin & SuperAdmin only)
        item {
            EmergencySimulationCenterCard(
                currentUserProfile = currentUserProfile,
                isSimulationActive = isSimulationActive,
                activeScenarioName = activeScenarioName,
                onStartSimulation = { scenario, triggerActuators, userDescription ->
                    viewModel.startEmergencySimulation(scenario, triggerActuators, userDescription)
                },
                onStopSimulation = {
                    viewModel.stopEmergencySimulation()
                },
                onDescribeSimulation = {
                    val simIncident = reportedIncidents.firstOrNull { it.id.startsWith("sim_") }
                        ?: ReportedIncident(
                            id = "sim_${System.currentTimeMillis()}",
                            tipo = activeScenarioName.ifBlank { "Simulacro de Evacuación" },
                            ubicacion = "Sector Subterráneo / General",
                            gravedad = "Crítica",
                            descripcion = "Simulacro en curso: $activeScenarioName",
                            autorNombre = currentUserProfile.nombre,
                            autorRol = currentUserProfile.rolDisplay,
                            timestamp = System.currentTimeMillis()
                        )
                    incidentToDescribe = simIncident
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReportedIncidentCard(
    incident: ReportedIncident,
    onSelectToDescribe: (ReportedIncident) -> Unit
) {
    val severityColor = when (incident.gravedad) {
        "Baja" -> StatusSafeGreen
        "Media" -> StatusWarningAmber
        "Crítica" -> Color(0xFFF97316)
        else -> StatusDangerRed
    }

    val statusColor = when (incident.estado.lowercase()) {
        "resuelta" -> StatusSafeGreen
        "en atención", "en atencion", "atendida" -> StatusWarningAmber
        else -> StatusDangerRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectToDescribe(incident) }
            .testTag("card_reported_incident_${incident.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Type + Severity Badge + Status Badge
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
                            .size(10.dp)
                            .background(severityColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = incident.tipo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = severityColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = incident.gravedad.uppercase(),
                            color = severityColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = incident.estado.uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata: Ubicación + Autor + Fecha
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = incident.ubicacion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = incident.autorNombre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = incident.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Description Box (Destacado y clickable para describir/editar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clickable { onSelectToDescribe(incident) }
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Descripción del reporte:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = PrimaryTealDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Toca para describir / editar",
                                color = PrimaryTealDark,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (incident.descripcion.isNotBlank()) incident.descripcion else "Sin descripción detallada. Haz clic aquí para describir lo sucedido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (incident.descripcion.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Action Button to Select & Describe
            OutlinedButton(
                onClick = { onSelectToDescribe(incident) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_describe_incident_${incident.id}"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryTealDark
                ),
                border = BorderStroke(1.dp, PrimaryTealDark.copy(alpha = 0.45f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "📝 Describir / Editar Observaciones de esta Alerta",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (incident.alarmaActivada) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusDangerRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Alarma sonora y baliza activadas con este reporte",
                        color = StatusDangerRed,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ThresholdGaugeRow(
    name: String,
    value: Double,
    unit: String,
    threshold: Double,
    maxScale: Double,
    isDanger: Boolean
) {
    val progress = (value / maxScale).toFloat().coerceIn(0f, 1f)
    val color = if (isDanger) StatusDangerRed else StatusSafeGreen

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", value)} / ${threshold.toInt()} $unit",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
