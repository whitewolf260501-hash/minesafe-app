package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.ScreenDestination
import com.example.ui.screens.AdminUsersScreen
import com.example.ui.screens.AlertConfigScreen
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.ChartsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.BorderAccentOrange
import com.example.ui.theme.GradientNavyStart
import com.example.ui.theme.GradientTealEnd
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.PrimaryTealDarkVariant
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineSafeApp(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isDeviceConnected by viewModel.isDeviceConnected.collectAsState()
    val isGatewayConnected by viewModel.isGatewayConnected.collectAsState()
    val deviceLastSeenText by viewModel.deviceLastSeenText.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val alertReading by viewModel.alertDeviceData.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Gate unauthenticated users with Login Screen
    if (!isLoggedIn) {
        LoginScreen(
            viewModel = viewModel,
            onSuccess = {
                viewModel.navigateTo(ScreenDestination.DASHBOARD)
            }
        )
        return
    }

    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen != ScreenDestination.DASHBOARD) {
        viewModel.navigateTo(ScreenDestination.DASHBOARD)
    }

    // Primary items for bottom bar
    val bottomNavItems = listOf(
        ScreenDestination.DASHBOARD,
        ScreenDestination.DEVICES,
        ScreenDestination.ALERTS,
        ScreenDestination.ALERT_CONFIG,
        ScreenDestination.CHARTS
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Brand Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryTealDark)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Logo",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "MineSafe",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "v1.0 • IoT Minero",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = userProfile.nombre.ifBlank { "Operador de Guardia" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = userProfile.email.ifBlank { "operador@minesafe.pe" },
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Role badge
                        val roleBg = when (userProfile.rolEfectivo) {
                            "superAdmin" -> BorderAccentBlue
                            "admin" -> BorderAccentOrange
                            else -> StatusSafeGreen
                        }

                        Box(
                            modifier = Modifier
                                .background(roleBg, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = userProfile.rolDisplay.uppercase(Locale.getDefault()),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MÓDULOS DE MONITOREO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Standard Operator Navigation Items
                listOf(
                    ScreenDestination.DASHBOARD,
                    ScreenDestination.DEVICES,
                    ScreenDestination.ALERTS,
                    ScreenDestination.ALERT_CONFIG,
                    ScreenDestination.CHARTS,
                    ScreenDestination.MAP,
                    ScreenDestination.HISTORY,
                    ScreenDestination.PROFILE
                ).forEach { destination ->
                    NavigationDrawerItem(
                        icon = {
                            if (destination == ScreenDestination.ALERTS && !alertReading.isAllSafe) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = StatusDangerRed) {
                                            Text("${alertReading.activeAlerts.size}")
                                        }
                                    }
                                ) {
                                    Icon(destination.icon, contentDescription = destination.title)
                                }
                            } else {
                                Icon(destination.icon, contentDescription = destination.title)
                            }
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontWeight = if (currentScreen == destination) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = currentScreen == destination,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(destination)
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = PrimaryTealDark.copy(alpha = 0.12f),
                            selectedIconColor = PrimaryTealDark,
                            selectedTextColor = PrimaryTealDark
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_${destination.name.lowercase()}")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

                // Admin Section
                Text(
                    text = "ADMINISTRACIÓN (SOLO ADMIN)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (userProfile.canAccessAdminPanel) BorderAccentOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (userProfile.canAccessAdminPanel) {
                    NavigationDrawerItem(
                        icon = {
                            Icon(ScreenDestination.ADMIN.icon, contentDescription = ScreenDestination.ADMIN.title, tint = BorderAccentOrange)
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ScreenDestination.ADMIN.title,
                                    fontWeight = if (currentScreen == ScreenDestination.ADMIN) FontWeight.Bold else FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(BorderAccentOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ADMIN", color = BorderAccentOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        selected = currentScreen == ScreenDestination.ADMIN,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenDestination.ADMIN)
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = BorderAccentOrange.copy(alpha = 0.12f),
                            selectedIconColor = BorderAccentOrange,
                            selectedTextColor = BorderAccentOrange
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_admin")
                    )
                } else {
                    // Locked indicator for regular users
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "🔒 Gestión de Usuarios bloqueada.\nDisponible solo para administradores de mina.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Active Session Info Card in Drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ROL ASIGNADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = PrimaryTealDark,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userProfile.rolDisplay,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", tint = StatusDangerRed) },
                    label = { Text("Cerrar Sesión", color = StatusDangerRed, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("drawer_btn_logout")
                )
                Text(
                    text = "Backend: minefase-fc5f5\nSensor: dispositivo1 (${if (isDeviceConnected) "ESP32 Conectado" else "ESP32 Offline"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "MineSafe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = currentScreen.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("btn_menu_drawer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menú Principal",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // IoT Device Status Pill (click to test toggle simulation)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDeviceConnected) StatusSafeGreen.copy(alpha = 0.25f) else StatusDangerRed.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.toggleDeviceSimulation() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isDeviceConnected) StatusSafeGreen else StatusDangerRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isDeviceConnected) "ESP32 ON" else "ESP32 OFF",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Gateway Device Status Pill (click to test toggle simulation)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isGatewayConnected) StatusSafeGreen.copy(alpha = 0.25f) else StatusDangerRed.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.toggleGatewaySimulation() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isGatewayConnected) StatusSafeGreen else StatusDangerRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isGatewayConnected) "GW ON" else "GW OFF",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Firebase connection pill
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(StatusSafeGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "minefase-fc5f5",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Quick Alert indicator in Top Bar
                        if (!alertReading.isAllSafe) {
                            IconButton(
                                onClick = { viewModel.navigateTo(ScreenDestination.ALERTS) },
                                modifier = Modifier.testTag("top_bar_alert_icon")
                            ) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = StatusDangerRed) {
                                            Text("${alertReading.activeAlerts.size}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Alertas",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryTealDark
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { destination ->
                        val isSelected = currentScreen == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(destination) },
                            icon = {
                                if (destination == ScreenDestination.ALERTS && !alertReading.isAllSafe) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = StatusDangerRed) {
                                                Text("${alertReading.activeAlerts.size}")
                                            }
                                        }
                                    ) {
                                        Icon(destination.icon, contentDescription = destination.title)
                                    }
                                } else {
                                    Icon(destination.icon, contentDescription = destination.title)
                                }
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryTealDark,
                                selectedTextColor = PrimaryTealDark,
                                indicatorColor = PrimaryTealDark.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("bottom_nav_${destination.name.lowercase()}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    ScreenDestination.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                    ScreenDestination.DEVICES -> DevicesScreen(
                        viewModel = viewModel,
                        onNavigateToCharts = { viewModel.navigateTo(ScreenDestination.CHARTS) }
                    )
                    ScreenDestination.ALERTS -> AlertsScreen(viewModel = viewModel)
                    ScreenDestination.ALERT_CONFIG -> AlertConfigScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.ALERTS) }
                    )
                    ScreenDestination.CHARTS -> ChartsScreen(viewModel = viewModel)
                    ScreenDestination.MAP -> MapScreen(viewModel = viewModel)
                    ScreenDestination.HISTORY -> HistoryScreen(viewModel = viewModel)
                    ScreenDestination.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        onLogout = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
                    )
                    ScreenDestination.ADMIN -> AdminUsersScreen(viewModel = viewModel)
                }
            }
        }
    }
}
