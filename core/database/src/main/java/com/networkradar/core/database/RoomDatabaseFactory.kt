package com.networkradar.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

class RoomDatabaseFactory(private val context: Context) {
    fun <T : RoomDatabase> create(databaseClass: Class<T>, name: String): T {
        return Room.databaseBuilder(
            context.applicationContext,
            databaseClass,
            name
        ).build()
    }
}
