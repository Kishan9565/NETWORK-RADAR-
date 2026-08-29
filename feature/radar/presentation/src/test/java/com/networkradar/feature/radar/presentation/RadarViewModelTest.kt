package com.networkradar.feature.radar.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorMapLocalDataSource
import com.networkradar.core.domain.location.LocationObservation
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.ScanManager
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.networking.ConnectivityState
import com.networkradar.core.domain.util.Result
import com.networkradar.feature.radar.domain.AnalyzeScanUseCase
import com.networkradar.feature.radar.domain.ObserveRadarMeasurementsUseCase
import com.networkradar.feature.radar.domain.RadarMeasurement
import com.networkradar.feature.speedtest.domain.RunDownloadTestUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModelTest {

    private lateinit var viewModel: RadarViewModel
    private val observeRadarMeasurementsUseCase = mockk<ObserveRadarMeasurementsUseCase>()
    private val analyzeScanUseCase = mockk<AnalyzeScanUseCase>()
    private val runDownloadTestUseCase = mockk<RunDownloadTestUseCase>()
    private val scanManager = mockk<ScanManager>()
    private val indoorDataSource = mockk<IndoorDataSource>()
    private val mapDataSource = mockk<IndoorMapLocalDataSource>()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private val activeSessionFlow = MutableStateFlow<ScanSession?>(null)
    private val activeMapFlow = MutableStateFlow<IndoorMap?>(null)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        every { observeRadarMeasurementsUseCase() } returns flowOf(
            RadarMeasurement(
                connectivity = ConnectivityState(ConnectivityState.NetworkType.WIFI, true),
                locationStatus = LocationObservation.Unavailable,
                point = NetworkMeasurementPoint(null, null, null, null, null, 123L)
            )
        )
        every { scanManager.activeSession } returns activeSessionFlow
        every { indoorDataSource.activeMap } returns activeMapFlow
        every { indoorDataSource.setActiveMap(any()) } returns Unit
        
        viewModel = RadarViewModel(
            observeRadarMeasurementsUseCase,
            analyzeScanUseCase,
            runDownloadTestUseCase,
            scanManager,
            indoorDataSource,
            mapDataSource
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `StartQuickScan starts scan with null mapId`() = runTest {
        coEvery { scanManager.startScan(any(), any()) } returns Result.Success(mockk())
        
        viewModel.onAction(RadarAction.StartQuickScan("Quick"))
        testDispatcher.scheduler.runCurrent()
        
        coVerify { scanManager.startScan("Quick", null) }
    }

    @Test
    fun `StartSpatialScan with valid map starts scan with mapId`() = runTest {
        val map = IndoorMap("map-1", "Office", "", 10.0, 10.0, 1.0)
        coEvery { mapDataSource.getMapById("map-1") } returns Result.Success(map)
        coEvery { scanManager.startScan(any(), any()) } returns Result.Success(mockk())
        
        viewModel.onAction(RadarAction.StartSpatialScan("Spatial", "map-1"))
        testDispatcher.scheduler.runCurrent()
        
        verify { indoorDataSource.setActiveMap(map) }
        coVerify { scanManager.startScan("Spatial", "map-1") }
    }

    @Test
    fun `ObserveRadarMeasurementsUseCase is called only once`() = runTest {

        verify(exactly = 1) { observeRadarMeasurementsUseCase() }
    }
}
