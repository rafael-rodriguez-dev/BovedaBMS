package com.rafael.bovedabms.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import javax.crypto.spec.SecretKeySpec

@Composable
fun BovedaScreen(
    llaveMaestra: SecretKeySpec,
    llavePersonal: SecretKeySpec,
    viewModel: BovedaViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var equipoEditando by remember { mutableStateOf<EquipoUiModel?>(null) }

    var tabSeleccionada by remember { mutableStateOf(0) }
    val titulosTabs = listOf("Infraestructura BMS", "Mis Claves Personales")

    LaunchedEffect(Unit) {
        viewModel.cargarEquipos(llaveMaestra, context)
        val idUsuarioLogueado = 3 // Extraer de la sesión en el futuro
        viewModel.cargarClavesPersonales(idUsuarioLogueado, llavePersonal)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                equipoEditando = null
                showDialog = true
            }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Equipo")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).systemBarsPadding().padding(16.dp)) {

            TabRow(selectedTabIndex = tabSeleccionada) {
                titulosTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabSeleccionada == index,
                        onClick = { tabSeleccionada = index },
                        text = { Text(text = title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Buscar...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                if (viewModel.errorMessage.value.isNotEmpty()) {
                    Text(text = viewModel.errorMessage.value, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
                }

                val query = viewModel.searchQuery.value.lowercase()

                if (tabSeleccionada == 0) {
                    val equiposFiltrados = viewModel.equiposList.filter {
                        it.nombre.lowercase().contains(query) || it.sitio.lowercase().contains(query) || it.ip.lowercase().contains(query)
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(equiposFiltrados) { equipo ->
                            EquipoCard(equipo) {
                                equipoEditando = equipo
                                showDialog = true
                            }
                        }
                    }
                } else {
                    val clavesFiltradas = viewModel.clavesPersonalesList.filter {
                        it.titulo.lowercase().contains(query) || it.categoria.lowercase().contains(query)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(clavesFiltradas) { clave ->
                            ClavePersonalCard(clave) {
                                // Aquí iría la lógica para editar clave personal
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var nombre by remember { mutableStateOf(equipoEditando?.nombre ?: "") }
        var sitio by remember { mutableStateOf(equipoEditando?.sitio ?: "") }
        var tipo by remember { mutableStateOf(equipoEditando?.tipo ?: "Servidor") }
        var ip by remember { mutableStateOf(equipoEditando?.ip ?: "") }
        var usuario by remember { mutableStateOf(equipoEditando?.usuario ?: "") }
        var clave by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (equipoEditando == null) "Nuevo Equipo BMS" else "Actualizar Contraseña") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (equipoEditando == null) {
                        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre del Equipo") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sitio, onValueChange = { sitio = it }, label = { Text("Sitio / Edificio") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = tipo, onValueChange = { tipo = it }, label = { Text("Tipo (CCTV, Acceso, etc)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("Dirección IP") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = usuario, onValueChange = { usuario = it }, label = { Text("Usuario de Acceso") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = clave, onValueChange = { clave = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text(text = "Equipo: ${equipoEditando?.nombre}", fontWeight = FontWeight.Bold)
                        Text(text = "Sitio: ${equipoEditando?.sitio}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = clave, onValueChange = { clave = it }, label = { Text("Nueva Contraseña") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val idUsuarioLogueado = 3

                    viewModel.guardarOActualizarEquipo(
                        id = equipoEditando?.id, nombre = nombre, sitio = sitio, tipo = tipo,
                        ip = ip, usuario = usuario, passPlana = clave,
                        llaveMaestra = llaveMaestra, context = context,
                        usuarioId = idUsuarioLogueado
                    )
                    showDialog = false
                }) { Text("Guardar cambios") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun EquipoCard(equipo: EquipoUiModel, onEditClick: () -> Unit) {
    val colorClave = if (equipo.passwordRevelada == "Bloqueado") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = equipo.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = equipo.sitio + " • " + equipo.tipo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar Equipo", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().clickable { clipboardManager.setText(AnnotatedString(equipo.ip)); Toast.makeText(context, "IP copiada", Toast.LENGTH_SHORT).show() }.padding(vertical = 4.dp)) {
                Text(text = "IP: ", fontWeight = FontWeight.Bold); Text(text = equipo.ip)
            }
            Row(modifier = Modifier.fillMaxWidth().clickable { clipboardManager.setText(AnnotatedString(equipo.usuario)); Toast.makeText(context, "Usuario copiado", Toast.LENGTH_SHORT).show() }.padding(vertical = 4.dp)) {
                Text(text = "Usuario: ", fontWeight = FontWeight.Bold); Text(text = equipo.usuario)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().clickable {
                    if (equipo.passwordRevelada != "Bloqueado") {
                        clipboardManager.setText(AnnotatedString(equipo.passwordRevelada))
                        Toast.makeText(context, "Clave copiada", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text(text = "Clave: ${equipo.passwordRevelada}", color = colorClave, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
fun ClavePersonalCard(clave: ClavePersonalUiModel, onEditClick: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val colorClave = if (clave.passwordRevelada == "Bloqueado") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = clave.titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = clave.categoria, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar Clave Personal", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (clave.urlServicio.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().clickable { clipboardManager.setText(AnnotatedString(clave.urlServicio)); Toast.makeText(context, "URL copiada", Toast.LENGTH_SHORT).show() }.padding(vertical = 4.dp)) {
                    Text(text = "URL: ", fontWeight = FontWeight.Bold); Text(text = clave.urlServicio)
                }
            }
            if (clave.usuario.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().clickable { clipboardManager.setText(AnnotatedString(clave.usuario)); Toast.makeText(context, "Usuario copiado", Toast.LENGTH_SHORT).show() }.padding(vertical = 4.dp)) {
                    Text(text = "Usuario: ", fontWeight = FontWeight.Bold); Text(text = clave.usuario)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().clickable {
                    if (clave.passwordRevelada != "Bloqueado") {
                        clipboardManager.setText(AnnotatedString(clave.passwordRevelada))
                        Toast.makeText(context, "Clave copiada", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text(text = "Clave: ${clave.passwordRevelada}", color = colorClave, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
            }
        }
    }
}
