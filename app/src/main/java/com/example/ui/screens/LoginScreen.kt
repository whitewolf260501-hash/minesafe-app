package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BorderAccentBlue
import com.example.ui.theme.BorderAccentGreen
import com.example.ui.theme.BorderAccentOrange
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.PrimaryTealDarkVariant
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onSuccess: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val savedEmail by viewModel.savedEmail.collectAsState()
    val savedPassword by viewModel.savedPassword.collectAsState()
    val rememberDevicePref by viewModel.rememberDevice.collectAsState()
    val autoLoginPref by viewModel.autoLogin.collectAsState()

    var email by remember(savedEmail) { mutableStateOf(savedEmail) }
    var password by remember(savedPassword) { mutableStateOf(savedPassword) }
    var rememberMe by remember(rememberDevicePref) { mutableStateOf(rememberDevicePref) }
    var autoLogin by remember(autoLoginPref) { mutableStateOf(autoLoginPref) }

    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Brand Header Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(PrimaryTealDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "MineSafe Logo",
                    tint = AccentTeal,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "MineSafe",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryTealDark
            )

            Text(
                text = "Monitoreo Ambiental y Seguridad Minera",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = PrimaryTealDark
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Iniciar Sesión", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("tab_login")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Registrarse", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("tab_register")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Remembered Device Indicator Banner
                    if (savedEmail.isNotBlank() && rememberDevicePref) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("banner_device_remembered"),
                            colors = CardDefaults.cardColors(containerColor = PrimaryTealDark.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, PrimaryTealDark.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "Dispositivo Recordado",
                                        tint = PrimaryTealDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Dispositivo de Confianza",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryTealDark
                                        )
                                        Text(
                                            text = "Credenciales recordadas para $savedEmail",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.forgetSavedDevice()
                                        email = ""
                                        password = ""
                                        rememberMe = false
                                        autoLogin = false
                                    }
                                ) {
                                    Text(
                                        text = "Olvidar",
                                        fontSize = 11.sp,
                                        color = StatusDangerRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = PrimaryTealDark)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_email")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Contraseña", tint = PrimaryTealDark)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password")
                    )

                    // Forgot password link
                    if (selectedTab == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    forgotEmail = email
                                    showForgotDialog = true
                                },
                                modifier = Modifier.testTag("button_forgot_password")
                            ) {
                                Text(
                                    text = "¿Olvidé mi contraseña?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryTealDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // CHECKBOX: Recordar dispositivo y credenciales
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryTealDark),
                            modifier = Modifier.testTag("checkbox_remember_device")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Recordar este dispositivo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Guarda tus credenciales para no tener que escribirlas de nuevo",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // CHECKBOX: Inicio de sesión automático
                    if (rememberMe) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { autoLogin = !autoLogin }
                                .padding(start = 12.dp, top = 2.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoLogin,
                                onCheckedChange = { autoLogin = it },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryTealDark),
                                modifier = Modifier.testTag("checkbox_auto_login")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Iniciar sesión automáticamente al abrir la app",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Button (Login or Register)
                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                viewModel.login(
                                    email = email,
                                    pass = password,
                                    remember = rememberMe,
                                    autoLog = autoLogin,
                                    onSuccess = onSuccess
                                )
                            } else {
                                viewModel.register(
                                    email = email,
                                    pass = password,
                                    remember = rememberMe,
                                    autoLog = autoLogin,
                                    onSuccess = onSuccess
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_submit_auth"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTealDark,
                            contentColor = Color.White
                        ),
                        enabled = email.isNotBlank() && password.length >= 6
                    ) {
                        Text(
                            text = if (selectedTab == 0) "INGRESAR A MINESAFE" else "CREAR CUENTA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security & Role Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Autenticación Segura",
                        tint = PrimaryTealDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "El acceso y nivel de permisos (SuperAdmin, Admin o Operador) se asignan estrictamente según las credenciales del usuario autenticado en Firebase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Forgot password Dialog
        if (showForgotDialog) {
            AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                title = { Text("Recuperar Contraseña", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Ingresa el correo electrónico asociado a tu cuenta de MineSafe para recibir las instrucciones de restablecimiento."
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("Correo registrado") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (forgotEmail.isNotBlank()) {
                                viewModel.resetPassword(forgotEmail)
                                showForgotDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTealDark)
                    ) {
                        Text("Enviar Correo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
