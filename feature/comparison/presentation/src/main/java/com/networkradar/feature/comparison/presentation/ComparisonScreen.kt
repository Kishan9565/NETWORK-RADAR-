package com.networkradar.feature.comparison.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.designsystem.SignalExcellent
import com.networkradar.core.designsystem.SignalGood
import com.networkradar.core.designsystem.SignalPoor
import com.networkradar.core.domain.measurement.*
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
            is ComparisonEvent.Error -> { /* Handle error */ }
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
            TopAppBar(title = { Text("Scan Comparison", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (state.comparison == null) {
                Text(
                    text = "SELECT TWO SCANS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                
                ScanSelector(
                    scans = state.scans,
                    selectedIdA = state.selectedIdA,
                    selectedIdB = state.selectedIdB,
                    onSelectA = { onAction(ComparisonAction.SelectScanA(it)) },
                    onSelectB = { onAction(ComparisonAction.SelectScanB(it)) },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onAction(ComparisonAction.Compare) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = state.selectedIdA != null && state.selectedIdB != null && state.selectedIdA != state.selectedIdB && !state.isLoading,
                    shape = MaterialTheme.shapes.large
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.CompareArrows, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COMPARE SELECTED", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                ComparisonResults(
                    comparison = state.comparison,
                    onReset = { /* Implementation to reset could be added to VM action */ }
                )
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
    onSelectB: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Column(modifier = modifier) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Primary Scan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ScanList(scans, selectedIdA, onSelectA, dateFormat)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Comparison Scan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ScanList(scans, selectedIdB, onSelectB, dateFormat)
            }
        }
    }
}

@Composable
private fun ScanList(
    scans: List<ScanSession>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    dateFormat: SimpleDateFormat
) {
    if (scans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No scans available", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(scans, key = { it.id }) { scan ->
                FilterChip(
                    selected = selectedId == scan.id,
                    onClick = { onSelect(scan.id) },
                    label = { 
                        Text(
                            text = "${scan.name.ifBlank { "Scan" }} (${dateFormat.format(Date(scan.startedAt))})",
                            style = MaterialTheme.typography.labelMedium
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    }
}

@Composable
private fun ComparisonResults(comparison: ScanComparison, onReset: () -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "COMPARISON SUMMARY",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "${comparison.scanA.name} vs ${comparison.scanB.name}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        comparison.results.forEach { (metric, comp) ->
            MetricComparisonCard(metric, comp)
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MetricComparisonCard(metric: NetworkMetric, comp: com.networkradar.core.domain.measurement.MetricComparison) {
    val unit = when(metric) {
        NetworkMetric.DOWNLOAD, NetworkMetric.UPLOAD -> " Mbps"
        NetworkMetric.LATENCY -> " ms"
        NetworkMetric.WIFI_RSSI, NetworkMetric.CELLULAR_RSRP -> " dBm"
        else -> ""
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = metric.name.replace("_", " "),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "${"%.1f".format(comp.valueA)}$unit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Previous", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "${"%.1f".format(comp.valueB)}$unit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val trendColor = when(comp.trend) {
                ComparisonTrend.IMPROVED -> SignalExcellent
                ComparisonTrend.WORSENED -> SignalPoor
                else -> MaterialTheme.colorScheme.onSurface
            }
            
            Surface(
                color = trendColor.copy(alpha = 0.1f),
                contentColor = trendColor,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (comp.difference > 0) "+" else ""}${"%.1f".format(comp.difference)}$unit",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    comp.percentageChange?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${"%.1f".format(it)}%)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = comp.trend.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
