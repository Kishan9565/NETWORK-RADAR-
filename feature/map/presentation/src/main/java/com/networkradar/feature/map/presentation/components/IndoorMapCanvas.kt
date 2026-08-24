package com.networkradar.feature.map.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorPosition

@Composable
fun IndoorMapCanvas(
    indoorMap: IndoorMap,
    currentPosition: IndoorPosition?,
    onPositionSelected: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        // Calculate scale to fit map while maintaining aspect ratio
        val scaleX = canvasWidth / indoorMap.width
        val scaleY = canvasHeight / indoorMap.height
        val scale = minOf(scaleX, scaleY)

        val drawWidth = indoorMap.width * scale
        val drawHeight = indoorMap.height * scale

        // Center map in canvas
        val offsetX = (canvasWidth - drawWidth) / 2
        val offsetY = (canvasHeight - drawHeight) / 2

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(indoorMap) {
                    detectTapGestures { offset ->
                        // Convert pixel coordinates to domain coordinates
                        val domainX = (offset.x - offsetX) / scale
                        val domainY = (offset.y - offsetY) / scale
                        onPositionSelected(domainX, domainY)
                    }
                }
        ) {
            // Draw map boundary
            drawRect(
                color = Color.Gray,
                topLeft = Offset(offsetX, offsetY),
                size = Size(drawWidth, drawHeight),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw current position marker
            currentPosition?.let { pos ->
                if (pos.mapId == indoorMap.id) {
                    val markerX = offsetX + pos.x * scale
                    val markerY = offsetY + pos.y * scale
                    drawCircle(
                        color = Color.Red,
                        radius = 8.dp.toPx(),
                        center = Offset(markerX, markerY)
                    )
                }
            }
        }
    }
}
