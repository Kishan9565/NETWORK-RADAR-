package com.networkradar.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_points",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class NetworkMeasurementPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    
    // Location (Outdoor)
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracy: Float?,
    val locationTimestamp: Long?,

    // Indoor Position
    val indoorX: Float?,
    val indoorY: Float?,
    val indoorTimestamp: Long?,

    // Wifi
    @Embedded(prefix = "wifi_") val wifi: WifiMeasurementEntity?,
    
    // Cellular
    @Embedded(prefix = "cell_") val cellular: CellularMeasurementEntity?,
    
    // Internet
    @Embedded(prefix = "net_") val internet: InternetMeasurementEntity?
)

data class WifiMeasurementEntity(
    val rssi: Int?,
    val ssid: String?,
    val frequency: Int?,
    val linkSpeed: Int?,
    val timestamp: Long
)

data class CellularMeasurementEntity(
    val networkType: String?,
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?,
    val rssi: Int?,
    val timestamp: Long
)

data class InternetMeasurementEntity(
    val latencyMs: Double?,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val timestamp: Long
)
