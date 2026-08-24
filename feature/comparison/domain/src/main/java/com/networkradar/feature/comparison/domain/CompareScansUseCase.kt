package com.networkradar.feature.comparison.domain

import com.networkradar.core.domain.measurement.*
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.first

class CompareScansUseCase(
    private val scanSessionDataSource: ScanSessionLocalDataSource,
    private val pointDataSource: NetworkMeasurementPointLocalDataSource
) {
    suspend operator fun invoke(idA: String, idB: String): Result<ScanComparison, DataError.Local> {
        return try {
            val sessionA = scanSessionDataSource.getSessionById(idA).let { (it as? Result.Success)?.data }
            val sessionB = scanSessionDataSource.getSessionById(idB).let { (it as? Result.Success)?.data }

            if (sessionA == null || sessionB == null) {
                return Result.Error(DataError.Local.NOT_FOUND)
            }

            val pointsA = pointDataSource.getMeasurementsForSession(idA).first()
            val pointsB = pointDataSource.getMeasurementsForSession(idB).first()

            val statsA = IntelligenceEngine.analyze(pointsA).statistics
            val statsB = IntelligenceEngine.analyze(pointsB).statistics

            val metrics = NetworkMetric.entries.mapNotNull { metric ->
                val valA = statsA[metric]?.average
                val valB = statsB[metric]?.average

                if (valA != null && valB != null) {
                    metric to calculateComparison(metric, valA, valB)
                } else null
            }.toMap()

            Result.Success(
                ScanComparison(
                    scanA = sessionA,
                    scanB = sessionB,
                    results = metrics
                )
            )
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    private fun calculateComparison(metric: NetworkMetric, a: Double, b: Double): MetricComparison {
        val diff = b - a
        val pct = if (a != 0.0) (diff / a) * 100.0 else null

        val trend = when {
            diff == 0.0 -> ComparisonTrend.UNCHANGED
            isImprovement(metric, diff) -> ComparisonTrend.IMPROVED
            else -> ComparisonTrend.WORSENED
        }

        return MetricComparison(
            valueA = a,
            valueB = b,
            difference = diff,
            percentageChange = pct,
            trend = trend
        )
    }

    private fun isImprovement(metric: NetworkMetric, diff: Double): Boolean {
        return when (metric) {
            NetworkMetric.DOWNLOAD,
            NetworkMetric.UPLOAD,
            NetworkMetric.WIFI_RSSI,
            NetworkMetric.CELLULAR_RSRP -> diff > 0
            NetworkMetric.LATENCY -> diff < 0
        }
    }
}
