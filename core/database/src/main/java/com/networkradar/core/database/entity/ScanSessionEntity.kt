package com.networkradar.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_sessions"
)
data class ScanSessionEntity(
    @PrimaryKey val id: String,
    val isSpatial: Boolean,
    val name: String,
    val startedAt: Long,
    val endedAt: Long?,
    val measurementCount: Int
)
