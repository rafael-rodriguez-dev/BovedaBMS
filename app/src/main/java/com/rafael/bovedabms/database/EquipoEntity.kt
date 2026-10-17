package com.rafael.bovedabms.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipos_table")
data class EquipoEntity(
    @PrimaryKey val id: Int,
    val sitio: String,
    val nombre_equipo: String,
    val tipo: String,
    val ip_direccion: String,
    val usuario_acceso: String,
    val password_cifrada: String,
    val iv_cifrado: String
)