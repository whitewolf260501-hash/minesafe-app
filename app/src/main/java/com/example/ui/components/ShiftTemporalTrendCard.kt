package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.BorderAccentOrange
import com.example.ui.theme.BorderAccentPurple
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber

enum class ShiftFilterGroup(val label: String) {
    CO_PM25("CO & PM2.5"),
    TEMP_HUM("Temp & Humedad"),
    CO2_CH4_RUIDO("CO2, Metano & Ruido"),
    TODOS("Todos (Comparativa Turno)")
}

data class ShiftTimePoint(
    val timeLabel: String, // "00:00", "04:00", "08:00", "12:00", "16:00", "20:00"
    val co: Double,
    val pm25: Double,
    val temp: Double,
    val hum: Double,
    val co2: Double,
    val ch4: Double,
    val ruido: Double
)

@Composable
fun ShiftTemporalTrendCard(
    modifier: Modifier = Modifier
) {
    var selectedGroup by remember { mutableStateOf(ShiftFilterGroup.CO_PM25) }

    val shiftData = remember {
        listOf(
            ShiftTimePoint("00:00", co = 12.0, pm25 = 14.0, temp = 21.0, hum = 64.0, co2 = 520.0, ch4 = 0.1, ruido = 60.0),
            ShiftTimePoint("04:00", co = 14.0, pm25 = 18.0, temp = 22.0, hum = 62.0, co2 = 560.0, ch4 = 0.2, ruido = 62.0),
            ShiftTimePoint("08:00", co = 28.0, pm25 = 35.0, temp = 24.2, hum = 58.0, co2 = 680.0, ch4 = 0.4, ruido = 78.0),
            ShiftTimePoint("12:00", co = 42.0, pm25 = 48.0, temp = 25.8, hum = 54.0, co2 = 820.0, ch4 = 0.6, ruido = 84.0),
            ShiftTimePoint("16:00", co = 36.0, pm25 = 38.0, temp = 24.5, hum = 56.0, co2 = 740.0, ch4 = 0.4, ruido = 76.0),
            ShiftTimePoint("20:00", co = 18.0, pm25 = 22.0, temp = 23.0, hum = 60.0, co2 = 610.0, ch4 = 0.2, ruido = 66.0)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_shift_temporal_trend"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrimaryTealDark.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AccentTeal.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TENDENCIA TEMPORAL DEL TURNO",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Historial Turno Minero (00:00 a 20:00)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Turno A", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs for Sensor Groups (CO & PM2.5 | Temp & Humedad | CO2, Metano & Ruido | Todos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShiftFilterGroup.values().forEach { group ->
                    val isSelected = selectedGroup == group
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGroup = group },
                        label = { Text(group.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                            selectedLabelColor = AccentTeal
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("filter_shift_${group.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shift Trend Visualization Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                shiftData.forEach { pt ->
                    ShiftHourColumn(
                        point = pt,
                        group = selectedGroup
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Group Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (selectedGroup) {
                    ShiftFilterGroup.CO_PM25 -> {
                        LegendBullet(color = BorderAccentOrange, label = "CO (ppm)")
                        LegendBullet(color = BorderAccentPurple, label = "PM2.5 (µg/m³)")
                        Text("Límite CO: 50 | PM2.5: 50", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ShiftFilterGroup.TEMP_HUM -> {
                        LegendBullet(color = StatusDangerRed, label = "Temperatura (°C)")
                        LegendBullet(color = BorderAccentBlue, label = "Humedad (%)")
                        Text("Límite Temp: 28°C", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ShiftFilterGroup.CO2_CH4_RUIDO -> {
                        LegendBullet(color = AccentTeal, label = "CO2 (/10 ppm)")
                        LegendBullet(color = StatusWarningAmber, label = "Metano x10 (%)")
                        LegendBullet(color = Color(0xFFE879F9), label = "Ruido (dB)")
                    }
                    ShiftFilterGroup.TODOS -> {
                        LegendBullet(color = BorderAccentOrange, label = "CO")
                        LegendBullet(color = BorderAccentPurple, label = "PM2.5")
                        LegendBullet(color = StatusSafeGreen, label = "Temp")
                        LegendBullet(color = AccentTeal, label = "CO2")
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftHourColumn(
    point: ShiftTimePoint,
    group: ShiftFilterGroup
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(44.dp)
    ) {
        // Multi-bar grouping for the selected variables
        Row(
            modifier = Modifier
                .height(115.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            when (group) {
                ShiftFilterGroup.CO_PM25 -> {
                    MiniTrendBar(fraction = (point.co / 60.0).toFloat(), color = BorderAccentOrange)
                    MiniTrendBar(fraction = (point.pm25 / 60.0).toFloat(), color = BorderAccentPurple)
                }
                ShiftFilterGroup.TEMP_HUM -> {
                    MiniTrendBar(fraction = (point.temp / 35.0).toFloat(), color = StatusDangerRed)
                    MiniTrendBar(fraction = (point.hum / 100.0).toFloat(), color = BorderAccentBlue)
                }
                ShiftFilterGroup.CO2_CH4_RUIDO -> {
                    MiniTrendBar(fraction = (point.co2 / 1200.0).toFloat(), color = AccentTeal)
                    MiniTrendBar(fraction = (point.ch4 / 2.0).toFloat(), color = StatusWarningAmber)
                    MiniTrendBar(fraction = (point.ruido / 100.0).toFloat(), color = Color(0xFFE879F9))
                }
                ShiftFilterGroup.TODOS -> {
                    MiniTrendBar(fraction = (point.co / 60.0).toFloat(), color = BorderAccentOrange)
                    MiniTrendBar(fraction = (point.pm25 / 60.0).toFloat(), color = BorderAccentPurple)
                    MiniTrendBar(fraction = (point.temp / 35.0).toFloat(), color = StatusSafeGreen)
                    MiniTrendBar(fraction = (point.co2 / 1200.0).toFloat(), color = AccentTeal)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = point.timeLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MiniTrendBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(8.dp)
            .fillMaxHeight(fraction.coerceIn(0.08f, 1f))
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .background(color)
    )
}

@Composable
fun LegendBullet(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
