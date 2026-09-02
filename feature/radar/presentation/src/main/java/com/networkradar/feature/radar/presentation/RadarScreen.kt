package com.networkradar.feature.radar.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.networkradar.core.designsystem.SignalExcellent
import com.networkradar.core.designsystem.SignalFair
import com.networkradar.core.designsystem.SignalGood
import com.networkradar.core.designsystem.SignalPoor
import com.networkradar.core.designsystem.SignalUnavailable
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.location.LocationObservation
import com.networkradar.core.domain.measurement.*
import com.networkradar.core.presentation.util.ObserveAsEvents
import com.networkradar.feature.radar.domain.RadarMeasurement
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

@Composable
fun RadarRoot(
    onViewHeatmap: (String) -> Unit,
    viewModel: RadarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMarkSpotDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onAction(RadarAction.PermissionResult(permissions))
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RadarEvent.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
            RadarEvent.RequestLocationPermission -> {
                val permissions = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                permissionLauncher.launch(permissions.toTypedArray())
            }
            RadarEvent.OpenAppSettings -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }

    RadarScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onViewHeatmap = onViewHeatmap,
        onMarkSpotClick = { showMarkSpotDialog = true }
    )
    
    if (state.showPermissionRationale) {
        PermissionRationaleDialog(
            onConfirm = { viewModel.onAction(RadarAction.RequestPermission) },
            onDismiss = { viewModel.onAction(RadarAction.DismissRationale) }
        )
    }

    if (showMarkSpotDialog) {
        MarkSpotDialog(
            onConfirm = { label ->
                viewModel.onAction(RadarAction.MarkSpot(label))
                showMarkSpotDialog = false
            },
            onDismiss = { showMarkSpotDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    state: RadarState,
    snackbarHostState: SnackbarHostState,
    onAction: (RadarAction) -> Unit,
    onViewHeatmap: (String) -> Unit,
    onMarkSpotClick: () -> Unit
) {
    val measurement = state.measurement
    val activeSession = state.activeSession
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Network Radar", fontWeight = FontWeight.Bold) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            if (!state.isLocationPermissionGranted && activeSession == null) {
                PermissionDeniedBanner(
                    onRequestPermission = { onAction(RadarAction.RequestPermission) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (activeSession == null) {
                ScanModeSelection(
                    onStartQuickScan = { onAction(RadarAction.StartQuickScan("Quick Scan ${System.currentTimeMillis()}")) },
                    onStartSpatialScan = { onAction(RadarAction.StartSpatialScan("Spatial Scan ${System.currentTimeMillis()}")) }
                )
            } else {
                ActiveScanHeader(
                    activeSession = activeSession,
                    isSpatial = state.isSpatialScan,
                    onStopScan = { onAction(RadarAction.StopScan) },
                    onRecalibrate = { onAction(RadarAction.RecalibratePosition) },
                    onMarkSpot = onMarkSpotClick
                )
                
                if (state.isSpatialScan) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PdrPathView(path = state.spatialPath, currentPos = state.currentIndoorPosition)
                }
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
private fun MarkSpotDialog(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark this spot") },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (e.g. Living Room)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(label.takeIf { it.isNotBlank() }) }) {
                Text("Save Marker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PdrPathView(path: List<IndoorPosition>, currentPos: IndoorPosition?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Approximate Path (Relative)", 
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    .padding(8.dp)
            ) {
                if (path.isNotEmpty() || currentPos != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val allPoints = if (currentPos != null) path + currentPos else path
                        if (allPoints.isEmpty()) return@Canvas
                        
                        val minX = allPoints.minOf { it.x }
                        val maxX = allPoints.maxOf { it.x }
                        val minY = allPoints.minOf { it.y }
                        val maxY = allPoints.maxOf { it.y }
                        
                        val rangeX = abs(maxX - minX).coerceAtLeast(10f)
                        val rangeY = abs(maxY - minY).coerceAtLeast(10f)
                        
                        val scale = minOf(size.width / rangeX, size.height / rangeY) * 0.8f
                        
                        val offsetX = (size.width - rangeX * scale) / 2f - minX * scale
                        val offsetY = (size.height - rangeY * scale) / 2f - minY * scale
                        
                        // Draw path
                        if (allPoints.size > 1) {
                            val drawPath = Path().apply {
                                val start = allPoints.first()
                                moveTo(start.x * scale + offsetX, start.y * scale + offsetY)
                                for (i in 1 until allPoints.size) {
                                    val p = allPoints[i]
                                    lineTo(p.x * scale + offsetX, p.y * scale + offsetY)
                                }
                            }
                            drawPath(drawPath, color = Color.Gray, style = Stroke(width = 2.dp.toPx()))
                        }
                        
                        // Draw current position
                        currentPos?.let {
                            drawCircle(
                                color = Color.Blue,
                                radius = 6.dp.toPx(),
                                center = Offset(it.x * scale + offsetX, it.y * scale + offsetY)
                            )
                        }
                        
                        // Draw start point
                        val start = allPoints.first()
                        drawCircle(
                            color = Color.Green,
                            radius = 4.dp.toPx(),
                            center = Offset(start.x * scale + offsetX, start.y * scale + offsetY)
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Start walking to track path", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanModeSelection(
    onStartQuickScan: () -> Unit,
    onStartSpatialScan: () -> Unit
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
                Text("Instant measurement of RF and Internet quality at your current position.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            onClick = onStartSpatialScan,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Spatial Scan (PDR)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Automatic indoor tracking using phone sensors. No manual setup required.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ActiveScanHeader(
    activeSession: ScanSession,
    isSpatial: Boolean,
    onStopScan: () -> Unit,
    onRecalibrate: () -> Unit,
    onMarkSpot: () -> Unit
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
            
            if (isSpatial) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRecalibrate,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recalibrate", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onMarkSpot,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Spot", style = MaterialTheme.typography.labelMedium)
                    }
                }
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
    val color = when (quality) {
        SignalQuality.EXCELLENT -> SignalExcellent
        SignalQuality.GOOD -> SignalGood
        SignalQuality.FAIR -> SignalFair
        SignalQuality.POOR -> SignalPoor
        SignalQuality.UNAVAILABLE -> SignalUnavailable
    }
    val text = when (quality) {
        SignalQuality.EXCELLENT -> "EXCELLENT"
        SignalQuality.GOOD -> "GOOD"
        SignalQuality.FAIR -> "FAIR"
        SignalQuality.POOR -> "POOR"
        SignalQuality.UNAVAILABLE -> "UNAVAILABLE"
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
                        InfoItem("Relative X", "${"%.2f".format(indoor.x)} m")
                        InfoItem("Relative Y", "${"%.2f".format(indoor.y)} m")
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
            RankedSpotCard("BEST SPOT", spot = summary.bestSpot, containerColor = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f))
            RankedSpotCard("WEAKEST SPOT", spot = summary.worstSpot, containerColor = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.weight(1f))
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

@Composable
private fun PermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Access Required") },
        text = {
            Text("Network Radar needs location access to measure Wi-Fi and GPS signal quality at your position. This is required by Android to perform network scans.")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Allow Access")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@Composable
private fun PermissionDeniedBanner(
    onRequestPermission: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "To measure signal quality, please grant location access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Grant Permission")
            }
        }
    }
}
