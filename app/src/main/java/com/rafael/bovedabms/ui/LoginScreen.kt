package com.rafael.bovedabms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import javax.crypto.spec.SecretKeySpec

@Composable
fun LoginScreen(
    tieneCredencialesGuardadas: Boolean,
    onPedirPinHuella: () -> Unit,
    onLoginSuccess: (SecretKeySpec, String, String, String) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    // Estados para controlar si se ve o no el texto de cada contraseña
    var passwordPersonalVisible by remember { mutableStateOf(false) }
    var passwordMaestraVisible by remember { mutableStateOf(false) }

    // Fondo general de la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Tarjeta blanca (o gris oscuro en modo oscuro) con sombra
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icono superior de candado
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Seguridad",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Bóveda BMS",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Inicia sesión para acceder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Campo de Correo
                OutlinedTextField(
                    value = viewModel.email.value,
                    onValueChange = { viewModel.email.value = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Campo de Contraseña Personal con botón de "Ojito"
                OutlinedTextField(
                    value = viewModel.passwordPersonal.value,
                    onValueChange = { viewModel.passwordPersonal.value = it },
                    label = { Text("Contraseña Personal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordPersonalVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordPersonalVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordPersonalVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        IconButton(onClick = { passwordPersonalVisible = !passwordPersonalVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Campo de Contraseña Maestra con botón de "Ojito"
                OutlinedTextField(
                    value = viewModel.passwordMaestra.value,
                    onValueChange = { viewModel.passwordMaestra.value = it },
                    label = { Text("Contraseña Maestra (BMS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordMaestraVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordMaestraVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordMaestraVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        IconButton(onClick = { passwordMaestraVisible = !passwordMaestraVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Mensaje de Error
                if (viewModel.errorMessage.value.isNotEmpty()) {
                    Text(
                        text = viewModel.errorMessage.value,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Botón Principal de Iniciar Sesión
                Button(
                    onClick = { viewModel.login(onSuccess = onLoginSuccess) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isLoading.value
                ) {
                    Text(
                        text = if (viewModel.isLoading.value) "Autenticando..." else "Iniciar Sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Separador y Botón de Biometría (Aparece si ya iniciaste sesión una vez)
                if (tieneCredencialesGuardadas) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedButton(
                        onClick = onPedirPinHuella,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Huella",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Desbloquear con PIN / Huella")
                    }
                }
            }
        }
    }
}