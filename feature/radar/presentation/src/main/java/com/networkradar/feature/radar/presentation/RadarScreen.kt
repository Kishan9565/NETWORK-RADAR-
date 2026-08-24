package com.networkradar.feature.radar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import com.networkradar.core.domain.measurement.DataSufficiency
import com.networkradar.core.domain.measurement.NetworkMetric
import com.networkradar.core.domain.measurement.RankedSpot
import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.feature.radar.domain.RadarMeasurement
import org.koin.androidx.compose.koinViewModel

@Composable
fun RadarRoot(
    onViewHeatmap: (String) -> Unit,
    viewModel: RadarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RadarScreen(
        state = state,
        onAction = viewModel::onAction,
        onViewHeatmap = onViewHeatmap
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    state: RadarState,
    onAction: (RadarAction) -> Unit,
    onViewHeatmap: (String) -> Unit
) {
    val measurement = state.measurement
    val activeSession = state.activeSession
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Network Radar") })
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Live Radar", style = MaterialTheme.typography.headlineSmall)
                
                if (activeSession == null) {
                    Button(onClick = { onAction(RadarAction.StartScan("Quick Scan")) }) {
                        Text("Start Scan")
                    }
                } else {
                    Button(
                        onClick = { onAction(RadarAction.StopScan) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop Scan")
                    }
                }
            }
            
            if (activeSession != null) {
                Text(
                    text = "Scanning: ${activeSession.name} (${activeSession.measurementCount} points)",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

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
                LiveMeasurementView(measurement)
            } else if (activeSession == null && state.intelligenceSummary == null) {
                Text("Start a scan to begin collecting network intelligence.", style = MaterialTheme.typography.bodyMedium)
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
                    indoor != null -> "Indoor (${indoor.x}, ${indoor.y})"
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
private fun LiveMeasurementView(measurement: RadarMeasurement) {
    Column {
        Text(text = "LIVE DATA", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(8.dp))

        InfoItem("Connectivity", measurement.connectivity.networkType.name)
        InfoItem("Internet", if (measurement.connectivity.isInternetAvailable) "Available" else "Unavailable")

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Location", style = MaterialTheme.typography.titleMedium)

        when (val status = measurement.locationStatus) {
            is LocationObservation.Success -> {
                val loc = status.location
                InfoItem("Coordinates", "${"%.5f".format(loc.lat)}, ${"%.5f".format(loc.long)}")
                InfoItem("Accuracy", "${"%.1f".format(loc.accuracy)} m")
            }
            LocationObservation.PermissionRequired -> Text("Permission Required", color = MaterialTheme.colorScheme.error)
            LocationObservation.PermissionsDenied -> Text("Permission Denied", color = MaterialTheme.colorScheme.error)
            LocationObservation.ServicesDisabled -> Text("Location Services Disabled", color = MaterialTheme.colorScheme.error)
            LocationObservation.Unavailable -> Text("Location Unavailable", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))
        val wifi = measurement.point.wifi
        if (wifi != null) {
            Text(text = "Wi-Fi", style = MaterialTheme.typography.titleMedium)
            InfoItem("SSID", wifi.ssid ?: "Hidden")
            InfoItem("RSSI", "${wifi.rssi} dBm")
        }

        val cellular = measurement.point.cellular
        if (cellular != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Cellular", style = MaterialTheme.typography.titleMedium)
            InfoItem("Type", cellular.networkType ?: "Unknown")
            InfoItem("RSRP", "${cellular.rsrp ?: "N/A"} dBm")
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
