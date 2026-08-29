package com.networkradar.core.data.measurement

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import com.networkradar.core.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RealScanManagerTest {

    private lateinit var scanManager: RealScanManager
    private val sessionDataSource = mockk<ScanSessionLocalDataSource>()
    private val measurementDataSource = mockk<NetworkMeasurementPointLocalDataSource>()

    @BeforeEach
    fun setUp() {
        scanManager = RealScanManager(sessionDataSource, measurementDataSource)
    }

    @Test
    fun `startScan should create and persist a new session`() = runTest {
        coEvery { sessionDataSource.startSession(any()) } returns Result.Success(Unit)

        val result = scanManager.startScan("Test Scan", "map-1")

        assertThat(result is Result.Success).isEqualTo(true)
        val session = (result as Result.Success).data
        assertThat(session.name).isEqualTo("Test Scan")
        assertThat(session.mapId).isEqualTo("map-1")
        assertThat(scanManager.activeSession.value).isEqualTo(session)
        
        coVerify { sessionDataSource.startSession(session) }
    }

    @Test
    fun `stopScan should end the active session`() = runTest {
        coEvery { sessionDataSource.startSession(any()) } returns Result.Success(Unit)
        coEvery { sessionDataSource.endSession(any(), any()) } returns Result.Success(Unit)

        scanManager.startScan("Test Scan")
        val session = scanManager.activeSession.value!!

        val result = scanManager.stopScan()

        assertThat(result is Result.Success).isEqualTo(true)
        assertThat(scanManager.activeSession.value).isNull()
        
        coVerify { sessionDataSource.endSession(session.id, any()) }
    }

    @Test
    fun `recordMeasurement should save point and increment count`() = runTest {
        coEvery { sessionDataSource.startSession(any()) } returns Result.Success(Unit)
        coEvery { measurementDataSource.saveMeasurementPoint(any(), any()) } returns Result.Success(Unit)
        coEvery { sessionDataSource.incrementMeasurementCount(any()) } returns Result.Success(Unit)

        scanManager.startScan("Test Scan")
        val session = scanManager.activeSession.value!!
        val point = NetworkMeasurementPoint(
            id = null,
            location = null,
            indoorPosition = null,
            wifi = null,
            cellular = null,
            internet = null,
            timestamp = 123L
        )

        val result = scanManager.recordMeasurement(point)

        assertThat(result is Result.Success).isEqualTo(true)
        
        coVerify { 
            measurementDataSource.saveMeasurementPoint(session.id, point)
            sessionDataSource.incrementMeasurementCount(session.id)
        }
        
        // Check local state increment
        assertThat(scanManager.activeSession.value?.measurementCount).isEqualTo(1)
    }

    @Test
    fun `concurrent startScan calls should not leak multiple sessions`() = runTest {
        coEvery { sessionDataSource.startSession(any()) } coAnswers {
            delay(100)
            Result.Success(Unit)
        }

        val results = awaitAll(
            async { scanManager.startScan("Scan 1") },
            async { scanManager.startScan("Scan 2") }
        )

        assertThat(results.count { it is Result.Success }).isEqualTo(1)
        assertThat(results.count { it is Result.Error }).isEqualTo(1)
    }

    @Test
    fun `concurrent recordMeasurement calls should remain consistent`() = runTest {
        coEvery { sessionDataSource.startSession(any()) } returns Result.Success(Unit)
        coEvery { measurementDataSource.saveMeasurementPoint(any(), any()) } returns Result.Success(Unit)
        coEvery { sessionDataSource.incrementMeasurementCount(any()) } coAnswers {
            delay(10)
            Result.Success(Unit)
        }

        scanManager.startScan("Test Scan")
        
        val point = NetworkMeasurementPoint(
            id = null,
            location = null,
            indoorPosition = null,
            wifi = null,
            cellular = null,
            internet = null,
            timestamp = 123L
        )
        
        val jobs = List(10) {
            async { scanManager.recordMeasurement(point) }
        }
        jobs.awaitAll()
        
        assertThat(scanManager.activeSession.value?.measurementCount).isEqualTo(10)
    }
}
