package com.rafael.bovedabms.ui

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafael.bovedabms.crypto.CryptoManager
import com.rafael.bovedabms.database.BovedaDatabase
import com.rafael.bovedabms.database.EquipoEntity
import com.rafael.bovedabms.models.CredencialPersonalCreate
import com.rafael.bovedabms.network.ApiClient
import kotlinx.coroutines.launch
import javax.crypto.spec.SecretKeySpec

// Modelos simplificados para mostrar en la interfaz (UI)
data class EquipoUiModel(
    val id: Int,
    val nombre: String,
    val sitio: String,
    val tipo: String,
    val ip: String,
    val usuario: String,
    val passwordRevelada: String
)

data class ClavePersonalUiModel(
    val id: Int,
    val titulo: String,
    val categoria: String,
    val urlServicio: String,
    val usuario: String,
    val passwordRevelada: String
)

class BovedaViewModel : ViewModel() {
    var isLoading = mutableStateOf(true)
    var errorMessage = mutableStateOf("")

    var equiposList = mutableStateListOf<EquipoUiModel>()
    var clavesPersonalesList = mutableStateListOf<ClavePersonalUiModel>()

    var searchQuery = mutableStateOf("")

    // El token dura 12 horas: si el servidor responde 401, la sesión venció
    private fun esSesionExpirada(e: Exception): Boolean =
        e is retrofit2.HttpException && e.code() == 401

    fun cargarEquipos(llaveMaestra: SecretKeySpec, context: Context) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = ""
            val dao = BovedaDatabase.getDatabase(context).equipoDao()

            try {
                // Obtenemos los equipos cifrados desde la API de Render
                val equiposCifrados = ApiClient.retrofitService.getEquipos()

                // Mapeamos para guardar en la base de datos local (Modo Offline)
                val entidadesGuardar = equiposCifrados.map { eq ->
                    EquipoEntity(
                        id = eq.id,
                        sitio = eq.sitio ?: "Sin Asignar",
                        nombre_equipo = eq.nombre_equipo,
                        tipo = eq.tipo,
                        ip_direccion = eq.ip_direccion ?: "N/A",
                        usuario_acceso = eq.usuario_acceso ?: "N/A",
                        password_cifrada = eq.password_cifrada,
                        iv_cifrado = eq.iv_cifrado
                    )
                }

                dao.guardarEquipos(entidadesGuardar)
                procesarYMostrarEquipos(entidadesGuardar, llaveMaestra)

            } catch (e: Exception) {
                val sesionExpirada = esSesionExpirada(e)
                // Si falla el internet (o venció la sesión), buscamos en la base de datos local
                val equiposOffline = dao.obtenerEquiposOffline()
                if (equiposOffline.isNotEmpty()) {
                    procesarYMostrarEquipos(equiposOffline, llaveMaestra)
                    errorMessage.value = if (sesionExpirada) {
                        "Tu sesión expiró. Cierra sesión y vuelve a entrar para sincronizar. Mostrando datos guardados."
                    } else {
                        "⚡ MODO OFFLINE: Mostrando datos guardados localmente."
                    }
                } else {
                    errorMessage.value = if (sesionExpirada) {
                        "Tu sesión expiró. Cierra sesión y vuelve a entrar."
                    } else {
                        "Sin conexión y no hay datos guardados en el celular."
                    }
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    // Desciframos usando exclusivamente la llave personal
    fun cargarClavesPersonales(usuarioId: Int, llavePersonal: SecretKeySpec) {
        viewModelScope.launch {
            try {
                val clavesCifradas = ApiClient.retrofitService.getCredencialesPersonales(usuarioId)

                val listaDescifrada = mutableListOf<ClavePersonalUiModel>()
                for (clave in clavesCifradas) {
                    val passRevelada = CryptoManager.descifrarTexto(clave.password_cifrada, clave.iv_cifrado, llavePersonal)
                    listaDescifrada.add(
                        ClavePersonalUiModel(
                            id = clave.id,
                            titulo = clave.titulo,
                            categoria = clave.categoria,
                            urlServicio = clave.url_servicio ?: "",
                            usuario = clave.usuario_acceso ?: "",
                            passwordRevelada = passRevelada
                        )
                    )
                }
                clavesPersonalesList.clear()
                clavesPersonalesList.addAll(listaDescifrada)

            } catch (e: Exception) {
                errorMessage.value = if (esSesionExpirada(e)) {
                    "Tu sesión expiró. Cierra sesión y vuelve a entrar."
                } else {
                    "Fallo al cargar claves personales: ${e.message}"
                }
            }
        }
    }

    private fun procesarYMostrarEquipos(listaEntidades: List<EquipoEntity>, llave: SecretKeySpec) {
        val listaDescifrada = mutableListOf<EquipoUiModel>()
        for (eq in listaEntidades) {
            val passRevelada = CryptoManager.descifrarTexto(eq.password_cifrada, eq.iv_cifrado, llave)
            listaDescifrada.add(
                EquipoUiModel(
                    id = eq.id,
                    nombre = eq.nombre_equipo,
                    sitio = eq.sitio,
                    tipo = eq.tipo,
                    ip = eq.ip_direccion,
                    usuario = eq.usuario_acceso,
                    passwordRevelada = passRevelada
                )
            )
        }
        equiposList.clear()
        equiposList.addAll(listaDescifrada)
    }

    fun guardarOActualizarEquipo(
        id: Int?, nombre: String, sitio: String, tipo: String, ip: String, usuario: String, passPlana: String,
        llaveMaestra: SecretKeySpec, context: Context, usuarioId: Int
    ) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                val (passCifrada, ivCifrado) = CryptoManager.cifrarTexto(passPlana, llaveMaestra)

                if (id == null || id == 0) {
                    val nuevoEquipo = com.rafael.bovedabms.models.Equipo(
                        id = 0,
                        sitio = sitio,
                        nombre_equipo = nombre,
                        tipo = tipo,
                        ip_direccion = ip,
                        usuario_acceso = usuario,
                        password_cifrada = passCifrada,
                        iv_cifrado = ivCifrado,
                        creado_por = usuarioId
                    )
                    ApiClient.retrofitService.crearEquipo(nuevoEquipo)
                } else {
                    val updateModel = com.rafael.bovedabms.models.EquipoUpdateModel(
                        password_cifrada_nueva = passCifrada,
                        iv_cifrado_nuevo = ivCifrado,
                        modificado_por = usuarioId
                    )
                    ApiClient.retrofitService.actualizarPassword(id, updateModel)
                }

                // Recargar la lista después de guardar
                cargarEquipos(llaveMaestra, context)
            } catch (e: Exception) {
                if (esSesionExpirada(e)) {
                    errorMessage.value = "Tu sesión expiró. Cierra sesión y vuelve a entrar. No se guardó el cambio."
                } else if (e is retrofit2.HttpException) {
                    val errorDelServidor = e.response()?.errorBody()?.string()
                    errorMessage.value = "Rechazado: $errorDelServidor"
                } else {
                    errorMessage.value = "Fallo de red: ${e.message}"
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    // NUEVA FUNCIÓN: Guarda en la tabla equipos_personales usando llavePersonal
    fun guardarCredencialPersonal(
        titulo: String,
        categoria: String,
        urlServicio: String,
        usuario: String,
        passPlana: String,
        llavePersonal: SecretKeySpec,
        usuarioId: Int
    ) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                val (passCifrada, ivCifrado) = CryptoManager.cifrarTexto(passPlana, llavePersonal)

                val nuevaCredencial = CredencialPersonalCreate(
                    usuario_id = usuarioId,
                    titulo = titulo,
                    categoria = categoria.ifBlank { "General" },
                    url_servicio = urlServicio,
                    usuario_acceso = usuario,
                    password_cifrada = passCifrada,
                    iv_cifrado = ivCifrado
                )

                ApiClient.retrofitService.crearCredencialPersonal(nuevaCredencial)
                cargarClavesPersonales(usuarioId, llavePersonal)
            } catch (e: Exception) {
                if (esSesionExpirada(e)) {
                    errorMessage.value = "Tu sesión expiró. Cierra sesión y vuelve a entrar. No se guardó el cambio."
                } else if (e is retrofit2.HttpException) {
                    val errorDelServidor = e.response()?.errorBody()?.string()
                    errorMessage.value = "Rechazado: $errorDelServidor"
                } else {
                    errorMessage.value = "Fallo al guardar clave personal: ${e.message}"
                }
            } finally {
                isLoading.value = false
            }
        }
    }
}