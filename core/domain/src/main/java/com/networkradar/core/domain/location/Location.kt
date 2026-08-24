package com.networkradar.core.domain.location

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val lat: Double,
    val long: Double,
    val accuracy: Float,
    val timestamp: Long
)
