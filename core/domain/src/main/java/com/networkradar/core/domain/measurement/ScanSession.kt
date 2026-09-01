package com.networkradar.core.domain.measurement

import kotlinx.serialization.Serializable

@Serializable
data class ScanSession(
    val id: String,
    val isSpatial: Boolean,
    val name: String,
    val startedAt: Long,
    val endedAt: Long?,
    val measurementCount: Int
)
