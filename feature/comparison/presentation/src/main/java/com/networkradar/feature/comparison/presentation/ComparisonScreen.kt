package com.networkradar.feature.comparison.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.designsystem.ElectricCyan
import com.networkradar.core.designsystem.SignalExcellent
import com.networkradar.core.designsystem.SignalFair
import com.networkradar.core.designsystem.SignalGood
import com.networkradar.core.designsystem.SignalPoor
import com.networkradar.core.designsystem.SignalUnavailable
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
            is ComparisonEvent.Error -> { /* Handle error via snackbar if needed */ }
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
            TopAppBar(title = { Text("Scan Comparison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when {
                state.isLoading && state.scans.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                }
                state.comparison == null -> {
                    Text(
                        text = "SELECT TWO SCANS TO COMPARE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 16.dp)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = state.selectedIdA != null && state.selectedIdB != null && state.selectedIdA != state.selectedIdB && !state.isLoading,
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.CompareArrows, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COMPARE SELECTED", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                else -> {
                    ComparisonResults(
                        comparison = state.comparison,
                        onReset = { /* Could add a back/reset button if state allowed */ }
                    )
                }
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
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Primary Scan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ScanList(scans, selectedIdA, onSelectA, dateFormat)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Comparison Scan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            Text(
                text = "No recorded scans found.", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricCyan
                    )
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
            style = MaterialTheme.typography.headlineMedium,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = metric.name.replace("_", " "),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "PREVIOUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${"%.1f".format(comp.valueA)}$unit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "CURRENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${"%.1f".format(comp.valueB)}$unit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (comp.difference > 0) "+" else ""}${"%.1f".format(comp.difference)}$unit",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    comp.percentageChange?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${"%.1f".format(it)}%)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = comp.trend.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
