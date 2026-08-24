package com.networkradar.feature.radar.domain

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.location.Location
import com.networkradar.core.domain.location.LocationDataSource
import com.networkradar.core.domain.location.LocationObservation
import com.networkradar.core.domain.measurement.CellularDataSource
import com.networkradar.core.domain.measurement.CellularMeasurement
import com.networkradar.core.domain.measurement.ConnectivityDataSource
import com.networkradar.core.domain.measurement.WifiDataSource
import com.networkradar.core.domain.measurement.WifiMeasurement
import com.networkradar.core.domain.networking.ConnectivityState
import com.networkradar.core.domain.networking.NetworkType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObserveRadarMeasurementsUseCaseTest {

    private lateinit var useCase: ObserveRadarMeasurementsUseCase
    private val connectivityDataSource = mockk<ConnectivityDataSource>()
    private val wifiDataSource = mockk<WifiDataSource>()
    private val cellularDataSource = mockk<CellularDataSource>()
    private val locationDataSource = mockk<LocationDataSource>()
    private val indoorDataSource = mockk<IndoorDataSource>()

    private val defaultConnectivity = ConnectivityState(true, NetworkType.WIFI, true)
    private val indoorPositionFlow = MutableStateFlow<IndoorPosition?>(null)

    @BeforeEach
    fun setUp() {
        every { indoorDataSource.currentPosition } returns indoorPositionFlow
        useCase = ObserveRadarMeasurementsUseCase(
            connectivityDataSource,
            wifiDataSource,
            cellularDataSource,
            locationDataSource,
            indoorDataSource
        )
    }

    @Test
    fun `when all data is fresh, measurement point is complete`() = runTest {
        val currentTime = System.currentTimeMillis()
        val location = Location(50.0, 10.0, 5.0f, currentTime)
        val wifi = WifiMeasurement(-50, "SSID", 2400, 100, currentTime)
        val cellular = CellularMeasurement("LTE", -100, -10, 15, null, currentTime)
        val indoorPos = IndoorPosition("map_1", 5f, 5f)
        indoorPositionFlow.value = indoorPos

        every { connectivityDataSource.getConnectivityState() } returns flowOf(defaultConnectivity)
        every { wifiDataSource.getWifiMeasurement() } returns flowOf(wifi)
        every { cellularDataSource.getCellularMeasurement() } returns flowOf(cellular)
        every { locationDataSource.getLocationUpdates() } returns flowOf(LocationObservation.Success(location))

        useCase().test {
            val item = awaitItem()
            assertThat(item.point.location).isNotNull()
            assertThat(item.point.wifi).isNotNull()
            assertThat(item.point.cellular).isNotNull()
            assertThat(item.point.indoorPosition).isEqualTo(indoorPos)
        }
    }

    @Test
    fun `when location is stale, it is excluded from measurement point`() = runTest {
        val currentTime = System.currentTimeMillis()
        val staleTime = currentTime - 30_000L // 30s ago (threshold is 15s)
        val location = Location(50.0, 10.0, 5.0f, staleTime)
        val wifi = WifiMeasurement(-50, "SSID", 2400, 100, currentTime)

        every { connectivityDataSource.getConnectivityState() } returns flowOf(defaultConnectivity)
        every { wifiDataSource.getWifiMeasurement() } returns flowOf(wifi)
        every { cellularDataSource.getCellularMeasurement() } returns flowOf(null)
        every { locationDataSource.getLocationUpdates() } returns flowOf(LocationObservation.Success(location))

        useCase().test {
            val item = awaitItem()
            assertThat(item.point.location).isNull()
            assertThat(item.point.wifi).isNotNull()
        }
    }

    @Test
    fun `when wifi is stale, it is excluded from measurement point`() = runTest {
        val currentTime = System.currentTimeMillis()
        val staleTime = currentTime - 30_000L
        val location = Location(50.0, 10.0, 5.0f, currentTime)
        val wifi = WifiMeasurement(-50, "SSID", 2400, 100, staleTime)

        every { connectivityDataSource.getConnectivityState() } returns flowOf(defaultConnectivity)
        every { wifiDataSource.getWifiMeasurement() } returns flowOf(wifi)
        every { cellularDataSource.getCellularMeasurement() } returns flowOf(null)
        every { locationDataSource.getLocationUpdates() } returns flowOf(LocationObservation.Success(location))

        useCase().test {
            val item = awaitItem()
            assertThat(item.point.location).isNotNull()
            assertThat(item.point.wifi).isNull()
        }
    }

    @Test
    fun `when location is missing, point location is null but radio data preserved if fresh`() = runTest {
        val currentTime = System.currentTimeMillis()
        val wifi = WifiMeasurement(-50, "SSID", 2400, 100, currentTime)

        every { connectivityDataSource.getConnectivityState() } returns flowOf(defaultConnectivity)
        every { wifiDataSource.getWifiMeasurement() } returns flowOf(wifi)
        every { cellularDataSource.getCellularMeasurement() } returns flowOf(null)
        every { locationDataSource.getLocationUpdates() } returns flowOf(LocationObservation.Unavailable)

        useCase().test {
            val item = awaitItem()
            assertThat(item.point.location).isNull()
            assertThat(item.locationStatus).isEqualTo(LocationObservation.Unavailable)
            assertThat(item.point.wifi).isNotNull()
        }
    }
}
