package com.networkradar.feature.map.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.feature.map.presentation.components.IndoorMapCanvas
import org.koin.androidx.compose.koinViewModel

@Composable
fun MapRoot(
    viewModel: MapViewModel = koinViewModel(),
    onPositionConfirmed: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                MapEvent.PositionSelected -> onPositionConfirmed()
            }
        }
    }

    MapScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    state: MapState,
    onAction: (MapAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Indoor Map") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (state.activeMap == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        // For Phase 5, we "create" a default map since persistence is deferred
                        onAction(MapAction.LoadMap(
                            IndoorMap(
                                id = "default_map",
                                name = "Office Floor 1",
                                width = 20f, // 20 meters
                                height = 15f, // 15 meters
                                createdAt = System.currentTimeMillis()
                            )
                        ))
                    }) {
                        Text("Create Test Map")
                    }
                }
            } else {
                Text(
                    text = "Map: ${state.activeMap.name}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Dimensions: ${state.activeMap.width}m x ${state.activeMap.height}m",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                state.error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    IndoorMapCanvas(
                        indoorMap = state.activeMap,
                        currentPosition = state.currentPosition,
                        onPositionSelected = { x, y ->
                            onAction(MapAction.SelectPosition(x, y))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                state.currentPosition?.let {
                    Text(
                        text = "Selected Position: X=${"%.2f".format(it.x)}m, Y=${"%.2f".format(it.y)}m",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = { /* In a real app, this might confirm and navigate back */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.currentPosition != null
                ) {
                    Text("Confirm Position")
                }
            }
        }
    }
}
