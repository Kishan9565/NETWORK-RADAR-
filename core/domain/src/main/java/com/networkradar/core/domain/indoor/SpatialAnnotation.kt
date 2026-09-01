package com.networkradar.core.domain.indoor

import kotlinx.serialization.Serializable

@Serializable
data class SpatialAnnotation(
    val id: String,
    val sessionId: String,
    val x: Float,
    val y: Float,
    val label: String?,
    val timestamp: Long
)
