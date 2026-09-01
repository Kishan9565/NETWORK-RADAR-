package com.networkradar.core.database.mappers

import com.networkradar.core.database.entity.SpatialAnnotationEntity
import com.networkradar.core.domain.indoor.SpatialAnnotation

fun SpatialAnnotationEntity.toDomain(): SpatialAnnotation {
    return SpatialAnnotation(
        id = id.toString(),
        sessionId = sessionId,
        x = x,
        y = y,
        label = label,
        timestamp = timestamp
    )
}

fun SpatialAnnotation.toEntity(): SpatialAnnotationEntity {
    return SpatialAnnotationEntity(
        id = id.toLongOrNull() ?: 0L,
        sessionId = sessionId,
        x = x,
        y = y,
        label = label,
        timestamp = timestamp
    )
}
