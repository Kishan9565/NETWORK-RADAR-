package com.networkradar.feature.history.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.designsystem.*
import com.networkradar.core.domain.indoor.SpatialAnnotation
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
import kotlin.math.pow
import kotlin.math.sqrt

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
                shareFile(context, event.uri, event.filename) { _ ->
                    viewModel.onAction(ScanReportAction.LoadReport)
                }
            }
            is ScanReportEvent.Error -> {
                // Surface error to user
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
                title = { Text("Scan Report", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onAction(ScanReportAction.ExportCsv) }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export CSV", tint = ElectricCyan)
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
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                }
                state.summary != null -> {
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
                        annotations = state.annotations,
                        isSpatial = state.scan?.isSpatial ?: false,
                        scanStartTime = state.scan?.startedAt ?: 0L,
                        onViewHeatmap = onViewHeatmap
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(text = "EXPORT DATA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onAction(ScanReportAction.ExportCsv) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Export CSV")
                        }
                        Button(
                            onClick = { onAction(ScanReportAction.ExportJson) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Export JSON")
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.error ?: "Report unavailable", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun IntelligenceSummaryContent(
    summary: ScanIntelligenceSummary,
    annotations: List<SpatialAnnotation>,
    isSpatial: Boolean,
    scanStartTime: Long,
    onViewHeatmap: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "SCAN INTELLIGENCE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Button(
                onClick = onViewHeatmap,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
            ) {
                Text("View Heatmap", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Data Coverage", summary.dataSufficiency.name)
                    InfoItem("Confidence", "${(summary.overallConfidence * 100).toInt()}%")
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(text = "METRIC AVERAGES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
            RankedSpotCard(
                label = if (isSpatial) "BEST SPOT" else "BEST MOMENT",
                spot = summary.bestSpot,
                annotations = annotations,
                isSpatial = isSpatial,
                scanStartTime = scanStartTime,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            RankedSpotCard(
                label = if (isSpatial) "WEAKEST SPOT" else "WEAKEST MOMENT",
                spot = summary.worstSpot,
                annotations = annotations,
                isSpatial = isSpatial,
                scanStartTime = scanStartTime,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricAverageRow(label: String, value: String, quality: SignalQuality) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
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
        else -> SignalUnavailable
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun RankedSpotCard(
    label: String, 
    spot: RankedSpot?, 
    annotations: List<SpatialAnnotation>,
    isSpatial: Boolean,
    scanStartTime: Long,
    containerColor: Color, 
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier, 
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.2f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, containerColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = containerColor)
            Spacer(modifier = Modifier.height(8.dp))
            if (spot != null) {
                Text(text = "Score: ${"%.1f".format(spot.score)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (isSpatial) {
                    val indoor = spot.indoorPosition
                    val loc = spot.location
                    
                    if (indoor != null) {
                        val nearestPin = annotations.minByOrNull { pin ->
                            sqrt((pin.x - indoor.x).pow(2) + (pin.y - indoor.y).pow(2))
                        }
                        val distanceToPin = nearestPin?.let { pin ->
                            sqrt((pin.x - indoor.x).pow(2) + (pin.y - indoor.y).pow(2))
                        }
                        
                        if (distanceToPin != null && distanceToPin < 1.5) {
                            Text(
                                text = "Near: ${nearestPin.label ?: "Marker"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "POS: ${indoor.x.toInt()}m, ${indoor.y.toInt()}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (loc != null) {
                        Text(
                            text = "GPS Coordinates",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val uri = "geo:${loc.lat},${loc.long}?q=${loc.lat},${loc.long}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                context.startActivity(intent)
                            }
                        )
                    } else {
                        Text(text = "Unknown Position", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    // Quick Scan - Time-based reference
                    val relativeTimeMs = spot.timestamp - scanStartTime
                    val minutes = (relativeTimeMs / 1000) / 60
                    val seconds = (relativeTimeMs / 1000) % 60
                    Text(
                        text = if (minutes > 0) "${minutes}m ${seconds}s into scan" else "${seconds}s into scan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

private fun shareFile(context: Context, content: String, filename: String, onError: (String) -> Unit) {
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
        Toast.makeText(context, "Couldn't share file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}
