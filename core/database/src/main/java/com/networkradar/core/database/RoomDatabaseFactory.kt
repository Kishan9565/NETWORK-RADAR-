package com.networkradar.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

class RoomDatabaseFactory(private val context: Context) {
    fun <T : RoomDatabase> create(
        databaseClass: Class<T>, 
        name: String,
        vararg migrations: Migration
    ): T {
        return Room.databaseBuilder(
            context.applicationContext,
            databaseClass,
            name
        )
        .addMigrations(*migrations)
        .build()
    }
}
