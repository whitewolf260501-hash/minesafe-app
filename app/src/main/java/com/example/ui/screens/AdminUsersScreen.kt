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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.UserProfile
import com.example.ui.MainViewModel
import com.example.ui.components.BorderCard
import com.example.ui.components.EmergencySimulationCenterCard
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.BorderAccentGreen
import com.example.ui.theme.BorderAccentOrange
import com.example.ui.theme.PrimaryTealDark
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(viewModel: MainViewModel) {
    val allUsers by viewModel.allUsersList.collectAsState()
    val currentUserProfile by viewModel.userProfile.collectAsState()
    val isSimulationActive by viewModel.isEmergencySimulationActive.collectAsState()
    val activeScenarioName by viewModel.simulationScenarioName.collectAsState()

    var userToEdit by remember { mutableStateOf<UserProfile?>(null) }
    var selectedRoleToAssign by remember { mutableStateOf("usuario") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val rolesList = listOf("superAdmin", "admin", "usuario")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_admin"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(PrimaryTealDark.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = PrimaryTealDark,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GESTIÓN DE USUARIOS Y ROLES",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryTealDark,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Colección Firestore: users",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Operando como: ${currentUserProfile.rolDisplay}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Emergency Simulation Center for Admins
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
                }
            )
        }

        // Section Summary & Roles Rules
        item {
            Text(
                text = "USUARIOS REGISTRADOS EN EL SISTEMA (${allUsers.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // User list cards using the 4dp left border according to category
        items(allUsers, key = { it.uid }) { user ->
            val borderColor = when (user.rolEfectivo) {
                "superAdmin" -> BorderAccentBlue
                "admin" -> BorderAccentOrange
                else -> BorderAccentGreen
            }

            BorderCard(
                borderColor = borderColor,
                title = user.nombre.ifBlank { "Usuario ${user.uid.take(8)}" },
                subtitle = "${user.email} • ${user.empresa.ifBlank { "Minera Asociada" }}",
                testTag = "user_item_${user.uid}",
                contentSlot = {
                    Column {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Rol efectivo: ${user.rolDisplay}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = borderColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "tipoUsuario: ${user.tipoUsuario} | isAdmin: ${user.isAdmin} | isSuper: ${user.isSuperUser}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    userToEdit = user
                                    selectedRoleToAssign = user.rolEfectivo
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_edit_role_${user.uid}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rol", fontSize = 11.sp)
                            }
                        }
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Role Edit Dialog
    if (userToEdit != null) {
        val editing = userToEdit!!
        AlertDialog(
            onDismissRequest = { userToEdit = null },
            title = {
                Text("Modificar Rol de Usuario", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Usuario: ${editing.nombre.ifBlank { editing.email }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "UID: ${editing.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Selecciona el nuevo rol en Firestore:")

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = when (selectedRoleToAssign) {
                                "superAdmin" -> "superAdmin (Control Total)"
                                "admin" -> "admin (Administrador Mina)"
                                else -> "usuario (Operador Estándar)"
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            rolesList.forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (role) {
                                                "superAdmin" -> "superAdmin (Control Total)"
                                                "admin" -> "admin (Administrador Mina)"
                                                else -> "usuario (Operador Estándar)"
                                            }
                                        )
                                    },
                                    onClick = {
                                        selectedRoleToAssign = role
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserRole(editing.uid, selectedRoleToAssign)
                        userToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTealDark)
                ) {
                    Text("Guardar en Firestore")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
