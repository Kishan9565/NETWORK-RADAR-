package com.networkradar.feature.radar.domain

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.indoor.PdrDataSource
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
    private val pdrDataSource = mockk<PdrDataSource>()

    private val defaultConnectivity = ConnectivityState(true, NetworkType.WIFI, true)
    private val indoorPositionFlow = MutableStateFlow<IndoorPosition>(IndoorPosition("", 0f, 0f, 0L))

    @BeforeEach
    fun setUp() {
        every { pdrDataSource.currentPosition } returns indoorPositionFlow
        useCase = ObserveRadarMeasurementsUseCase(
            connectivityDataSource,
            wifiDataSource,
            cellularDataSource,
            locationDataSource,
            pdrDataSource
        )
    }

    @Test
    fun `when all data is fresh, measurement point is complete`() = runTest {
        val currentTime = System.currentTimeMillis()
        val location = Location(50.0, 10.0, 5.0f, currentTime)
        val wifi = WifiMeasurement(-50, "SSID", 2400, 100, currentTime)
        val cellular = CellularMeasurement("LTE", -100, -10, 15, null, currentTime)
        val indoorPos = IndoorPosition("session_1", 5f, 5f, currentTime)
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
    fun `when indoor position timestamp is 0, it is excluded`() = runTest {
        val currentTime = System.currentTimeMillis()
        val location = Location(50.0, 10.0, 5.0f, currentTime)
        val indoorPos = IndoorPosition("session_1", 5f, 5f, 0L)
        indoorPositionFlow.value = indoorPos

        every { connectivityDataSource.getConnectivityState() } returns flowOf(defaultConnectivity)
        every { wifiDataSource.getWifiMeasurement() } returns flowOf(null)
        every { cellularDataSource.getCellularMeasurement() } returns flowOf(null)
        every { locationDataSource.getLocationUpdates() } returns flowOf(LocationObservation.Success(location))

        useCase().test {
            val item = awaitItem()
            assertThat(item.point.indoorPosition).isNull()
        }
    }
}
