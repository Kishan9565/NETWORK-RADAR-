package com.networkradar.feature.history.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.designsystem.SignalExcellent
import com.networkradar.core.designsystem.SignalFair
import com.networkradar.core.designsystem.SignalGood
import com.networkradar.core.designsystem.SignalPoor
import com.networkradar.core.designsystem.SignalUnavailable
import com.networkradar.core.domain.measurement.NetworkMetric
import com.networkradar.core.domain.measurement.RankedSpot
import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.core.domain.measurement.SignalQuality
import com.networkradar.core.domain.measurement.SignalQualityStrategy
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
                // Error feedback
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
                title = { Text("Scan Report", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onAction(ScanReportAction.ExportCsv) }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export CSV")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.summary != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                state.scan?.let { scan ->
                    Text(
                        text = scan.name.ifBlank { "Unnamed Scan" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy • h:mm a", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date(scan.startedAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                IntelligenceSummaryContent(
                    summary = state.summary,
                    onViewHeatmap = onViewHeatmap
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(text = "EXPORT DATA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onAction(ScanReportAction.ExportCsv) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Export CSV")
                    }
                    OutlinedButton(
                        onClick = { onAction(ScanReportAction.ExportJson) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Export JSON")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.error ?: "Report unavailable", style = MaterialTheme.typography.bodyLarge)
                }
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
            Text(text = "SCAN INTELLIGENCE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onViewHeatmap) {
                Text("View Heatmap")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Data Coverage", summary.dataSufficiency.name)
                    InfoItem("Confidence", "${(summary.overallConfidence * 100).toInt()}%")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "METRIC AVERAGES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                if (summary.statistics.isNotEmpty()) {
                    summary.statistics[NetworkMetric.DOWNLOAD]?.let {
                        MetricAverageRow("Download Speed", "${"%.1f".format(it.average)} Mbps", SignalQualityStrategy.getDownloadQuality(it.average))
                    }
                    summary.statistics[NetworkMetric.LATENCY]?.let {
                        MetricAverageRow("Average Latency", "${it.average.toInt()} ms", SignalQualityStrategy.getLatencyQuality(it.average))
                    }
                    summary.statistics[NetworkMetric.WIFI_RSSI]?.let {
                        MetricAverageRow("Wi-Fi Signal", "${it.average.toInt()} dBm", SignalQualityStrategy.getWifiQuality(it.average.toInt()))
                    }
                } else {
                    Text("No aggregate metrics available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RankedSpotCard("BEST SPOT", summary.bestSpot, MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f))
            RankedSpotCard("WEAKEST SPOT", summary.worstSpot, MaterialTheme.colorScheme.errorContainer, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricAverageRow(label: String, value: String, quality: SignalQuality) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            SignalBadgeTiny(quality)
        }
    }
}

@Composable
private fun SignalBadgeTiny(quality: SignalQuality) {
    val color = when (quality) {
        SignalQuality.EXCELLENT -> SignalExcellent
        SignalQuality.GOOD -> SignalGood
        SignalQuality.FAIR -> SignalFair
        SignalQuality.POOR -> SignalPoor
        SignalQuality.UNAVAILABLE -> SignalUnavailable
    }
    Surface(
        color = color,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.size(8.dp)
    ) {}
}

@Composable
private fun RankedSpotCard(label: String, spot: RankedSpot?, containerColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            if (spot != null) {
                Text(text = "Score: ${"%.1f".format(spot.score)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                val indoor = spot.indoorPosition
                val loc = spot.location
                val pos = when {
                    indoor != null -> "Indoor (${indoor.x.toInt()}m, ${indoor.y.toInt()}m)"
                    loc != null -> "${"%.4f".format(loc.lat)}, ${"%.4f".format(loc.long)}"
                    else -> "Unknown"
                }
                Text(text = pos, style = MaterialTheme.typography.labelSmall)
            } else {
                Text(text = "Insufficient Data", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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
