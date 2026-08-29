package com.networkradar.feature.radar.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.designsystem.SignalExcellent
import com.networkradar.core.designsystem.SignalFair
import com.networkradar.core.designsystem.SignalGood
import com.networkradar.core.designsystem.SignalPoor
import com.networkradar.core.designsystem.SignalUnavailable
import com.networkradar.core.domain.location.LocationObservation
import com.networkradar.core.domain.measurement.*
import com.networkradar.feature.radar.domain.RadarMeasurement
import org.koin.androidx.compose.koinViewModel

@Composable
fun RadarRoot(
    onViewHeatmap: (String) -> Unit,
    onNavigateToMapSelection: () -> Unit,
    viewModel: RadarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RadarScreen(
        state = state,
        onAction = viewModel::onAction,
        onViewHeatmap = onViewHeatmap,
        onNavigateToMapSelection = onNavigateToMapSelection
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    state: RadarState,
    onAction: (RadarAction) -> Unit,
    onViewHeatmap: (String) -> Unit,
    onNavigateToMapSelection: () -> Unit
) {
    val measurement = state.measurement
    val activeSession = state.activeSession
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Network Radar", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .animateContentSize(),
            verticalArrangement = Arrangement.Top
        ) {
            if (activeSession == null) {
                ScanModeSelection(
                    selectedMapName = state.selectedMap?.name,
                    onStartQuickScan = { onAction(RadarAction.StartQuickScan("Quick Scan ${System.currentTimeMillis()}")) },
                    onStartSpatialScan = { 
                        state.selectedMap?.let {
                            onAction(RadarAction.StartSpatialScan("Spatial Scan ${System.currentTimeMillis()}", it.id))
                        } ?: onNavigateToMapSelection()
                    },
                    onSelectMap = onNavigateToMapSelection
                )
            } else {
                ActiveScanHeader(
                    activeSession = activeSession,
                    isSpatial = state.isSpatialScan,
                    mapName = state.selectedMap?.name,
                    onStopScan = { onAction(RadarAction.StopScan) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isAnalyzing) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analyzing Intelligence...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            state.intelligenceSummary?.let { summary ->
                IntelligenceSummaryView(
                    summary = summary,
                    onViewHeatmap = {
                        state.analyzedSessionId?.let { onViewHeatmap(it) }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (measurement != null) {
                LiveRFView(measurement)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SpeedTestView(
                    speedMbps = state.downloadSpeedMbps,
                    isTesting = state.isTestingSpeed,
                    onRunTest = { onAction(RadarAction.RunSpeedTest) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LocationView(measurement, state.isSpatialScan)
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ScanModeSelection(
    selectedMapName: String?,
    onStartQuickScan: () -> Unit,
    onStartSpatialScan: () -> Unit,
    onSelectMap: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(text = "SELECT SCAN MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            onClick = onStartQuickScan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Instant measurement of RF and Internet quality at your current position. No map required.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            onClick = onStartSpatialScan,
            modifier = Modifier.fillMaxWidth(),
            colors = if (selectedMapName != null) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) 
                     else CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Spatial Scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (selectedMapName != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge { Text("READY") }
                    }
                }
                Text("Map RF quality across a floor plan. Move around to create a heatmap.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onSelectMap,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedMapName ?: "SELECT FLOOR PLAN")
                }
            }
        }
    }
}

@Composable
private fun ActiveScanHeader(
    activeSession: ScanSession,
    isSpatial: Boolean,
    mapName: String?,
    onStopScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSpatial) "SPATIAL SCAN ACTIVE" else "QUICK SCAN ACTIVE", 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(text = activeSession.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onStopScan,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("STOP")
                }
            }
            if (isSpatial && mapName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Mapping: $mapName", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${activeSession.measurementCount} points recorded", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LiveRFView(measurement: RadarMeasurement) {
    Column {
        Text(text = "LIVE RF INTELLIGENCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        val wifi = measurement.point.wifi
        val cellular = measurement.point.cellular

        if (wifi != null) {
            RFMetricCard(
                title = "Wi-Fi Signal",
                icon = Icons.Default.Wifi,
                quality = SignalQualityStrategy.getWifiQuality(wifi.rssi),
                details = listOf(
                    "RSSI" to "${wifi.rssi ?: "---"} dBm",
                    "Frequency" to "${wifi.frequency ?: "---"} MHz",
                    "Link Speed" to "${wifi.linkSpeed ?: "---"} Mbps"
                ),
                explanation = "RSSI is signal strength. Closer to 0 is stronger. Link Speed is negotiated throughput."
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
             RFMetricCard(
                title = "Wi-Fi Signal",
                icon = Icons.Default.Wifi,
                quality = SignalQuality.UNAVAILABLE,
                details = emptyList(),
                explanation = "Wi-Fi is not connected or unavailable."
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (cellular != null) {
            RFMetricCard(
                title = "Cellular Signal",
                icon = Icons.Default.CellTower,
                quality = SignalQualityStrategy.getCellularQuality(cellular.rsrp),
                details = listOf(
                    "Network" to (cellular.networkType ?: "---"),
                    "RSRP" to "${cellular.rsrp ?: "---"} dBm",
                    "RSRQ" to "${cellular.rsrq ?: "---"} dB",
                    "SINR" to "${cellular.sinr ?: "---"} dB"
                ),
                explanation = "RSRP is signal strength. RSRQ and SINR indicate signal quality and interference levels."
            )
        } else {
            RFMetricCard(
                title = "Cellular Signal",
                icon = Icons.Default.CellTower,
                quality = SignalQuality.UNAVAILABLE,
                details = emptyList(),
                explanation = "Cellular radio information is unavailable."
            )
        }
    }
}

@Composable
private fun RFMetricCard(
    title: String,
    icon: ImageVector,
    quality: SignalQuality,
    details: List<Pair<String, String>>,
    explanation: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                SignalBadge(quality)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (details.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    details.chunked((details.size + 1) / 2).forEach { column ->
                        Column {
                            column.forEach { (label, value) ->
                                InfoItem(label, value)
                            }
                        }
                    }
                }
            } else {
                Text("Data unavailable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = explanation, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SignalBadge(quality: SignalQuality) {
    val (color, text) = when (quality) {
        SignalQuality.EXCELLENT -> SignalExcellent to "EXCELLENT"
        SignalQuality.GOOD -> SignalGood to "GOOD"
        SignalQuality.FAIR -> SignalFair to "FAIR"
        SignalQuality.POOR -> SignalPoor to "POOR"
        SignalQuality.UNAVAILABLE -> SignalUnavailable to "UNAVAILABLE"
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SpeedTestView(
    speedMbps: Double?,
    isTesting: Boolean,
    onRunTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "NETWORK SPEED", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isTesting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Testing...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(
                            text = speedMbps?.let { "${"%.1f".format(it)} Mbps" } ?: "Not tested",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Button(onClick = onRunTest, enabled = !isTesting) {
                    Text("RUN TEST")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Actively measures real throughput. Data usage applies.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationView(measurement: RadarMeasurement, isSpatial: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "SPATIAL CONTEXT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isSpatial) {
                val indoor = measurement.point.indoorPosition
                if (indoor != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoItem("Indoor X", "${"%.2f".format(indoor.x)} m")
                        InfoItem("Indoor Y", "${"%.2f".format(indoor.y)} m")
                    }
                } else {
                    Text("Acquiring indoor position...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            } else {
                when (val status = measurement.locationStatus) {
                    is LocationObservation.Success -> {
                        val loc = status.location
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoItem("GPS Accuracy", "±${"%.1f".format(loc.accuracy)} m")
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Lat: ${"%.5f".format(loc.lat)}", style = MaterialTheme.typography.labelSmall)
                                Text(text = "Long: ${"%.5f".format(loc.long)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    is LocationObservation.Loading -> Text("Acquiring GPS...", style = MaterialTheme.typography.bodyMedium)
                    else -> Text("GPS location unavailable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun IntelligenceSummaryView(
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
                Text("VIEW HEATMAP")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Coverage", summary.dataSufficiency.name)
                    InfoItem("Confidence", "${(summary.overallConfidence * 100).toInt()}%")
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (summary.statistics.isNotEmpty()) {
                    Text(text = "Averages", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        summary.statistics[NetworkMetric.DOWNLOAD]?.let {
                            InfoItem("Download", "${"%.1f".format(it.average)} Mbps")
                        }
                        summary.statistics[NetworkMetric.WIFI_RSSI]?.let {
                            InfoItem("Wi-Fi RSSI", "${it.average.toInt()} dBm")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RankedSpotCard("BEST SPOT", summary.bestSpot, MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f))
            RankedSpotCard("WEAKEST SPOT", summary.worstSpot, MaterialTheme.colorScheme.errorContainer, modifier = Modifier.weight(1f))
        }
    }
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
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
