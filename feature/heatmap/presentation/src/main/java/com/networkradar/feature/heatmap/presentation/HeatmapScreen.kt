package com.networkradar.feature.heatmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.feature.heatmap.domain.HeatmapMetric
import com.networkradar.feature.heatmap.presentation.components.HeatmapCanvas
import org.koin.androidx.compose.koinViewModel

@Composable
fun HeatmapRoot(
    sessionId: String,
    viewModel: HeatmapViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.onAction(HeatmapAction.LoadSession(sessionId))
    }

    HeatmapScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    state: HeatmapState,
    onAction: (HeatmapAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spatial Signal Map") },
                actions = {
                    IconButton(onClick = { onAction(HeatmapAction.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Metric Selector
            ScrollableTabRow(
                selectedTabIndex = state.selectedMetric.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                HeatmapMetric.entries.forEach { metric ->
                    Tab(
                        selected = state.selectedMetric == metric,
                        onClick = { onAction(HeatmapAction.SelectMetric(metric)) },
                        text = { 
                            Text(
                                text = getMetricLabel(metric),
                                style = MaterialTheme.typography.labelLarge
                            ) 
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    state.error != null -> {
                        HeatmapEmptyState(
                            title = "Heatmap Unavailable",
                            description = state.error,
                            icon = Icons.Default.Info
                        )
                    }
                    state.activeMap != null && (state.heatmapCells.isNotEmpty() || state.sourcePoints.isNotEmpty()) -> {
                        HeatmapCanvas(
                            indoorMap = state.activeMap,
                            cells = state.heatmapCells,
                            sourcePoints = state.sourcePoints,
                            metric = state.selectedMetric,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        HeatmapEmptyState(
                            title = "No Data",
                            description = "No spatial measurements were found for this session.",
                            icon = Icons.Default.Info
                        )
                    }
                }
            }

            // Legend & Info
            HeatmapLegend(
                metric = state.selectedMetric,
                range = state.metricRange
            )
        }
    }
}

@Composable
private fun HeatmapEmptyState(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeatmapLegend(
    metric: HeatmapMetric,
    range: MetricRange?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${getMetricLabel(metric)} (${getMetricUnit(metric)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (metric.isHigherBetter()) "Poor → Good" else "Good → Poor",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
                        ),
                        shape = MaterialTheme.shapes.small
                    )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val minLabel = range?.let { "%.1f".format(if (metric.isHigherBetter()) it.min else it.max) } ?: "Min"
                val maxLabel = range?.let { "%.1f".format(if (metric.isHigherBetter()) it.max else it.min) } ?: "Max"

                Text(text = minLabel, style = MaterialTheme.typography.labelSmall)
                Text(text = maxLabel, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, shape = MaterialTheme.shapes.small)
                        .padding(1.dp)
                        .background(Color.Black, shape = MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "● Real Measurement Point",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getMetricLabel(metric: HeatmapMetric): String = when(metric) {
    HeatmapMetric.DOWNLOAD -> "Download Speed"
    HeatmapMetric.UPLOAD -> "Upload Speed"
    HeatmapMetric.LATENCY -> "Latency"
    HeatmapMetric.WIFI_RSSI -> "Wi-Fi RSSI"
    HeatmapMetric.CELLULAR_RSRP -> "Cellular RSRP"
    HeatmapMetric.CELLULAR_RSRQ -> "Cellular RSRQ"
    HeatmapMetric.CELLULAR_SINR -> "Cellular SINR"
}

private fun getMetricUnit(metric: HeatmapMetric): String = when(metric) {
    HeatmapMetric.DOWNLOAD, HeatmapMetric.UPLOAD -> "Mbps"
    HeatmapMetric.LATENCY -> "ms"
    HeatmapMetric.WIFI_RSSI, HeatmapMetric.CELLULAR_RSRP -> "dBm"
    HeatmapMetric.CELLULAR_RSRQ -> "dB"
    HeatmapMetric.CELLULAR_SINR -> "dB"
}
