package com.networkradar.feature.comparison.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.domain.measurement.ComparisonTrend
import com.networkradar.core.domain.measurement.NetworkMetric
import com.networkradar.core.domain.measurement.ScanComparison
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.presentation.util.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ComparisonRoot(
    viewModel: ComparisonViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ComparisonEvent.Error -> {
                // Show error message
            }
        }
    }

    ComparisonScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    state: ComparisonState,
    onAction: (ComparisonAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Compare Scans") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (state.comparison == null) {
                ScanSelector(
                    scans = state.scans,
                    selectedIdA = state.selectedIdA,
                    selectedIdB = state.selectedIdB,
                    onSelectA = { onAction(ComparisonAction.SelectScanA(it)) },
                    onSelectB = { onAction(ComparisonAction.SelectScanB(it)) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onAction(ComparisonAction.Compare) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.selectedIdA != null && state.selectedIdB != null && state.selectedIdA != state.selectedIdB && !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Compare Now")
                    }
                }
            } else {
                ComparisonResults(comparison = state.comparison)
            }
        }
    }
}

@Composable
private fun ScanSelector(
    scans: List<ScanSession>,
    selectedIdA: String?,
    selectedIdB: String?,
    onSelectA: (String) -> Unit,
    onSelectB: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Column {
        Text("Select First Scan", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ScanList(scans, selectedIdA, onSelectA, dateFormat)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Select Second Scan", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ScanList(scans, selectedIdB, onSelectB, dateFormat)
    }
}

@Composable
private fun ColumnScope.ScanList(
    scans: List<ScanSession>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    dateFormat: SimpleDateFormat
) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(scans, key = { it.id }) { scan ->
            FilterChip(
                selected = selectedId == scan.id,
                onClick = { onSelect(scan.id) },
                label = { 
                    Text("${scan.name.ifBlank { "Scan" }} (${dateFormat.format(Date(scan.startedAt))})") 
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ComparisonResults(comparison: ScanComparison) {
    Column {
        Text(
            text = "Comparison Summary",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "${comparison.scanA.name} vs ${comparison.scanB.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        comparison.results.forEach { (metric, comp) ->
            MetricComparisonItem(metric, comp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun MetricComparisonItem(metric: NetworkMetric, comp: com.networkradar.core.domain.measurement.MetricComparison) {
    val unit = when(metric) {
        NetworkMetric.DOWNLOAD, NetworkMetric.UPLOAD -> " Mbps"
        NetworkMetric.LATENCY -> " ms"
        NetworkMetric.WIFI_RSSI, NetworkMetric.CELLULAR_RSRP -> " dBm"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = metric.name.replace("_", " "), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "${"%.2f".format(comp.valueA)}$unit", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Before", style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${"%.2f".format(comp.valueB)}$unit", style = MaterialTheme.typography.bodyLarge)
                Text(text = "After", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val trendColor = when(comp.trend) {
            ComparisonTrend.IMPROVED -> Color(0xFF4CAF50)
            ComparisonTrend.WORSENED -> Color(0xFFF44336)
            else -> MaterialTheme.colorScheme.onSurface
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${if (comp.difference > 0) "+" else ""}${"%.2f".format(comp.difference)}$unit",
                color = trendColor,
                fontWeight = FontWeight.Bold
            )
            comp.percentageChange?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${"%.1f".format(it)}%)",
                    color = trendColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = comp.trend.name,
                color = trendColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
