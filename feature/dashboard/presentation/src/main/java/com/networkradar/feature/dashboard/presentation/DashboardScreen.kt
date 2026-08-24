package com.networkradar.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spatial Connectivity Intelligence",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { onAction(DashboardAction.StartScan) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start New Scan")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onAction(DashboardAction.ViewHistory) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan History")
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onAction(DashboardAction.CompareScans) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Compare Scans")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onAction(DashboardAction.ManageMaps) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Floor Plans")
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
