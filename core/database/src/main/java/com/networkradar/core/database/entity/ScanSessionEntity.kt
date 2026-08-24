package com.networkradar.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_sessions",
    foreignKeys = [
        ForeignKey(
            entity = IndoorMapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["mapId"])]
)
data class ScanSessionEntity(
    @PrimaryKey val id: String,
    val mapId: String?,
    val name: String,
    val startedAt: Long,
    val endedAt: Long?,
    val measurementCount: Int
)
