package com.networkradar.core.database.mappers

import com.networkradar.core.database.entity.IndoorMapEntity
import com.networkradar.core.domain.indoor.IndoorMap

fun IndoorMapEntity.toDomain(): IndoorMap {
    return IndoorMap(
        id = id,
        name = name,
        width = width,
        height = height,
        createdAt = createdAt
    )
}

fun IndoorMap.toEntity(): IndoorMapEntity {
    return IndoorMapEntity(
        id = id,
        name = name,
        width = width,
        height = height,
        createdAt = createdAt
    )
}
