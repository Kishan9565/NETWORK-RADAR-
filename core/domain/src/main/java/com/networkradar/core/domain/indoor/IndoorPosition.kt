package com.networkradar.core.domain.indoor

import kotlinx.serialization.Serializable

@Serializable
data class IndoorPosition(
    val mapId: String,
    val x: Float,
    val y: Float
)
