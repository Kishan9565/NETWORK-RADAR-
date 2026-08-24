package com.networkradar.feature.heatmap.domain

import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GenerateHeatmapUseCase(
    private val measurementDataSource: NetworkMeasurementPointLocalDataSource
) {
    suspend operator fun invoke(
        sessionId: String,
        map: IndoorMap,
        metric: HeatmapMetric,
        config: HeatmapEngine.HeatmapConfig = HeatmapEngine.HeatmapConfig()
    ): Result<HeatmapResult, DataError.Local> = withContext(Dispatchers.Default) {
        try {
            val points = measurementDataSource.getMeasurementsForSession(sessionId).first()
            
            val weightedPoints = points.mapNotNull { point ->
                val pos = point.indoorPosition ?: return@mapNotNull null
                if (pos.mapId != map.id) return@mapNotNull null
                
                val value = metric.extract(point) ?: return@mapNotNull null
                
                HeatmapEngine.WeightedPoint(
                    x = pos.x,
                    y = pos.y,
                    value = value
                )
            }

            if (weightedPoints.isEmpty()) {
                return@withContext Result.Success(HeatmapResult(emptyList(), emptyList()))
            }

            val cells = HeatmapEngine.generateGrid(
                width = map.width,
                height = map.height,
                points = weightedPoints,
                config = config
            )

            Result.Success(HeatmapResult(cells, weightedPoints))
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    data class HeatmapResult(
        val cells: List<HeatmapCell>,
        val sourcePoints: List<HeatmapEngine.WeightedPoint>
    )
}
