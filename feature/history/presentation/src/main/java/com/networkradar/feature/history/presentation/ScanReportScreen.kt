package com.networkradar.feature.history.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.domain.measurement.NetworkMetric
import com.networkradar.core.domain.measurement.RankedSpot
import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.core.presentation.util.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScanReportRoot(
    sessionId: String,
    onViewHeatmap: (String) -> Unit,
    viewModel: ScanReportViewModel = koinViewModel(parameters = { parametersOf(sessionId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ScanReportEvent.ExportReady -> {
                shareFile(context, event.uri, event.filename)
            }
            is ScanReportEvent.Error -> {
                // UI feedback could be added here via a Snackbar
            }
        }
    }

    ScanReportScreen(
        state = state,
        onAction = viewModel::onAction,
        onViewHeatmap = { onViewHeatmap(sessionId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReportScreen(
    state: ScanReportState,
    onAction: (ScanReportAction) -> Unit,
    onViewHeatmap: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Report") },
                actions = {
                    IconButton(onClick = { onAction(ScanReportAction.ExportCsv) }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export CSV")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.summary != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                state.scan?.let { scan ->
                    Text(text = scan.name.ifBlank { "Unnamed Scan" }, style = MaterialTheme.typography.headlineMedium)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    Text(text = dateFormat.format(Date(scan.startedAt)), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                IntelligenceSummaryContent(
                    summary = state.summary,
                    onViewHeatmap = onViewHeatmap
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(text = "Export Data", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(ScanReportAction.ExportCsv) }, modifier = Modifier.weight(1f)) {
                        Text("CSV")
                    }
                    Button(onClick = { onAction(ScanReportAction.ExportJson) }, modifier = Modifier.weight(1f)) {
                        Text("JSON")
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error ?: "Report unavailable")
            }
        }
    }
}

@Composable
private fun IntelligenceSummaryContent(
    summary: ScanIntelligenceSummary,
    onViewHeatmap: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "INTELLIGENCE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Button(onClick = onViewHeatmap) {
                Text("View Heatmap")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Sufficiency", summary.dataSufficiency.name)
                    InfoItem("Confidence", "${(summary.overallConfidence * 100).toInt()}%")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Total Points", summary.totalPoints.toString())
                    InfoItem("Valid Points", summary.validPoints.toString())
                }

                if (summary.statistics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Averages", style = MaterialTheme.typography.labelLarge)
                    summary.statistics[NetworkMetric.DOWNLOAD]?.let {
                        InfoItem("Avg Download", "${"%.2f".format(it.average)} Mbps")
                    }
                    summary.statistics[NetworkMetric.LATENCY]?.let {
                        InfoItem("Avg Latency", "${"%.1f".format(it.average)} ms")
                    }
                    summary.statistics[NetworkMetric.WIFI_RSSI]?.let {
                        InfoItem("Avg Wi-Fi RSSI", "${it.average.toInt()} dBm")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RankedSpotView("BEST SPOT", summary.bestSpot, modifier = Modifier.weight(1f))
            RankedSpotView("WORST SPOT", summary.worstSpot, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RankedSpotView(label: String, spot: RankedSpot?, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            if (spot != null) {
                Text(text = "Score: ${"%.1f".format(spot.score)}", style = MaterialTheme.typography.bodyMedium)
                val indoorPos = spot.indoorPosition
                val loc = spot.location
                val pos = when {
                    indoorPos != null -> "Indoor (${indoorPos.x}, ${indoorPos.y})"
                    loc != null -> "${"%.4f".format(loc.lat)}, ${"%.4f".format(loc.long)}"
                    else -> "Unknown Position"
                }
                Text(text = pos, style = MaterialTheme.typography.labelSmall)
            } else {
                Text(text = "N/A", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun shareFile(context: Context, content: String, filename: String) {
    try {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { 
            it.write(content.toByteArray())
        }
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (filename.endsWith("csv")) "text/csv" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Scan Data"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
