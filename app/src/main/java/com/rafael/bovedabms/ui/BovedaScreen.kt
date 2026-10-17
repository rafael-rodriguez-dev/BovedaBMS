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
import com.rafael.bovedabms.network.Sesion
import javax.crypto.spec.SecretKeySpec

@Composable
fun BovedaScreen(
    llaveMaestra: SecretKeySpec,
    llavePersonal: SecretKeySpec,
    viewModel: BovedaViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDialogBMS by remember { mutableStateOf(false) }
    var showDialogPersonal by remember { mutableStateOf(false) }
    var equipoEditando by remember { mutableStateOf<EquipoUiModel?>(null) }

    var tabSeleccionada by remember { mutableStateOf(0) }
    val titulosTabs = listOf("Infraestructura BMS", "Mis Claves Personales")

    // Obtenemos dinámicamente el ID del usuario que inició sesión en Sesion
    val idUsuarioLogueado = Sesion.usuarioActual?.id ?: 3

    LaunchedEffect(idUsuarioLogueado) {
        viewModel.cargarEquipos(llaveMaestra, context)
        viewModel.cargarClavesPersonales(idUsuarioLogueado, llavePersonal)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (tabSeleccionada == 0) {
                        equipoEditando = null
                        showDialogBMS = true
                    } else {
                        showDialogPersonal = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (tabSeleccionada == 0) "Agregar Equipo BMS" else "Agregar Clave Personal"
                )
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
                                showDialogBMS = true
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

    // DIÁLOGO 1: CREAR O EDITAR EQUIPO BMS
    if (showDialogBMS) {
        var nombre by remember { mutableStateOf(equipoEditando?.nombre ?: "") }
        var sitio by remember { mutableStateOf(equipoEditando?.sitio ?: "") }
        var tipo by remember { mutableStateOf(equipoEditando?.tipo ?: "Servidor") }
        var ip by remember { mutableStateOf(equipoEditando?.ip ?: "") }
        var usuario by remember { mutableStateOf(equipoEditando?.usuario ?: "") }
        var clave by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialogBMS = false },
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
                    viewModel.guardarOActualizarEquipo(
                        id = equipoEditando?.id, nombre = nombre, sitio = sitio, tipo = tipo,
                        ip = ip, usuario = usuario, passPlana = clave,
                        llaveMaestra = llaveMaestra, context = context,
                        usuarioId = idUsuarioLogueado
                    )
                    showDialogBMS = false
                }) { Text("Guardar cambios") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogBMS = false }) { Text("Cancelar") }
            }
        )
    }

    // DIÁLOGO 2: CREAR NUEVA CREDENCIAL PERSONAL
    if (showDialogPersonal) {
        var categoria by remember { mutableStateOf("") }
        var titulo by remember { mutableStateOf("") }
        var urlServicio by remember { mutableStateOf("") }
        var usuario by remember { mutableStateOf("") }
        var clave by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialogPersonal = false },
            title = { Text("Nueva Credencial Personal") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoría (Ej. Correos, Bancos, VPN)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título (Ej. Gmail Personal)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = urlServicio, onValueChange = { urlServicio = it }, label = { Text("URL / Sistema (Opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = usuario, onValueChange = { usuario = it }, label = { Text("Usuario (Opcional)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = clave, onValueChange = { clave = it }, label = { Text("Contraseña Personal") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (titulo.isBlank() || clave.isBlank()) {
                        Toast.makeText(context, "El título y la contraseña son obligatorios", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.guardarCredencialPersonal(
                        titulo = titulo,
                        categoria = categoria,
                        urlServicio = urlServicio,
                        usuario = usuario,
                        passPlana = clave,
                        llavePersonal = llavePersonal,
                        usuarioId = idUsuarioLogueado
                    )
                    showDialogPersonal = false
                }) { Text("Guardar Secreto") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogPersonal = false }) { Text("Cancelar") }
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