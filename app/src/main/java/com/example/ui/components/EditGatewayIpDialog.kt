package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditGatewayIpDialog(
    currentIp: String,
    currentPort: Int,
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (newIp: String, newPort: Int) -> Unit,
    onElevateRoleToAdmin: () -> Unit = {}
) {
    var ipInput by remember { mutableStateOf(currentIp) }
    var portInput by remember { mutableStateOf(currentPort.toString()) }

    val canEdit = userProfile.canAccessAdminPanel

    val isIpValid by remember(ipInput) {
        derivedStateOf {
            val trimmed = ipInput.trim()
            val parts = trimmed.split(".")
            parts.size == 4 && parts.all {
                val num = it.toIntOrNull()
                num != null && num in 0..255
            }
        }
    }

    val isPortValid by remember(portInput) {
        derivedStateOf {
            val p = portInput.toIntOrNull()
            p != null && p in 1..65535
        }
    }

    val isFormValid = isIpValid && isPortValid && canEdit

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_edit_gateway_ip"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lan,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Configuración IP Gateway",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Concentrador Bocamina LoRa/WiFi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Role Verification Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (canEdit) StatusSafeGreen.copy(alpha = 0.1f) else StatusDangerRed.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (canEdit) StatusSafeGreen.copy(alpha = 0.35f) else StatusDangerRed.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (canEdit) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (canEdit) StatusSafeGreen else StatusDangerRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (canEdit) "Rol Autorizado: ${userProfile.rolDisplay}" else "Acceso Restringido (Solo Admin)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (canEdit) StatusSafeGreen else StatusDangerRed
                            )
                            Text(
                                text = if (canEdit)
                                    "Permiso concedido para reconfigurar parámetros de red en Firebase."
                                else
                                    "Tu usuario (${userProfile.email}) no tiene permisos para guardar cambios.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!canEdit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onElevateRoleToAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_elevate_admin_demo"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Activar Modo Super Administrador", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // IPv4 Input
                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("Dirección IPv4 del Gateway") },
                    placeholder = { Text("192.168.1.100") },
                    leadingIcon = {
                        Icon(Icons.Default.Lan, contentDescription = null, tint = AccentTeal)
                    },
                    isError = ipInput.isNotEmpty() && !isIpValid,
                    supportingText = {
                        if (ipInput.isNotEmpty() && !isIpValid) {
                            Text("Formato IPv4 inválido (ej: 192.168.1.100)", color = StatusDangerRed)
                        } else {
                            Text("Dirección IP asignada al concentrador en la mina")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_gateway_ip"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Port Input
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    label = { Text("Puerto de Servicio") },
                    placeholder = { Text("8080") },
                    leadingIcon = {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = AccentTeal)
                    },
                    isError = portInput.isNotEmpty() && !isPortValid,
                    supportingText = {
                        if (portInput.isNotEmpty() && !isPortValid) {
                            Text("Puerto debe estar entre 1 y 65535", color = StatusDangerRed)
                        } else {
                            Text("Puerto estándar: 8080 (REST), 1883 (MQTT)")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isFormValid) {
                                onSave(ipInput.trim(), portInput.toIntOrNull() ?: 8080)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_gateway_port"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick subnet presets
                Text(
                    text = "Direcciones sugeridas por subestación minera:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val presets = listOf(
                    "192.168.1.100" to "Bocamina Principal",
                    "192.168.0.50" to "Galería Subterránea",
                    "10.0.0.10" to "Troncal LoRa",
                    "172.16.1.1" to "VLAN Refugio"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (ip, label) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (ipInput == ip) AccentTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (ipInput == ip) AccentTeal else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.clickable { ipInput = ip }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ip,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ipInput == ip) AccentTeal else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($label)",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        onSave(ipInput.trim(), portInput.toIntOrNull() ?: 8080)
                    }
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTeal,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("btn_save_gateway_ip")
            ) {
                Text("Guardar IP", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_gateway_ip")
            ) {
                Text("Cancelar")
            }
        }
    )
}
