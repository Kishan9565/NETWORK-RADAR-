package com.networkradar.core.database.mappers

import com.networkradar.core.database.entity.ScanSessionEntity
import com.networkradar.core.domain.measurement.ScanSession

fun ScanSessionEntity.toDomain(): ScanSession {
    return ScanSession(
        id = id,
        isSpatial = isSpatial,
        name = name,
        startedAt = startedAt,
        endedAt = endedAt,
        measurementCount = measurementCount
    )
}

fun ScanSession.toEntity(): ScanSessionEntity {
    return ScanSessionEntity(
        id = id,
        isSpatial = isSpatial,
        name = name,
        startedAt = startedAt,
        endedAt = endedAt,
        measurementCount = measurementCount
    )
}
