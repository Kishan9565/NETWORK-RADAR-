package com.networkradar.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "indoor_maps")
data class IndoorMapEntity(
    @PrimaryKey val id: String,
    val name: String,
    val width: Float,
    val height: Float,
    val createdAt: Long
)
