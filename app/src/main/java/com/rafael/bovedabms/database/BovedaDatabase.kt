package com.rafael.bovedabms.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EquipoEntity::class], version = 1, exportSchema = false)
abstract class BovedaDatabase : RoomDatabase() {

    abstract fun equipoDao(): EquipoDao

    companion object {
        @Volatile
        private var INSTANCE: BovedaDatabase? = null

        fun getDatabase(context: Context): BovedaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BovedaDatabase::class.java,
                    "boveda_offline_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}