package com.networkradar.core.domain.measurement

import com.networkradar.core.domain.location.Location
import com.networkradar.core.domain.indoor.IndoorPosition
import kotlinx.serialization.Serializable

@Serializable
data class NetworkMeasurementPoint(
    val id: Long? = null,
    val location: Location?,
    val indoorPosition: IndoorPosition?,
    val wifi: WifiMeasurement?,
    val cellular: CellularMeasurement?,
    val internet: InternetMeasurement?,
    val timestamp: Long
)
