package com.networkradar.feature.radar.domain

import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.location.Location
import com.networkradar.core.domain.location.LocationDataSource
import com.networkradar.core.domain.location.LocationObservation
import com.networkradar.core.domain.measurement.CellularDataSource
import com.networkradar.core.domain.measurement.CellularMeasurement
import com.networkradar.core.domain.measurement.ConnectivityDataSource
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.WifiDataSource
import com.networkradar.core.domain.measurement.WifiMeasurement
import com.networkradar.core.domain.networking.ConnectivityState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.abs


class ObserveRadarMeasurementsUseCase(
    private val connectivityDataSource: ConnectivityDataSource,
    private val wifiDataSource: WifiDataSource,
    private val cellularDataSource: CellularDataSource,
    private val locationDataSource: LocationDataSource,
    private val indoorDataSource: IndoorDataSource
) {
    operator fun invoke(): Flow<RadarMeasurement> = combine(
        connectivityDataSource.getConnectivityState(),
        wifiDataSource.getWifiMeasurement(),
        cellularDataSource.getCellularMeasurement(),
        locationDataSource.getLocationUpdates(),
        indoorDataSource.currentPosition
    ) { connectivity, wifi, cellular, locationObs, indoorPosition ->
        val currentTime = System.currentTimeMillis()
        
        val validLocation = (locationObs as? LocationObservation.Success)?.location?.takeIf {
            isFresh(it.timestamp, currentTime)
        }
        
        val validWifi = wifi?.takeIf { isFresh(it.timestamp, currentTime) }
        val validCellular = cellular?.takeIf { isFresh(it.timestamp, currentTime) }

        RadarMeasurement(
            connectivity = connectivity,
            locationStatus = locationObs,
            point = NetworkMeasurementPoint(
                location = validLocation,
                indoorPosition = indoorPosition,
                wifi = validWifi,
                cellular = validCellular,
                internet = null, // Internet speed tests are active, not passive
                timestamp = currentTime
            )
        )
    }

    private fun isFresh(timestamp: Long, currentTime: Long): Boolean {
        return abs(currentTime - timestamp) < FRESHNESS_THRESHOLD_MS
    }

    companion object {
        private const val FRESHNESS_THRESHOLD_MS = 15_000L // 15 seconds
    }
}

data class RadarMeasurement(
    val connectivity: ConnectivityState,
    val locationStatus: LocationObservation,
    val point: NetworkMeasurementPoint
)
