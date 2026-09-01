package com.networkradar.feature.heatmap.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.networkradar.core.domain.indoor.SpatialAnnotation
import com.networkradar.feature.heatmap.domain.HeatmapCell
import com.networkradar.feature.heatmap.domain.HeatmapEngine
import com.networkradar.feature.heatmap.domain.HeatmapMetric
import kotlin.math.abs

@Composable
fun HeatmapCanvas(
    cells: List<HeatmapCell>,
    sourcePoints: List<HeatmapEngine.WeightedPoint>,
    annotations: List<SpatialAnnotation>,
    metric: HeatmapMetric,
    modifier: Modifier = Modifier
) {
    if (cells.isEmpty() && sourcePoints.isEmpty()) return

    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        // Combine all spatial data to find bounds
        val allX = cells.map { it.x } + sourcePoints.map { it.x } + annotations.map { it.x }
        val allY = cells.map { it.y } + sourcePoints.map { it.y } + annotations.map { it.y }
        
        val minX = allX.minOrNull() ?: 0f
        val maxX = allX.maxOrNull() ?: 1f
        val minY = allY.minOrNull() ?: 0f
        val maxY = allY.maxOrNull() ?: 1f
        
        val rangeX = abs(maxX - minX).coerceAtLeast(10f)
        val rangeY = abs(maxY - minY).coerceAtLeast(10f)
        
        val scale = minOf(canvasWidth / rangeX, canvasHeight / rangeY) * 0.9f
        
        val offsetX = (canvasWidth - rangeX * scale) / 2f - minX * scale
        val offsetY = (canvasHeight - rangeY * scale) / 2f - minY * scale

        val validValues = cells.mapNotNull { it.value }
        val minVal = validValues.minOrNull() ?: 0.0
        val maxVal = validValues.maxOrNull() ?: 1.0
        val range = (maxVal - minVal).coerceAtLeast(0.001)

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw heatmap cells (Interpolated Data)
            cells.forEach { cell ->
                val cellX = cell.x * scale + offsetX
                val cellY = cell.y * scale + offsetY
                
                cell.value?.let { value ->
                    val normalized = if (metric.isHigherBetter()) {
                        (value - minVal) / range
                    } else {
                        (maxVal - value) / range
                    }.coerceIn(0.0, 1.0).toFloat()

                    val color = interpolateHeatmapColor(normalized, cell.density)
                    val cellSizePx = 1.0f * scale 
                    
                    drawRect(
                        color = color,
                        topLeft = Offset(cellX - cellSizePx / 2, cellY - cellSizePx / 2),
                        size = Size(cellSizePx, cellSizePx)
                    )
                }
            }

            // Draw source points (Real Measurements)
            sourcePoints.forEach { point ->
                val px = point.x * scale + offsetX
                val py = point.y * scale + offsetY
                
                drawCircle(
                    color = Color.Black,
                    radius = 3.dp.toPx(),
                    center = Offset(px, py),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Draw annotations (Pins)
            annotations.forEach { pin ->
                val px = pin.x * scale + offsetX
                val py = pin.y * scale + offsetY
                
                // Simplified pin marker
                drawCircle(
                    color = Color.Red,
                    radius = 6.dp.toPx(),
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}

private fun interpolateHeatmapColor(value: Float, density: Float): Color {
    // 0.0 (Cold/Blue) -> 1.0 (Hot/Red)
    val r = if (value > 0.5f) 1f else value * 2f
    val g = if (value > 0.5f) (1f - value) * 2f else value * 2f
    val b = if (value < 0.5f) 1f else (1f - value) * 2f
    
    return Color(r, g, b, alpha = density * 0.7f)
}
