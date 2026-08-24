package com.networkradar.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.networkradar.core.database.dao.IndoorMapDao
import com.networkradar.core.database.dao.NetworkMeasurementPointDao
import com.networkradar.core.database.dao.ScanSessionDao
import com.networkradar.core.database.entity.IndoorMapEntity
import com.networkradar.core.database.entity.NetworkMeasurementPointEntity
import com.networkradar.core.database.entity.ScanSessionEntity

@Database(
    entities = [
        IndoorMapEntity::class,
        ScanSessionEntity::class,
        NetworkMeasurementPointEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NetworkRadarDatabase : RoomDatabase() {
    abstract fun indoorMapDao(): IndoorMapDao
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun measurementPointDao(): NetworkMeasurementPointDao
}
