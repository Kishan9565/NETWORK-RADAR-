package com.networkradar.feature.heatmap.domain

import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.measurement.InternetMeasurement
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GenerateHeatmapUseCaseTest {

    private lateinit var useCase: GenerateHeatmapUseCase
    private val measurementDataSource = mockk<NetworkMeasurementPointLocalDataSource>()

    private val map = IndoorMap("map1", "Office", 10f, 10f, 0L)

    @BeforeEach
    fun setUp() {
        useCase = GenerateHeatmapUseCase(measurementDataSource)
    }

    @Test
    fun `invoke should return empty result when no measurements exist`() = runTest {
        coEvery { measurementDataSource.getMeasurementsForSession("session1") } returns flowOf(emptyList())

        val result = useCase("session1", map, HeatmapMetric.DOWNLOAD)

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.cells.isEmpty())
    }

    @Test
    fun `invoke should filter points from other maps`() = runTest {
        val points = listOf(
            createPoint(mapId = "map2", download = 50.0), // Wrong map
            createPoint(mapId = "map1", download = 100.0) // Correct map
        )
        coEvery { measurementDataSource.getMeasurementsForSession("session1") } returns flowOf(points)

        val result = useCase("session1", map, HeatmapMetric.DOWNLOAD)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.sourcePoints.size)
        assertEquals(100.0, data.sourcePoints.first().value)
    }

    @Test
    fun `invoke should ignore points without the selected metric`() = runTest {
        val points = listOf(
            createPoint(mapId = "map1", download = null), // No download
            createPoint(mapId = "map1", download = 50.0)
        )
        coEvery { measurementDataSource.getMeasurementsForSession("session1") } returns flowOf(points)

        val result = useCase("session1", map, HeatmapMetric.DOWNLOAD)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.sourcePoints.size)
    }

    private fun createPoint(
        mapId: String,
        download: Double? = null,
        x: Float = 0f,
        y: Float = 0f
    ) = NetworkMeasurementPoint(
        id = 1L,
        location = null,
        indoorPosition = IndoorPosition(mapId, x, y),
        wifi = null,
        cellular = null,
        internet = InternetMeasurement(null, download, null, 0L),
        timestamp = 0L
    )
}
