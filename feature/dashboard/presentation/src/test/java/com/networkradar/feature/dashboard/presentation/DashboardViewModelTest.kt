package com.networkradar.feature.dashboard.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
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
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private val sessionDataSource = mockk<ScanSessionLocalDataSource>()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionDataSource.getAllSessions() } returns flowOf(emptyList())
        viewModel = DashboardViewModel(sessionDataSource)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel.state.test {
            // Depending on how stateIn is initialized, we might see the initial state first
            val item = awaitItem()
            // The stateIn initialValue is DashboardState(isLoading = true)
            // But if the flow emits immediately, we might skip to the mapped value
            assertThat(item.isLoading).isEqualTo(false)
        }
    }
}
