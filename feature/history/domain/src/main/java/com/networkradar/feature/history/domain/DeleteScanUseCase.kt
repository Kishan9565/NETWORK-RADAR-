package com.networkradar.feature.history.domain

import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result

class DeleteScanUseCase(
    private val scanSessionDataSource: ScanSessionLocalDataSource
) {
    suspend operator fun invoke(sessionId: String): Result<Unit, DataError.Local> {
        return scanSessionDataSource.deleteSession(sessionId)
    }
}
