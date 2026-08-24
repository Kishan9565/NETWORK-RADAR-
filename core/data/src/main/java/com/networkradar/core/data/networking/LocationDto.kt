package com.networkradar.core.data.networking

import com.networkradar.core.domain.location.Location
import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(
    val lat: Double,
    val long: Double,
    val accuracy: Float,
    val timestamp: Long
)

fun LocationDto.toDomain(): Location {
    return Location(
        lat = lat,
        long = long,
        accuracy = accuracy,
        timestamp = timestamp
    )
}

fun Location.toDto(): LocationDto {
    return LocationDto(
        lat = lat,
        long = long,
        accuracy = accuracy,
        timestamp = timestamp
    )
}
