package com.networkradar.core.domain.measurement

import kotlinx.serialization.Serializable

@Serializable
data class InternetMeasurement(
    val latencyMs: Double?,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val timestamp: Long
)
