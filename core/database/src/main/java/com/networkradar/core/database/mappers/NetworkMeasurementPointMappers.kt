package com.networkradar.core.database.mappers

import com.networkradar.core.database.entity.CellularMeasurementEntity
import com.networkradar.core.database.entity.InternetMeasurementEntity
import com.networkradar.core.database.entity.NetworkMeasurementPointEntity
import com.networkradar.core.database.entity.WifiMeasurementEntity
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.location.Location
import com.networkradar.core.domain.measurement.CellularMeasurement
import com.networkradar.core.domain.measurement.InternetMeasurement
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.WifiMeasurement

fun NetworkMeasurementPointEntity.toDomain(): NetworkMeasurementPoint {
    return NetworkMeasurementPoint(
        id = id,
        location = if (latitude != null && longitude != null) {
            Location(
                lat = latitude,
                long = longitude,
                accuracy = locationAccuracy ?: 0f,
                timestamp = locationTimestamp ?: 0L
            )
        } else null,
        indoorPosition = if (indoorX != null && indoorY != null) {
            IndoorPosition(
                sessionId = sessionId,
                x = indoorX,
                y = indoorY,
                timestamp = indoorTimestamp ?: 0L
            )
        } else null,
        wifi = wifi?.toDomain(),
        cellular = cellular?.toDomain(),
        internet = internet?.toDomain(),
        timestamp = timestamp
    )
}

fun WifiMeasurementEntity.toDomain() = WifiMeasurement(
    rssi = rssi,
    ssid = ssid,
    frequency = frequency,
    linkSpeed = linkSpeed,
    timestamp = timestamp
)

fun CellularMeasurementEntity.toDomain() = CellularMeasurement(
    networkType = networkType,
    rsrp = rsrp,
    rsrq = rsrq,
    sinr = sinr,
    rssi = rssi,
    timestamp = timestamp
)

fun InternetMeasurementEntity.toDomain() = InternetMeasurement(
    latencyMs = latencyMs,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    timestamp = timestamp
)

fun NetworkMeasurementPoint.toEntity(sessionId: String): NetworkMeasurementPointEntity {
    return NetworkMeasurementPointEntity(
        id = id ?: 0L,
        sessionId = sessionId,
        timestamp = timestamp,
        latitude = location?.lat,
        longitude = location?.long,
        locationAccuracy = location?.accuracy,
        locationTimestamp = location?.timestamp,
        indoorX = indoorPosition?.x,
        indoorY = indoorPosition?.y,
        indoorTimestamp = indoorPosition?.timestamp,
        wifi = wifi?.toEntity(),
        cellular = cellular?.toEntity(),
        internet = internet?.toEntity()
    )
}

fun WifiMeasurement.toEntity() = WifiMeasurementEntity(
    rssi = rssi,
    ssid = ssid,
    frequency = frequency,
    linkSpeed = linkSpeed,
    timestamp = timestamp
)

fun CellularMeasurement.toEntity() = CellularMeasurementEntity(
    networkType = networkType,
    rsrp = rsrp,
    rsrq = rsrq,
    sinr = sinr,
    rssi = rssi,
    timestamp = timestamp
)

fun InternetMeasurement.toEntity() = InternetMeasurementEntity(
    latencyMs = latencyMs,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    timestamp = timestamp
)
