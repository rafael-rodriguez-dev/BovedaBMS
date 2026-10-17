package com.rafael.bovedabms.models

data class LoginRequest(
    val email: String,
    val password_personal: String
)

data class Usuario(
    val id: Int,
    val nombre: String,
    val email: String? = null,
    val activo: Boolean? = true,
    val rol: String? = "tecnico",
    val token: String? = null
)

data class Equipo(
    val id: Int,
    val nombre_equipo: String,
    val sitio: String?,
    val tipo: String,
    val ip_direccion: String?,
    val usuario_acceso: String?,
    val password_cifrada: String,
    val iv_cifrado: String,
    val creado_por: Int
)

// MODELO PARA LEER CLAVES PERSONALES
data class CredencialPersonal(
    val id: Int,
    val titulo: String,
    val categoria: String,
    val url_servicio: String?,
    val usuario_acceso: String?,
    val password_cifrada: String,
    val iv_cifrado: String
)

// MODELO PARA CREAR UNA NUEVA CLAVE PERSONAL
data class CredencialPersonalCreate(
    val usuario_id: Int,
    val titulo: String,
    val categoria: String,
    val url_servicio: String?,
    val usuario_acceso: String?,
    val password_cifrada: String,
    val iv_cifrado: String
)