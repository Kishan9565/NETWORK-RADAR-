package com.networkradar.core.domain.measurement

import kotlinx.serialization.Serializable

@Serializable
data class WifiMeasurement(
    val rssi: Int?,
    val ssid: String?,
    val frequency: Int?,
    val linkSpeed: Int?,
    val timestamp: Long
)
