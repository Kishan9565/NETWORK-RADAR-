package com.networkradar.feature.comparison.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.networkradar.core.domain.measurement.*
import com.networkradar.core.domain.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CompareScansUseCaseTest {

    private lateinit var compareScansUseCase: CompareScansUseCase
    private val scanSessionDataSource = mockk<ScanSessionLocalDataSource>()
    private val pointDataSource = mockk<NetworkMeasurementPointLocalDataSource>()

    @BeforeEach
    fun setUp() {
        compareScansUseCase = CompareScansUseCase(scanSessionDataSource, pointDataSource)
    }

    @Test
    fun `invoke should return comparison result for valid scans`() = runTest {
        val sessionA = ScanSession("1", null, "Scan 1", 1000L, 2000L, 1)
        val sessionB = ScanSession("2", null, "Scan 2", 3000L, 4000L, 1)
        
        coEvery { scanSessionDataSource.getSessionById("1") } returns Result.Success(sessionA)
        coEvery { scanSessionDataSource.getSessionById("2") } returns Result.Success(sessionB)
        
        val pointA = createPoint(download = 50.0)
        val pointB = createPoint(download = 100.0)
        
        coEvery { pointDataSource.getMeasurementsForSession("1") } returns flowOf(listOf(pointA))
        coEvery { pointDataSource.getMeasurementsForSession("2") } returns flowOf(listOf(pointB))

        val result = compareScansUseCase("1", "2")

        assertThat(result is Result.Success).isTrue()
        val comparison = (result as Result.Success).data
        assertThat(comparison.scanA).isEqualTo(sessionA)
        assertThat(comparison.scanB).isEqualTo(sessionB)
        
        val downloadComp = comparison.results[NetworkMetric.DOWNLOAD]
        assertThat(downloadComp).isNotNull()
        assertThat(downloadComp?.valueA).isEqualTo(50.0)
        assertThat(downloadComp?.valueB).isEqualTo(100.0)
        assertThat(downloadComp?.difference).isEqualTo(50.0)
        assertThat(downloadComp?.trend).isEqualTo(ComparisonTrend.IMPROVED)
    }

    private fun createPoint(download: Double) = NetworkMeasurementPoint(
        id = 1L,
        location = null,
        indoorPosition = null,
        wifi = null,
        cellular = null,
        internet = InternetMeasurement(null, download, null, 0L),
        timestamp = 0L
    )
}
