package com.networkradar.feature.heatmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                title = { Text("Network Heatmap") },
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
                        text = { Text(metric.name.replace("_", " ")) }
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
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else if (state.activeMap != null && state.heatmapCells.isNotEmpty()) {
                    HeatmapCanvas(
                        indoorMap = state.activeMap,
                        cells = state.heatmapCells,
                        sourcePoints = state.sourcePoints,
                        metric = state.selectedMetric,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.error != null) {
                    Text(text = "Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("No heatmap data available for this session.")
                }
            }

            // Legend
            HeatmapLegend(
                metric = state.selectedMetric,
                range = state.metricRange
            )
        }
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
            Text(
                text = metric.name.replace("_", " "),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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
                val unit = when(metric) {
                    HeatmapMetric.DOWNLOAD, HeatmapMetric.UPLOAD -> " Mbps"
                    HeatmapMetric.LATENCY -> " ms"
                    HeatmapMetric.WIFI_RSSI, HeatmapMetric.CELLULAR_RSRP -> " dBm"
                    HeatmapMetric.CELLULAR_RSRQ -> " dB"
                    HeatmapMetric.CELLULAR_SINR -> ""
                }

                val minLabel = range?.let { "%.1f".format(if (metric.isHigherBetter()) it.min else it.max) } ?: "Min"
                val maxLabel = range?.let { "%.1f".format(if (metric.isHigherBetter()) it.max else it.min) } ?: "Max"

                Text(text = "$minLabel$unit", style = MaterialTheme.typography.labelSmall)
                Text(text = "$maxLabel$unit", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
