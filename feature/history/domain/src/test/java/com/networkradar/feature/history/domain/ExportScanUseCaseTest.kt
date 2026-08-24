package com.networkradar.feature.history.domain

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isTrue
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExportScanUseCaseTest {

    private lateinit var exportScanUseCase: ExportScanUseCase
    private val pointDataSource = mockk<NetworkMeasurementPointLocalDataSource>()

    @BeforeEach
    fun setUp() {
        exportScanUseCase = ExportScanUseCase(pointDataSource)
    }

    @Test
    fun `toCsv should return valid CSV string`() = runTest {
        val point = NetworkMeasurementPoint(
            id = 1L,
            location = null,
            indoorPosition = null,
            wifi = null,
            cellular = null,
            internet = null,
            timestamp = 123456789L
        )
        coEvery { pointDataSource.getMeasurementsForSession("session1") } returns flowOf(listOf(point))

        val result = exportScanUseCase.toCsv("session1")

        assertThat(result is Result.Success).isTrue()
        val csv = (result as Result.Success).data
        assertThat(csv).contains("timestamp,latitude,longitude")
        assertThat(csv).contains("123456789")
    }

    @Test
    fun `toJson should return valid JSON string`() = runTest {
        val point = NetworkMeasurementPoint(
            id = 1L,
            location = null,
            indoorPosition = null,
            wifi = null,
            cellular = null,
            internet = null,
            timestamp = 123456789L
        )
        coEvery { pointDataSource.getMeasurementsForSession("session1") } returns flowOf(listOf(point))

        val result = exportScanUseCase.toJson("session1")

        assertThat(result is Result.Success).isTrue()
        val json = (result as Result.Success).data
        assertThat(json).contains("\"sessionId\": \"session1\"")
        assertThat(json).contains("123456789")
    }
}
