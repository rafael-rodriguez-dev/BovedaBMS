package com.rafael.bovedabms.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EquipoDao {
    // Leer todo lo que está guardado en el celular
    @Query("SELECT * FROM equipos_table")
    suspend fun obtenerEquiposOffline(): List<EquipoEntity>

    // Guardar o actualizar la lista descargada de internet
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEquipos(equipos: List<EquipoEntity>)

    // Limpiar la base de datos local por seguridad si es necesario
    @Query("DELETE FROM equipos_table")
    suspend fun borrarTodos()
}