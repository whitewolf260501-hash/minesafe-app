package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusSafeGreen

@Composable
fun UsersScrollableBoxCard(
    usersList: List<UserProfile>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("TODOS") } // "TODOS", "SUPERADMIN", "ADMIN", "OPERADOR"

    // Default sample list if usersList from RTDB is empty or single user
    val completeUsersList = remember(usersList) {
        if (usersList.size >= 3) usersList
        else {
            val base = usersList.toMutableList()
            if (base.none { it.rolEfectivo == "superAdmin" }) {
                base.add(
                    UserProfile(
                        uid = "usr_super_01",
                        nombre = "Manuel Crisostomo",
                        email = "manuelcrisostomovega@gmail.com",
                        tipoUsuario = "superAdmin",
                        isSuperUser = true,
                        empresa = "Minera Chuquicamata",
                        rut = "16.845.920-K",
                        cargo = "Superintendente General de Faena",
                        sectorTurno = "Turno Continuo • Centro de Control Bocamina",
                        isOnline = true
                    )
                )
            }
            if (base.none { it.rolEfectivo == "admin" }) {
                base.add(
                    UserProfile(
                        uid = "usr_admin_02",
                        nombre = "Pepe Lota",
                        email = "white.wolf260501@gmail.com",
                        tipoUsuario = "admin",
                        isAdmin = true,
                        empresa = "Minera Los Andes",
                        rut = "18.320.145-2",
                        cargo = "Jefe de Prevención y Seguridad DS 132",
                        sectorTurno = "Turno A (Día) • Frente de Avance & Rampa",
                        isOnline = true
                    )
                )
            }
            if (base.none { it.rolEfectivo == "usuario" }) {
                base.add(
                    UserProfile(
                        uid = "usr_op_03",
                        nombre = "Gerardo Cuellar Velasquez",
                        email = "geencuve@gmail.com",
                        tipoUsuario = "usuario",
                        empresa = "Mina San José",
                        rut = "19.782.604-5",
                        cargo = "Operador de Sensores y Ventiladores",
                        sectorTurno = "Turno B (Rotativo) • Galería 1 Nivel -250m",
                        isOnline = true
                    )
                )
            }
            base.add(
                UserProfile(
                    uid = "usr_op_04",
                    nombre = "Carlos Mendoza Soto",
                    email = "c.mendoza.mina@gmail.com",
                    tipoUsuario = "usuario",
                    empresa = "Minera Los Andes",
                    rut = "17.452.889-1",
                    cargo = "Técnico Electromecánico Subterráneo",
                    sectorTurno = "Turno A • Mantenimiento Red LoRa",
                    isOnline = false
                )
            )
            base.add(
                UserProfile(
                    uid = "usr_op_05",
                    nombre = "Rodrigo Tapia Alvarez",
                    email = "r.tapia@minasafe.cl",
                    tipoUsuario = "admin",
                    isAdmin = true,
                    empresa = "Minera Los Andes",
                    rut = "15.932.110-8",
                    cargo = "Ingeniero en Minas y Ventilación",
                    sectorTurno = "Turno Día • Bocamina Principal",
                    isOnline = true
                )
            )
            base
        }
    }

    val filteredUsers = remember(completeUsersList, searchQuery, selectedRoleFilter) {
        completeUsersList.filter { user ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                user.nombre.lowercase().contains(q) ||
                        user.email.lowercase().contains(q) ||
                        user.rutDisplay.lowercase().contains(q) ||
                        user.cargoDisplay.lowercase().contains(q) ||
                        user.sectorTurnoDisplay.lowercase().contains(q)
            }

            val matchesRole = when (selectedRoleFilter) {
                "SUPERADMIN" -> user.rolEfectivo == "superAdmin"
                "ADMIN" -> user.rolEfectivo == "admin"
                "OPERADOR" -> user.rolEfectivo == "usuario"
                else -> true
            }

            matchesQuery && matchesRole
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_users_scrollable_box"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PrimaryTealDark.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Title & Total count badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(BorderAccentBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = BorderAccentBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "CONTROL DE PERSONAL EN FAENA",
                            style = MaterialTheme.typography.labelSmall,
                            color = BorderAccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Dotación y Turnos Mineros",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BorderAccentBlue.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BorderAccentBlue.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${filteredUsers.size} Usuarios",
                        color = BorderAccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar (Buscador por nombre, RUT o cargo)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre, RUT o cargo...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_users"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Role Filters Row (Todos | Superadmin | Admin | Operador)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedRoleFilter == "TODOS",
                    onClick = { selectedRoleFilter = "TODOS" },
                    label = { Text("Todos (${completeUsersList.size})", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                        selectedLabelColor = AccentTeal
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("filter_role_todos")
                )

                FilterChip(
                    selected = selectedRoleFilter == "SUPERADMIN",
                    onClick = { selectedRoleFilter = "SUPERADMIN" },
                    label = { Text("Superadmin", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFA855F7).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFA855F7)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("filter_role_superadmin")
                )

                FilterChip(
                    selected = selectedRoleFilter == "ADMIN",
                    onClick = { selectedRoleFilter = "ADMIN" },
                    label = { Text("Admin", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF06B6D4).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF06B6D4)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("filter_role_admin")
                )

                FilterChip(
                    selected = selectedRoleFilter == "OPERADOR",
                    onClick = { selectedRoleFilter = "OPERADOR" },
                    label = { Text("Operador", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFF59E0B)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("filter_role_operador")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Users List Box (overflow-y-auto max-h-[380px] as requested)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(6.dp)
            ) {
                if (filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron usuarios con ese criterio.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(filteredUsers, key = { it.uid }) { user ->
                            UserFullCardItem(user = user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserFullCardItem(user: UserProfile) {
    val roleColor = when (user.rolEfectivo) {
        "superAdmin" -> Color(0xFFA855F7) // Purple
        "admin" -> Color(0xFF06B6D4)      // Cyan
        else -> Color(0xFFF59E0B)         // Amber
    }

    val roleIcon: ImageVector = when (user.rolEfectivo) {
        "superAdmin" -> Icons.Default.WorkspacePremium
        "admin" -> Icons.Default.AdminPanelSettings
        else -> Icons.Default.Engineering
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_card_${user.uid}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, roleColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Avatar, Name & Role Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Themed Avatar with role icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(roleColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, roleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.nombre.ifBlank { "Personal Minero" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // Role tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = roleColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, roleColor)
                ) {
                    Text(
                        text = "(${user.rolDisplay.split(" ").first()})",
                        color = roleColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata Row: RUT, Cargo & Sector/Turno
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RUT: ${user.rutDisplay}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentTeal
                )

                // Online/Active indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (user.isOnline) StatusSafeGreen else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (user.isOnline) "En Línea" else "Desconectado",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (user.isOnline) StatusSafeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${user.cargoDisplay} • ${user.sectorTurnoDisplay}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
