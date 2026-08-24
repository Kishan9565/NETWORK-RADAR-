package com.networkradar.feature.history.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetScanHistoryUseCaseTest {

    private lateinit var getScanHistoryUseCase: GetScanHistoryUseCase
    private val scanSessionDataSource = mockk<ScanSessionLocalDataSource>()

    @BeforeEach
    fun setUp() {
        getScanHistoryUseCase = GetScanHistoryUseCase(scanSessionDataSource)
    }

    @Test
    fun `invoke should return sessions from data source`() = runTest {
        val sessions = listOf(
            ScanSession("1", null, "Scan 1", 1000L, 2000L, 10),
            ScanSession("2", null, "Scan 2", 3000L, 4000L, 20)
        )
        every { scanSessionDataSource.getAllSessions() } returns flowOf(sessions)

        val result = getScanHistoryUseCase().first()

        assertThat(result).isEqualTo(sessions)
    }
}
