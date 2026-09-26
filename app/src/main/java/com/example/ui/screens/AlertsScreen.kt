package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AlertActiveCard
import com.example.ui.components.AllSafeBanner
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber
import java.util.Locale

@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    val alertReading by viewModel.alertDeviceData.collectAsState()
    val activeAlerts = alertReading.activeAlerts

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

                    Text(
                        text = "Evaluación automática continua según los estándares internacionales de seguridad minera y límites de ventilación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    testTag = "alert_card_${alert.id}"
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

        // Testing / Drill Simulation Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryTealDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simulador de Simulacro de Evacuación",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTealDark
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Permite probar la respuesta de las alarmas sonoras y visuales forzando umbrales de riesgo en tiempo de ejecución.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateAlertTest(true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusDangerRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simular Peligro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.simulateAlertTest(false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Rango Seguro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
