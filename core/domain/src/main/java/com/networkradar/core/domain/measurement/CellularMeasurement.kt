package com.networkradar.core.domain.measurement

import kotlinx.serialization.Serializable

@Serializable
data class CellularMeasurement(
    val networkType: String?,
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?,
    val rssi: Int?,
    val timestamp: Long
)
