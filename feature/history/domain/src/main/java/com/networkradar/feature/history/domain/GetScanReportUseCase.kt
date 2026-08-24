package com.networkradar.feature.history.domain

import com.networkradar.core.domain.measurement.IntelligenceEngine
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.first

class GetScanReportUseCase(
    private val pointDataSource: NetworkMeasurementPointLocalDataSource
) {
    suspend operator fun invoke(sessionId: String): Result<ScanIntelligenceSummary, DataError.Local> {
        return try {
            val points = pointDataSource.getMeasurementsForSession(sessionId).first()
            val summary = IntelligenceEngine.analyze(points)
            Result.Success(summary)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
