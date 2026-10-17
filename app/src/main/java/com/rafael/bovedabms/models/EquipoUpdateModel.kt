package com.rafael.bovedabms.models

data class EquipoUpdateModel(
    val password_cifrada_nueva: String,
    val iv_cifrado_nuevo: String,
    val modificado_por: Int
)