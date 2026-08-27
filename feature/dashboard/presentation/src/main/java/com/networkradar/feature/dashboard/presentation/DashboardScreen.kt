package com.networkradar.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.designsystem.NetworkRadarTheme
import com.networkradar.core.presentation.util.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardRoot(
    onNavigateToRadar: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMaps: () -> Unit,
    onNavigateToComparison: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            DashboardEvent.NavigateToRadar -> onNavigateToRadar()
            DashboardEvent.NavigateToHistory -> onNavigateToHistory()
            DashboardEvent.NavigateToMaps -> onNavigateToMaps()
            DashboardEvent.NavigateToComparison -> onNavigateToComparison()
        }
    }

    DashboardScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    onAction: (DashboardAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Network Radar")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Spatial Connectivity Intelligence",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Measure Internet, Wi-Fi, Cellular and RF quality at real locations.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "SCANS", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onAction(DashboardAction.StartScan) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("START NEW SCAN")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onAction(DashboardAction.ViewHistory) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("VIEW HISTORY")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "ANALYSIS & TOOLS", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onAction(DashboardAction.CompareScans) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("COMPARE SCANS")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onAction(DashboardAction.ManageMaps) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("MANAGE FLOOR PLANS")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DashboardScreenPreview() {
    NetworkRadarTheme {
        DashboardScreen(
            state = DashboardState(),
            onAction = {}
        )
    }
}
