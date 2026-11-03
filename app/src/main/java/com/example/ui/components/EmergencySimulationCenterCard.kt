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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.UserProfile
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber

/**
 * High-fidelity Emergency Simulation & Real Evacuation Protocol Component
 * Matching exact industrial mine control design (DS 132 / DS 024-2016-EM).
 */
@Composable
fun EmergencySimulationCenterCard(
    currentUserProfile: UserProfile,
    isSimulationActive: Boolean,
    activeScenarioName: String,
    onStartSimulation: (scenarioName: String, triggerActuators: Boolean, userDescription: String) -> Unit,
    onStopSimulation: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    onDescribeSimulation: (() -> Unit)? = null
) {
    val canSimulate = currentUserProfile.canAccessAdminPanel
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Simulacro, 1 = Emergencia Real

    // User descriptions for each mode
    var drillUserDescription by remember { mutableStateOf("") }
    var realUserDescription by remember { mutableStateOf("") }

    // Simulacro options (Tab 0)
    var selectedDrillIndex by remember { mutableIntStateOf(0) }
    val drillOptions = listOf(
        DrillOption(
            title = "Simulacro de Fuga de Monóxido de Carbono (CO)",
            badgeText = "MQ-2 > 1200 ADC",
            badgeColor = Color(0xFFFBBF24),
            badgeBg = Color(0xFF451A03),
            description = "Fuerza niveles de gas tóxico sobre el límite crítico (DS 132). Activa sirena continua y alerta roja en consola.",
            icon = Icons.Default.LocalFireDepartment,
            iconTint = Color(0xFFF59E0B),
            iconBg = Color(0xFF422006),
            selectedBorder = Color(0xFFF97316),
            selectedBg = Color(0xFF23150C)
        ),
        DrillOption(
            title = "Simulacro de Incendio / Densidad de Humo y Polvo",
            badgeText = "PM2.5 > 500 µg/m³",
            badgeColor = Color(0xFFF87171),
            badgeBg = Color(0xFF450A0A),
            description = "Simula principio de incendio en socavón con saturación extrema de material particulado y baliza estroboscópica.",
            icon = Icons.Default.Air,
            iconTint = Color(0xFFEF4444),
            iconBg = Color(0xFF3F0E14),
            selectedBorder = Color(0xFFDC2626),
            selectedBg = Color(0xFF220B10)
        ),
        DrillOption(
            title = "Simulacro de Evacuación General Inmediata",
            badgeText = "Sirena + Baliza ESP32",
            badgeColor = Color(0xFFC084FC),
            badgeBg = Color(0xFF3B0764),
            description = "Dispara sirena de evacuación forzada en los detectores portátiles y commuta extractores al 100%.",
            icon = Icons.Default.VolumeUp,
            iconTint = Color(0xFFA855F7),
            iconBg = Color(0xFF2E1065),
            selectedBorder = Color(0xFF9333EA),
            selectedBg = Color(0xFF1D0B2E)
        )
    )

    // Real Emergency options (Tab 1)
    var selectedRealScopeIndex by remember { mutableIntStateOf(0) }
    val realEmergencyScopes = listOf(
        RealScopeOption(
            title = "Evacuación Total de Faena Subterránea",
            subtitle = "Todos los niveles hacia superficie y refugios mineros herméticos.",
            dotColor = Color(0xFFEF4444),
            selectedBorder = Color(0xFFDC2626),
            selectedBg = Color(0xFF1C0B12)
        ),
        RealScopeOption(
            title = "Repliegue Sectorial Galería 4-Sur",
            subtitle = "Aislamiento por emanación de gas CO/CO2 y confinamiento.",
            dotColor = Color(0xFFF59E0B),
            selectedBorder = Color(0xFFD97706),
            selectedBg = Color(0xFF1E1208)
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sim_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_emergency_simulation_center"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1320)),
        border = BorderStroke(
            width = if (isSimulationActive) 2.dp else 1.dp,
            color = if (isSimulationActive) StatusDangerRed else Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. TOP HEADER (Icon + Titles + DS 132 Badge + Close X)
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
                            .size(42.dp)
                            .background(
                                color = if (selectedTab == 0) Color(0xFF2D1808) else Color(0xFF3D0B12),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Security else Icons.Default.Warning,
                            contentDescription = "Tipo Alerta",
                            tint = if (selectedTab == 0) Color(0xFFF59E0B) else Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (selectedTab == 0) "Simulacro de Emergencia Minera" else "Protocolo de Evacuación Real",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (selectedTab == 0) Color(0xFF8B3A00) else Color(0xFF991B1B),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DS 132",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (selectedTab == 0)
                                "Entrenamiento de seguridad y prueba de respuesta de sensores"
                            else
                                "Activación de contingencia operacional de faena",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. SEGMENTED TAB SELECTOR: [Simulacro (Admin / Prevención)] vs [Emergencia Real de Faena]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070D18), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Simulacro
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedTab == 0) Color(0xFFEA580C) else Color.Transparent
                        )
                        .clickable { selectedTab = 0 }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedTab == 0) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFA855F7), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simulacro (Admin / Prevención)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Tab 1: Emergencia Real de Faena
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedTab == 1) Color(0xFFDC2626) else Color.Transparent
                        )
                        .clickable { selectedTab = 1 }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Emergencia Real de Faena",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. ACTIVE SIMULATION STATE BANNER
            if (isSimulationActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_simulacro_activo"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusDangerRed),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .alpha(pulseAlpha),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🚨 SIMULACRO / EVACUACIÓN EN CURSO",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Escenario: ${activeScenarioName.ifBlank { "Simulacro General de Evacuación" }}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Sirena acústica continua y vibración activas en el dispositivo y nodo IoT.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (onDescribeSimulation != null) {
                            OutlinedButton(
                                onClick = onDescribeSimulation,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("btn_describir_simulacro_activo"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.9f))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "📝 Describir / Agregar Notas al Simulacro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = onStopSimulation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_finalizar_simulacro"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = StatusDangerRed
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FINALIZAR SIMULACRO / CONDICIONES SEGURAS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else if (!canSimulate) {
                // RESTRICTED ROLE NOTICE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = StatusWarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Función exclusiva para Administradores de Faena",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Solo usuarios con rol de Administrador o Super Admin pueden autorizar y disparar simulacros de evacuación en interior mina.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                // 4. MAIN FORM BODY BASED ON SELECTED TAB
                if (selectedTab == 0) {
                    // TAB 0: SIMULACRO
                    // Authorisation Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF101726), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Mando",
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Autorización de Mando: ${currentUserProfile.nombre.ifBlank { "Manuel Crisostomo" }}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Esta función permite a SuperAdmins y Administradores de Faena simular incidentes críticos para verificar tiempos de evacuación, sirenas del ESP32 y conmutación de mangas de ventilación.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Green badge
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF042316), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF065F46), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Actuadores Físicos: Se activan de forma automática la ALARMA (Buzzer) y la LUZ (Baliza LED) en el ESP32.",
                                        color = Color(0xFF34D399),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SELECCIONE TIPO DE INCIDENTE PARA EL SIMULACRO:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3 Drill Cards
                    drillOptions.forEachIndexed { index, option ->
                        val isSelected = selectedDrillIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) option.selectedBg else Color(0xFF0F172A))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) option.selectedBorder else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedDrillIndex = index }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(option.iconBg, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = option.iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option.title,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(option.badgeBg, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = option.badgeText,
                                                color = option.badgeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = option.description,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                        if (index < drillOptions.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "DESCRIPCIÓN U OBSERVACIONES DEL SIMULACRO:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = drillUserDescription,
                        onValueChange = { drillUserDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_simulacro_descripcion"),
                        placeholder = {
                            Text(
                                text = "Describe el objetivo del simulacro, cuadrilla participante, sector o notas del ejercicio...",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA580C),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0B1324),
                            unfocusedContainerColor = Color(0xFF0B1324)
                        ),
                        minLines = 3,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons: Cancelar & Iniciar Simulacro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDismiss?.invoke() },
                            modifier = Modifier
                                .weight(0.4f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFFE2E8F0)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val selected = drillOptions[selectedDrillIndex]
                                onStartSimulation(selected.title, true, drillUserDescription.trim())
                            },
                            modifier = Modifier
                                .weight(0.6f)
                                .height(44.dp)
                                .testTag("btn_iniciar_simulacro"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEA580C),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "INICIAR SIMULACRO CONTROLADO",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                } else {
                    // TAB 1: EMERGENCIA REAL DE FAENA
                    // Warning Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF200B14), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF7F1D1D), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Emergencia Real",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ADVERTENCIA OPERACIONAL — EMERGENCIA REAL",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF87171),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Al confirmar esta acción se emitirá una alerta sonora de evacuación prioritaria en las radios de cuadrilla, se conmutarán los extractores de ventilación a régimen forzado y se notificará al Centro de Despacho de Rescate.",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ALCANCE DEL PROTOCOLO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real Scopes Options
                    realEmergencyScopes.forEachIndexed { index, option ->
                        val isSelected = selectedRealScopeIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) option.selectedBg else Color(0xFF0F172A))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) option.selectedBorder else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedRealScopeIndex = index }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = option.subtitle,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(option.dotColor, CircleShape)
                                )
                            }
                        }
                        if (index < realEmergencyScopes.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "DESCRIPCIÓN DEL INCIDENTE O CONDICIÓN DE EMERGENCIA:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = realUserDescription,
                        onValueChange = { realUserDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_emergencia_descripcion"),
                        placeholder = {
                            Text(
                                text = "Describe la situación en mina: fuga observada, personas en el sector, orden de repliegue, medidas tomadas...",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = Color(0xFF7F1D1D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E0B12),
                            unfocusedContainerColor = Color(0xFF1E0B12)
                        ),
                        minLines = 3,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons: Cancelar & Confirmar Evacuación Real
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDismiss?.invoke() },
                            modifier = Modifier
                                .weight(0.35f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFFE2E8F0)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val selected = realEmergencyScopes[selectedRealScopeIndex]
                                onStartSimulation("EMERGENCIA REAL: ${selected.title}", true, realUserDescription.trim())
                            },
                            modifier = Modifier
                                .weight(0.65f)
                                .height(44.dp)
                                .testTag("btn_activar_evacuacion_real"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONFIRMAR Y ACTIVAR EVACUACIÓN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen / Pop-up Dialog for Emergency Simulation & Real Protocol.
 */
@Composable
fun EmergencySimulationDialog(
    currentUserProfile: UserProfile,
    isSimulationActive: Boolean,
    activeScenarioName: String,
    onStartSimulation: (scenarioName: String, triggerActuators: Boolean, userDescription: String) -> Unit,
    onStopSimulation: () -> Unit,
    onDismiss: () -> Unit,
    onDescribeSimulation: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            EmergencySimulationCenterCard(
                currentUserProfile = currentUserProfile,
                isSimulationActive = isSimulationActive,
                activeScenarioName = activeScenarioName,
                onStartSimulation = { scenario, triggerActuators, userDescription ->
                    onStartSimulation(scenario, triggerActuators, userDescription)
                    onDismiss()
                },
                onStopSimulation = {
                    onStopSimulation()
                    onDismiss()
                },
                onDismiss = onDismiss,
                onDescribeSimulation = {
                    onDismiss()
                    onDescribeSimulation?.invoke()
                }
            )
        }
    }
}

private data class DrillOption(
    val title: String,
    val badgeText: String,
    val badgeColor: Color,
    val badgeBg: Color,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val selectedBorder: Color,
    val selectedBg: Color
)

private data class RealScopeOption(
    val title: String,
    val subtitle: String,
    val dotColor: Color,
    val selectedBorder: Color,
    val selectedBg: Color
)
