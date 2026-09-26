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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.components.BorderCard
import com.example.ui.components.MetricCard
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(StatusSafeGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FIREBASE RTDB: minefase-fc5f5",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSafeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SENSOR ESP32 EN VIVO (dispositivo1)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userProfile.empresa.ifBlank { "Minera Los Andes S.A.C." },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userProfile.tipoMina.ifBlank { "Mina Subterránea - Nivel Galería Principal" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (alertData.isAllSafe) StatusSafeGreen.copy(alpha = 0.12f) else StatusDangerRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (alertData.isAllSafe) "SEGURO" else "${alertData.activeAlerts.size} ALERTA(S)",
                            color = if (alertData.isAllSafe) StatusSafeGreen else StatusDangerRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section Title: Variables Ambientales en Tiempo Real
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VARIABLES AMBIENTALES EN VIVO",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryTealDark,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "RTDB Live Listener",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
