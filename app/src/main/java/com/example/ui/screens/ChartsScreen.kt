package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistoricalPoint
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.BorderAccentGreen
import com.example.ui.theme.BorderAccentOrange
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusWarningAmber
import java.util.Locale

@Composable
fun ChartsScreen(viewModel: MainViewModel) {
    val history by viewModel.historyList.collectAsState()
    val selectedDevice by viewModel.selectedDeviceForChart.collectAsState()
    val selectedMetric by viewModel.selectedMetricForChart.collectAsState()

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val metrics = listOf(
        Triple("temperatura", "Temperatura (°C)", Icons.Default.DeviceThermostat),
        Triple("humedad", "Humedad (%)", Icons.Default.Opacity),
        Triple("gases", "Gases (ppm)", Icons.Default.Air),
        Triple("pm25", "PM2.5 (µg/m³)", Icons.Default.Sensors)
    )

    val currentPoints = remember(history, selectedMetric) {
        history.map { point ->
            when (selectedMetric) {
                "humedad" -> point.humedad
                "gases" -> point.gases
                "pm25" -> point.pm25
                else -> point.temperatura
            }
        }
    }

    val unit = when (selectedMetric) {
        "humedad" -> "%"
        "gases" -> "ppm"
        "pm25" -> "µg/m³"
        else -> "°C"
    }

    val primaryLineColor = when (selectedMetric) {
        "humedad" -> BorderAccentBlue
        "gases" -> StatusWarningAmber
        "pm25" -> BorderAccentOrange
        else -> AccentTeal
    }

    val minVal = (currentPoints.minOrNull() ?: 0.0)
    val maxVal = (currentPoints.maxOrNull() ?: 100.0)
    val avgVal = if (currentPoints.isNotEmpty()) currentPoints.average() else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_charts"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Selector Row
        item {
            Column {
                Text(
                    text = "SELECCIONAR DISPOSITIVO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDevice == "device_38A839E81F84",
                        onClick = { viewModel.selectChartDevice("device_38A839E81F84") },
                        label = { Text("device_38A839E81F84", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTealDark,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedDevice == "dispositivo1",
                        onClick = { viewModel.selectChartDevice("dispositivo1") },
                        label = { Text("dispositivo1 (ESP32)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTealDark,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Metric Selector Chips
        item {
            Column {
                Text(
                    text = "VARIABLE AMBIENTAL HISTÓRICA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(metrics) { (key, label, icon) ->
                        FilterChip(
                            selected = selectedMetric == key,
                            onClick = {
                                viewModel.selectChartMetric(key)
                                selectedPointIndex = null
                            },
                            leadingIcon = {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryTealDark,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_metric_$key")
                        )
                    }
                }
            }
        }

        // Canvas Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tendencia Temporal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ruta: dispositivos/$selectedDevice/historial",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Selected point value tooltip
                        if (selectedPointIndex != null && selectedPointIndex!! in currentPoints.indices) {
                            val idx = selectedPointIndex!!
                            val pt = history.getOrNull(idx)
                            val v = currentPoints[idx]
                            Box(
                                modifier = Modifier
                                    .background(primaryLineColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${pt?.displayTime ?: ""}: ${String.format(Locale.US, "%.1f", v)} $unit",
                                    color = primaryLineColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Canvas Line Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(currentPoints) {
                                    detectTapGestures { tapOffset ->
                                        if (currentPoints.isNotEmpty()) {
                                            val stepX = size.width / (currentPoints.size - 1).coerceAtLeast(1)
                                            val nearestIdx = (tapOffset.x / stepX).toInt().coerceIn(0, currentPoints.size - 1)
                                            selectedPointIndex = nearestIdx
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val paddingBottom = 20f
                            val paddingTop = 10f
                            val chartH = h - paddingBottom - paddingTop

                            val min = (currentPoints.minOrNull() ?: 0.0).toFloat()
                            val max = ((currentPoints.maxOrNull() ?: 100.0) + 0.1).toFloat()
                            val range = (max - min).coerceAtLeast(1f)

                            // Horizontal Grid lines
                            val gridColor = Color.LightGray.copy(alpha = 0.35f)
                            for (i in 0..4) {
                                val y = paddingTop + (chartH / 4) * i
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            if (currentPoints.size > 1) {
                                val stepX = w / (currentPoints.size - 1)
                                val path = Path()
                                val fillPath = Path()

                                currentPoints.forEachIndexed { i, value ->
                                    val x = i * stepX
                                    val normalizedY = ((value.toFloat() - min) / range).coerceIn(0f, 1f)
                                    val y = paddingTop + (1f - normalizedY) * chartH

                                    if (i == 0) {
                                        path.moveTo(x, y)
                                        fillPath.moveTo(x, h - paddingBottom)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        val prevX = (i - 1) * stepX
                                        val prevNorm = ((currentPoints[i - 1].toFloat() - min) / range).coerceIn(0f, 1f)
                                        val prevY = paddingTop + (1f - prevNorm) * chartH

                                        // Smooth curve
                                        val cx1 = (prevX + x) / 2
                                        path.cubicTo(cx1, prevY, cx1, y, x, y)
                                        fillPath.cubicTo(cx1, prevY, cx1, y, x, y)
                                    }

                                    if (i == currentPoints.size - 1) {
                                        fillPath.lineTo(x, h - paddingBottom)
                                        fillPath.close()
                                    }
                                }

                                // Draw gradient fill below line
                                val fillBrush = Brush.verticalGradient(
                                    colors = listOf(primaryLineColor.copy(alpha = 0.3f), Color.Transparent),
                                    startY = paddingTop,
                                    endY = h - paddingBottom
                                )
                                drawPath(fillPath, brush = fillBrush)

                                // Draw line stroke
                                drawPath(
                                    path = path,
                                    color = primaryLineColor,
                                    style = Stroke(width = 3.dp.toPx())
                                )

                                // Draw points
                                currentPoints.forEachIndexed { i, value ->
                                    val x = i * stepX
                                    val normalizedY = ((value.toFloat() - min) / range).coerceIn(0f, 1f)
                                    val y = paddingTop + (1f - normalizedY) * chartH

                                    val isSelected = selectedPointIndex == i
                                    drawCircle(
                                        color = if (isSelected) Color.White else primaryLineColor,
                                        radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                    if (isSelected) {
                                        drawCircle(
                                            color = primaryLineColor,
                                            radius = 3.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Toca sobre el gráfico para ver el valor exacto en ese punto temporal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Summary Stats (Mínimo, Promedio, Máximo)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "MÍNIMO",
                    value = "${String.format(Locale.US, "%.1f", minVal)} $unit",
                    color = AccentTeal,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "PROMEDIO",
                    value = "${String.format(Locale.US, "%.1f", avgVal)} $unit",
                    color = BorderAccentBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "MÁXIMO",
                    value = "${String.format(Locale.US, "%.1f", maxVal)} $unit",
                    color = BorderAccentOrange,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
