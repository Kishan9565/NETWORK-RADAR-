package com.networkradar.feature.radar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.domain.location.LocationObservation
import com.networkradar.core.domain.measurement.NetworkMetric
import com.networkradar.core.domain.measurement.RankedSpot
import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
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
            TopAppBar(title = { Text("Radar") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
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
                LiveMeasurementView(measurement, state.isSpatialScan)
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
        Text(text = "Scan Mode", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                onClick = onStartQuickScan
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Scan", style = MaterialTheme.typography.titleMedium)
                    Text("Network & RF only", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                onClick = onStartSpatialScan,
                colors = if (selectedMapName != null) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) 
                         else CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Spatial Scan", style = MaterialTheme.typography.titleMedium)
                    Text(selectedMapName ?: "Select Floor Plan", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ActiveScanHeader(
    activeSession: com.networkradar.core.domain.measurement.ScanSession,
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
                    Text(text = if (isSpatial) "Spatial Scan Active" else "Quick Scan Active", style = MaterialTheme.typography.titleMedium)
                    if (isSpatial && mapName != null) {
                        Text(text = "Floor Plan: $mapName", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = onStopScan,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${activeSession.measurementCount} points recorded", style = MaterialTheme.typography.bodyMedium)
            if (isSpatial) {
                Text(text = "Walk around the area to map quality.", style = MaterialTheme.typography.labelSmall)
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
            Text(text = "SCAN INTELLIGENCE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Button(onClick = onViewHeatmap) {
                Text("View Heatmap")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("Data Sufficiency", summary.dataSufficiency.name)
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

                    summary.stability[NetworkMetric.DOWNLOAD]?.let {
                        InfoItem("Stability (Speed)", it.level.name)
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
                val indoor = spot.indoorPosition
                val loc = spot.location
                val pos = when {
                    indoor != null -> "Indoor (${indoor.x}m, ${indoor.y}m)"
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
private fun LiveMeasurementView(measurement: RadarMeasurement, isSpatial: Boolean) {
    Column {
        Text(text = "LIVE RADIO / RF", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                val wifi = measurement.point.wifi
                if (wifi != null) {
                    Text(text = "Wi-Fi", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoItem("SSID", wifi.ssid ?: "Hidden")
                        InfoItem("RSSI", "${wifi.rssi ?: "N/A"} dBm")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoItem("Frequency", "${wifi.frequency ?: "N/A"} MHz")
                        InfoItem("Link Speed", "${wifi.linkSpeed ?: "N/A"} Mbps")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val cellular = measurement.point.cellular
                if (cellular != null) {
                    Text(text = "Cellular", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoItem("Network", cellular.networkType ?: "Unknown")
                        InfoItem("RSRP", "${cellular.rsrp ?: "N/A"} dBm")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoItem("RSRQ", "${cellular.rsrq ?: "N/A"} dB")
                        InfoItem("SINR", "${cellular.sinr ?: "N/A"} dB")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "POSITION", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isSpatial) {
                    val indoor = measurement.point.indoorPosition
                    if (indoor != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoItem("X Position", "${"%.2f".format(indoor.x)} m")
                            InfoItem("Y Position", "${"%.2f".format(indoor.y)} m")
                        }
                    } else {
                        Text("Indoor position not available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    when (val status = measurement.locationStatus) {
                        is LocationObservation.Success -> {
                            val loc = status.location
                            InfoItem("Location Accuracy", "±${"%.1f".format(loc.accuracy)} m")
                            Text(text = "Lat: ${loc.lat}, Long: ${loc.long}", style = MaterialTheme.typography.labelSmall)
                        }
                        else -> Text("Location unavailable", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
