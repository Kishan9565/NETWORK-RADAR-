package com.networkradar.feature.heatmap.domain

import kotlin.math.pow
import kotlin.math.sqrt

object HeatmapEngine {

    fun generateGrid(
        width: Float,
        height: Float,
        points: List<WeightedPoint>,
        config: HeatmapConfig
    ): List<HeatmapCell> {
        if (points.isEmpty()) return emptyList()

        val cells = mutableListOf<HeatmapCell>()
        val xSteps = (width / config.cellSize).toInt()
        val ySteps = (height / config.cellSize).toInt()

        for (i in 0..xSteps) {
            for (j in 0..ySteps) {
                val x = i * config.cellSize
                val y = j * config.cellSize
                
                val (value, density) = calculateIdw(x, y, points, config)
                cells.add(HeatmapCell(x, y, value, density))
            }
        }
        return cells
    }

    private fun calculateIdw(
        x: Float,
        y: Float,
        points: List<WeightedPoint>,
        config: HeatmapConfig
    ): Pair<Double?, Float> {
        var weightedSum = 0.0
        var weightSum = 0.0
        var pointsInRadius = 0

        for (point in points) {
            val dist = sqrt((x - point.x).toDouble().pow(2) + (y - point.y).toDouble().pow(2))
            
            if (dist < 0.001) {
                return point.value to 1.0f
            }

            if (dist <= config.maxRadius) {
                val weight = 1.0 / dist.pow(config.power)
                weightedSum += point.value * weight
                weightSum += weight
                pointsInRadius++
            }
        }

        if (pointsInRadius < config.minPoints) {
            return null to (pointsInRadius.toFloat() / config.minPoints).coerceAtMost(1.0f)
        }

        val interpolatedValue = weightedSum / weightSum
        val density = (pointsInRadius.toFloat() / points.size.coerceAtLeast(1)).coerceAtMost(1.0f)
        
        return interpolatedValue to density
    }

    data class WeightedPoint(
        val x: Float,
        val y: Float,
        val value: Double
    )

    data class HeatmapConfig(
        val cellSize: Float = 1.0f,
        val power: Double = 2.0,
        val maxRadius: Double = 10.0,
        val minPoints: Int = 1
    )
}
