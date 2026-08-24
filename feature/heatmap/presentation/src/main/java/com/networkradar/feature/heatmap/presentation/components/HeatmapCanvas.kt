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
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.feature.heatmap.domain.HeatmapCell
import com.networkradar.feature.heatmap.domain.HeatmapEngine
import com.networkradar.feature.heatmap.domain.HeatmapMetric

@Composable
fun HeatmapCanvas(
    indoorMap: IndoorMap,
    cells: List<HeatmapCell>,
    sourcePoints: List<HeatmapEngine.WeightedPoint>,
    metric: HeatmapMetric,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        val scaleX = canvasWidth / indoorMap.width
        val scaleY = canvasHeight / indoorMap.height
        val scale = minOf(scaleX, scaleY)

        val drawWidth = indoorMap.width * scale
        val drawHeight = indoorMap.height * scale

        val offsetX = (canvasWidth - drawWidth) / 2
        val offsetY = (canvasHeight - drawHeight) / 2

        val validValues = cells.mapNotNull { it.value }
        val minVal = validValues.minOrNull() ?: 0.0
        val maxVal = validValues.maxOrNull() ?: 1.0
        val range = (maxVal - minVal).coerceAtLeast(0.001)

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw map boundary
            drawRect(
                color = Color.Gray,
                topLeft = Offset(offsetX, offsetY),
                size = Size(drawWidth, drawHeight),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw heatmap cells (Interpolated Data)
            cells.forEach { cell ->
                val cellX = offsetX + cell.x * scale
                val cellY = offsetY + cell.y * scale
                
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
                val px = offsetX + point.x * scale
                val py = offsetY + point.y * scale
                
                // Draw a small distinct marker for real measurement locations
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
