package com.networkradar.feature.radar.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.indoor.PdrDataSource
import com.networkradar.core.domain.indoor.SpatialAnnotationLocalDataSource
import com.networkradar.core.domain.location.LocationObservation
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.ScanManager
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.networking.ConnectivityState
import com.networkradar.core.domain.networking.NetworkType
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
    private val pdrDataSource = mockk<PdrDataSource>()
    private val annotationDataSource = mockk<SpatialAnnotationLocalDataSource>()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private val activeSessionFlow = MutableStateFlow<ScanSession?>(null)
    private val pdrPositionFlow = MutableStateFlow<IndoorPosition?>(null)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        every { observeRadarMeasurementsUseCase() } returns flowOf(
            RadarMeasurement(
                connectivity = ConnectivityState(isConnected = true, networkType = NetworkType.WIFI),
                locationStatus = LocationObservation.Unavailable,
                point = NetworkMeasurementPoint(null, null, null, null, null, null, 123L)
            )
        )
        every { scanManager.activeSession } returns activeSessionFlow
        coEvery { scanManager.recordMeasurement(any()) } returns Result.Success(Unit)

        every { pdrDataSource.currentPosition } returns pdrPositionFlow
        every { pdrDataSource.isAvailable } returns true
        every { pdrDataSource.startTracking(any()) } returns Unit
        every { pdrDataSource.stopTracking() } returns Unit
        every { pdrDataSource.resetOrigin() } returns Unit
        
        viewModel = RadarViewModel(
            observeRadarMeasurementsUseCase,
            analyzeScanUseCase,
            runDownloadTestUseCase,
            scanManager,
            pdrDataSource,
            annotationDataSource
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `StartQuickScan starts scan with isSpatial false`() = runTest {
        coEvery { scanManager.startScan(any(), any()) } returns Result.Success(mockk())
        
        viewModel.onAction(RadarAction.StartQuickScan("Quick"))
        testDispatcher.scheduler.runCurrent()
        
        coVerify { scanManager.startScan("Quick", isSpatial = false) }
    }

    @Test
    fun `StartSpatialScan starts scan with isSpatial true and starts PDR tracking`() = runTest {
        val session = ScanSession("session-1", true, "Spatial", 0L, null, 0)
        coEvery { scanManager.startScan(any(), any()) } returns Result.Success(session)
        
        viewModel.onAction(RadarAction.StartSpatialScan("Spatial"))
        testDispatcher.scheduler.runCurrent()
        
        coVerify { scanManager.startScan("Spatial", isSpatial = true) }
        verify { pdrDataSource.startTracking("session-1") }
    }

    @Test
    fun `StopScan stops PDR tracking`() = runTest {
        activeSessionFlow.value = ScanSession("session-1", true, "Spatial", 0L, null, 10)
        coEvery { scanManager.stopScan() } returns Result.Success(Unit)
        coEvery { analyzeScanUseCase(any()) } returns Result.Success(mockk())
        
        viewModel.onAction(RadarAction.StopScan)
        testDispatcher.scheduler.runCurrent()
        
        verify { pdrDataSource.stopTracking() }
        coVerify { scanManager.stopScan() }
    }

    @Test
    fun `RecalibratePosition calls pdrDataSource resetOrigin`() = runTest {
        viewModel.onAction(RadarAction.RecalibratePosition)
        testDispatcher.scheduler.runCurrent()
        
        verify { pdrDataSource.resetOrigin() }
    }
}
