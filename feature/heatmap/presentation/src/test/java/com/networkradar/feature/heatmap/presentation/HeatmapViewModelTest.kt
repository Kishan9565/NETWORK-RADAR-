package com.networkradar.feature.heatmap.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.networkradar.core.domain.indoor.SpatialAnnotationLocalDataSource
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import com.networkradar.core.domain.util.Result
import com.networkradar.feature.heatmap.domain.GenerateHeatmapUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeatmapViewModelTest {

    private lateinit var viewModel: HeatmapViewModel
    private val sessionDataSource = mockk<ScanSessionLocalDataSource>()
    private val annotationDataSource = mockk<SpatialAnnotationLocalDataSource>()
    private val generateHeatmapUseCase = mockk<GenerateHeatmapUseCase>()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HeatmapViewModel(sessionDataSource, annotationDataSource, generateHeatmapUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `LoadSession with missing session shows error`() = runTest {
        coEvery { sessionDataSource.getSessionById("session-1") } returns Result.Success(null)

        viewModel.onAction(HeatmapAction.LoadSession("session-1"))
        testDispatcher.scheduler.runCurrent()

        viewModel.state.test {
            val state = awaitItem()
            assertThat(state.error).isEqualTo("Scan session not found.")
            assertThat(state.isLoading).isEqualTo(false)
        }
    }

    @Test
    fun `LoadSession with non-spatial session shows error`() = runTest {
        val session = ScanSession("session-1", false, "Quick Scan", 0L, null, 0)
        coEvery { sessionDataSource.getSessionById("session-1") } returns Result.Success(session)

        viewModel.onAction(HeatmapAction.LoadSession("session-1"))
        testDispatcher.scheduler.runCurrent()

        viewModel.state.test {
            val state = awaitItem()
            assertThat(state.error).isEqualTo("This was a Quick Scan — no spatial data to map.")
            assertThat(state.isLoading).isEqualTo(false)
        }
    }

    @Test
    fun `LoadSession with valid data triggers heatmap generation`() = runTest {
        val session = ScanSession("session-1", true, "Spatial Scan", 0L, null, 10)
        coEvery { sessionDataSource.getSessionById("session-1") } returns Result.Success(session)
        every { annotationDataSource.getAnnotationsForSession("session-1") } returns flowOf(emptyList())
        coEvery { generateHeatmapUseCase(any(), any(), any()) } returns Result.Success(
            GenerateHeatmapUseCase.HeatmapResult(emptyList(), emptyList())
        )

        viewModel.onAction(HeatmapAction.LoadSession("session-1"))
        testDispatcher.scheduler.runCurrent()

        viewModel.state.test {
            val state = awaitItem()
            assertThat(state.selectedSessionId).isEqualTo("session-1")
            // error is set because HeatmapResult was empty in this mock
            assertThat(state.error).isEqualTo("Not enough movement recorded to build a heatmap.")
        }
    }
}
