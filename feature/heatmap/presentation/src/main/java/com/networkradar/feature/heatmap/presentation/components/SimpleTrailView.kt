package com.networkradar.feature.heatmap.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.networkradar.core.designsystem.*
import com.networkradar.core.domain.indoor.SpatialAnnotation
import com.networkradar.core.domain.measurement.SignalQuality
import com.networkradar.core.domain.measurement.SignalQualityStrategy
import com.networkradar.feature.heatmap.domain.HeatmapEngine
import com.networkradar.feature.heatmap.domain.HeatmapMetric
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun SimpleTrailView(
    sourcePoints: List<HeatmapEngine.WeightedPoint>,
    annotations: List<SpatialAnnotation>,
    metric: HeatmapMetric,
    modifier: Modifier = Modifier
) {
    var selectedPoint by remember { mutableStateOf<HeatmapEngine.WeightedPoint?>(null) }
    var selectedAnnotation by remember { mutableStateOf<SpatialAnnotation?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    if (sourcePoints.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Not enough of a walk recorded yet — try a longer Spatial Scan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        val allX = sourcePoints.map { it.x } + annotations.map { it.x }
        val allY = sourcePoints.map { it.y } + annotations.map { it.y }
        
        val minX = (allX.minOrNull() ?: 0f)
        val maxX = (allX.maxOrNull() ?: 0f)
        val minY = (allY.minOrNull() ?: 0f)
        val maxY = (allY.maxOrNull() ?: 0f)
        
        val rangeX = abs(maxX - minX).coerceAtLeast(10f)
        val rangeY = abs(maxY - minY).coerceAtLeast(10f)
        
        val scale = minOf(canvasWidth / rangeX, canvasHeight / rangeY) * 0.8f
        
        val offsetX = (canvasWidth - rangeX * scale) / 2f - minX * scale
        val offsetY = (canvasHeight - rangeY * scale) / 2f - minY * scale

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sourcePoints, annotations) {
                    detectTapGestures { offset ->
                        // Hit detection for annotations first
                        val tappedAnnotation = annotations.find { pin ->
                            val px = pin.x * scale + offsetX
                            val py = pin.y * scale + offsetY
                            val dist = sqrt((px - offset.x).pow(2) + (py - offset.y).pow(2))
                            dist < with(density) { 24.dp.toPx() }
                        }
                        
                        if (tappedAnnotation != null) {
                            selectedAnnotation = tappedAnnotation
                            selectedPoint = sourcePoints.minByOrNull { p ->
                                sqrt((p.x - tappedAnnotation.x).pow(2) + (p.y - tappedAnnotation.y).pow(2))
                            }
                            showDetailDialog = true
                            return@detectTapGestures
                        }

                        // Hit detection for points
                        val tappedPoint = sourcePoints.find { point ->
                            val px = point.x * scale + offsetX
                            val py = point.y * scale + offsetY
                            val dist = sqrt((px - offset.x).pow(2) + (py - offset.y).pow(2))
                            dist < with(density) { 20.dp.toPx() }
                        }

                        if (tappedPoint != null) {
                            selectedPoint = tappedPoint
                            selectedAnnotation = null
                            showDetailDialog = true
                        }
                    }
                }
        ) {
            // Draw path line
            val path = Path().apply {
                sourcePoints.forEachIndexed { index, point ->
                    val px = point.x * scale + offsetX
                    val py = point.y * scale + offsetY
                    if (index == 0) moveTo(px, py) else lineTo(px, py)
                }
            }
            drawPath(
                path = path,
                color = NeutralGray.copy(alpha = 0.3f),
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw points
            sourcePoints.forEach { point ->
                val px = point.x * scale + offsetX
                val py = point.y * scale + offsetY
                val quality = getSignalQuality(point.value, metric)
                
                drawCircle(
                    color = getQualityColor(quality),
                    radius = 4.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Draw start marker
            sourcePoints.firstOrNull()?.let { start ->
                val px = start.x * scale + offsetX
                val py = start.y * scale + offsetY
                
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(px, py),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Draw end marker
            sourcePoints.lastOrNull()?.let { end ->
                val px = end.x * scale + offsetX
                val py = end.y * scale + offsetY
                
                drawCircle(
                    color = ElectricCyan,
                    radius = 8.dp.toPx(),
                    center = Offset(px, py),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Draw pins
            annotations.forEach { pin ->
                val px = pin.x * scale + offsetX
                val py = pin.y * scale + offsetY
                
                drawCircle(
                    color = SignalRed,
                    radius = 10.dp.toPx(),
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // Floating Labels for Start/End
        sourcePoints.firstOrNull()?.let { start ->
            val px = (start.x * scale + offsetX)
            val py = (start.y * scale + offsetY)
            Label(
                text = "Started here",
                modifier = Modifier.offset(
                    x = with(density) { px.toDp() } - 40.dp,
                    y = with(density) { py.toDp() } + 12.dp
                )
            )
        }
        
        sourcePoints.lastOrNull()?.let { end ->
            val px = (end.x * scale + offsetX)
            val py = (end.y * scale + offsetY)
            Label(
                text = "Stopped here",
                modifier = Modifier.offset(
                    x = with(density) { px.toDp() } - 40.dp,
                    y = with(density) { py.toDp() } + 12.dp
                )
            )
        }
    }

    if (showDetailDialog) {
        DetailDialog(
            point = selectedPoint,
            annotation = selectedAnnotation,
            metric = metric,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = CircleShape,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DetailDialog(
    point: HeatmapEngine.WeightedPoint?,
    annotation: SpatialAnnotation?,
    metric: HeatmapMetric,
    onDismiss: () -> Unit
) {
    var showTechnical by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = annotation?.label ?: "Spot Detail")
        },
        text = {
            Column {
                val quality = point?.let { getSignalQuality(it.value, metric) }
                val qualityText = when(quality) {
                    SignalQuality.EXCELLENT -> "Signal here: Excellent"
                    SignalQuality.GOOD -> "Signal here: Good"
                    SignalQuality.FAIR -> "Signal here: Fair"
                    SignalQuality.POOR -> "Signal here: Poor"
                    else -> "Signal here: Unavailable"
                }
                
                Text(
                    text = qualityText,
                    style = MaterialTheme.typography.titleMedium,
                    color = quality?.let { getQualityColor(it) } ?: MaterialTheme.colorScheme.onSurface
                )
                
                if (annotation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This is a marked spot you saved during the scan.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { showTechnical = !showTechnical },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (showTechnical) "Hide technical details" else "Show technical details")
                }
                
                if (showTechnical && point != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${getMetricLabel(metric)}: ${"%.1f".format(point.value)} ${getMetricUnit(metric)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

private fun getSignalQuality(value: Double, metric: HeatmapMetric): SignalQuality {
    return when (metric) {
        HeatmapMetric.WIFI_RSSI -> SignalQualityStrategy.getWifiQuality(value.toInt())
        HeatmapMetric.CELLULAR_RSRP -> SignalQualityStrategy.getCellularQuality(value.toInt())
        else -> if (value > 0) SignalQuality.GOOD else SignalQuality.POOR
    }
}

private fun getQualityColor(quality: SignalQuality): Color {
    return when (quality) {
        SignalQuality.EXCELLENT -> SignalExcellent
        SignalQuality.GOOD -> SignalGood
        SignalQuality.FAIR -> SignalFair
        SignalQuality.POOR -> SignalPoor
        else -> SignalUnavailable
    }
}

private fun getMetricLabel(metric: HeatmapMetric): String = when(metric) {
    HeatmapMetric.WIFI_RSSI -> "Wi-Fi RSSI"
    HeatmapMetric.CELLULAR_RSRP -> "Cellular RSRP"
    HeatmapMetric.CELLULAR_RSRQ -> "Cellular RSRQ"
    HeatmapMetric.CELLULAR_SINR -> "Cellular SINR"
}

private fun getMetricUnit(metric: HeatmapMetric): String = when(metric) {
    HeatmapMetric.WIFI_RSSI, HeatmapMetric.CELLULAR_RSRP -> "dBm"
    HeatmapMetric.CELLULAR_RSRQ -> "dB"
    HeatmapMetric.CELLULAR_SINR -> "dB"
}
