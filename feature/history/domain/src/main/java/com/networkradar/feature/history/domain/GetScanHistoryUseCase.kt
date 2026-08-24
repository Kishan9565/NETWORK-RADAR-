package com.networkradar.feature.history.domain

import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import kotlinx.coroutines.flow.Flow

class GetScanHistoryUseCase(
    private val scanSessionDataSource: ScanSessionLocalDataSource
) {
    operator fun invoke(): Flow<List<ScanSession>> {
        return scanSessionDataSource.getAllSessions()
    }
}
