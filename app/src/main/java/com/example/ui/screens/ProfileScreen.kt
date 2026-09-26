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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.BorderAccentGreen
import com.example.ui.theme.BorderAccentOrange
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var nombre by remember(userProfile.nombre) { mutableStateOf(userProfile.nombre) }
    var empresa by remember(userProfile.empresa) { mutableStateOf(userProfile.empresa) }
    var tipoMina by remember(userProfile.tipoMina) { mutableStateOf(userProfile.tipoMina) }
    var latitudStr by remember(userProfile.latitud) { mutableStateOf(userProfile.latitud.toString()) }
    var longitudStr by remember(userProfile.longitud) { mutableStateOf(userProfile.longitud.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_profile"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Card Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(PrimaryTealDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = AccentTeal,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userProfile.nombre.ifBlank { "Usuario MineSafe" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = userProfile.email.ifBlank { "usuario@minesafe.pe" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Effective Role Badge
                    val roleBadgeColor = when (userProfile.rolEfectivo) {
                        "superAdmin" -> BorderAccentBlue
                        "admin" -> BorderAccentOrange
                        else -> BorderAccentGreen
                    }

                    Box(
                        modifier = Modifier
                            .background(roleBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ROL: ${userProfile.rolDisplay.uppercase(Locale.getDefault())}",
                            color = roleBadgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Extended Profile Details Form
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
                        Text(
                            text = "DATOS EXTENDIDOS (usuarios/{uid})",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryTealDark,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        if (!isEditing) {
                            OutlinedButton(
                                onClick = { isEditing = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_edit_profile")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Editar", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isEditing) {
                        // Editable Fields
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre Completo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = empresa,
                            onValueChange = { empresa = it },
                            label = { Text("Empresa Minera") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = tipoMina,
                            onValueChange = { tipoMina = it },
                            label = { Text("Tipo de Mina") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = latitudStr,
                                onValueChange = { latitudStr = it },
                                label = { Text("Latitud") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = longitudStr,
                                onValueChange = { longitudStr = it },
                                label = { Text("Longitud") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val lat = latitudStr.toDoubleOrNull() ?: userProfile.latitud
                                    val lng = longitudStr.toDoubleOrNull() ?: userProfile.longitud
                                    viewModel.updateUserProfile(nombre, empresa, tipoMina, lat, lng)
                                    isEditing = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTealDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar Cambios")
                            }

                            OutlinedButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar")
                            }
                        }
                    } else {
                        // Readonly summary
                        ProfileDetailRow(icon = Icons.Default.Person, title = "Nombre", value = userProfile.nombre.ifBlank { "Sin especificar" })
                        Spacer(modifier = Modifier.height(10.dp))
                        ProfileDetailRow(icon = Icons.Default.Business, title = "Empresa", value = userProfile.empresa.ifBlank { "Sin especificar" })
                        Spacer(modifier = Modifier.height(10.dp))
                        ProfileDetailRow(icon = Icons.Default.Terrain, title = "Tipo de Mina", value = userProfile.tipoMina.ifBlank { "Mina Subterránea" })
                        Spacer(modifier = Modifier.height(10.dp))
                        ProfileDetailRow(
                            icon = Icons.Default.LocationOn,
                            title = "Geolocalización",
                            value = "${String.format(Locale.US, "%.5f", userProfile.latitud)}, ${String.format(Locale.US, "%.5f", userProfile.longitud)}"
                        )
                    }
                }
            }
        }

        // Privileges and Permissions Matrix
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PrimaryTealDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Control de Acceso y Privilegios (RBAC)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTealDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Regla de cálculo de rol en Firestore (users/{uid}):\n• isSuperUser == true o tipoUsuario == 'superAdmin' -> superAdmin\n• isAdmin == true o tipoUsuario == 'admin' -> admin\n• De lo contrario -> usuario (operador estándar)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Permissions table
                    PermissionRow(
                        title = "Lectura de sensores ESP32 y gases",
                        allowed = true,
                        roleNote = "Todos los roles"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PermissionRow(
                        title = "Recepción de alertas de seguridad",
                        allowed = true,
                        roleNote = "Todos los roles"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PermissionRow(
                        title = "Acceso a historial y plano topográfico",
                        allowed = true,
                        roleNote = "Todos los roles"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PermissionRow(
                        title = "Gestión de Usuarios y Roles en Firestore",
                        allowed = userProfile.canAccessAdminPanel,
                        roleNote = if (userProfile.canAccessAdminPanel) "Habilitado (Admin)" else "Bloqueado (Solo Admin)"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CAMBIAR ROL PARA COMPROBAR SEPARACIÓN:",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryTealDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.enterAsRole("usuario") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Operador", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.enterAsRole("admin") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.enterAsRole("superAdmin") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SuperAdmin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Firebase Backend Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = PrimaryTealDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conexión Firebase Backend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTealDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Project ID: minefase-fc5f5", style = MaterialTheme.typography.bodySmall)
                    Text("• Realtime Database: https://minefase-fc5f5-default-rtdb.firebaseio.com", style = MaterialTheme.typography.bodySmall)
                    Text("• Firestore Collection: users", style = MaterialTheme.typography.bodySmall)
                    Text("• Sincronización activa con web y sensores IoT", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Logout Button
        item {
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_logout"),
                colors = ButtonDefaults.buttonColors(containerColor = StatusDangerRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CERRAR SESIÓN", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    allowed: Boolean,
    roleNote: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = roleNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .background(
                    if (allowed) StatusSafeGreen.copy(alpha = 0.15f) else StatusDangerRed.copy(alpha = 0.15f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (allowed) "HABILITADO" else "BLOQUEADO",
                color = if (allowed) StatusSafeGreen else StatusDangerRed,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}
