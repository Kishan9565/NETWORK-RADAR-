package com.networkradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.networkradar.core.designsystem.NetworkRadarTheme
import com.networkradar.feature.comparison.presentation.comparisonGraph
import com.networkradar.feature.comparison.presentation.navigateToComparison
import com.networkradar.feature.dashboard.presentation.DashboardRoute
import com.networkradar.feature.dashboard.presentation.dashboardGraph
import com.networkradar.feature.heatmap.presentation.heatmapGraph
import com.networkradar.feature.heatmap.presentation.navigateToHeatmap
import com.networkradar.feature.history.presentation.historyGraph
import com.networkradar.feature.history.presentation.navigateToHistory
import com.networkradar.feature.history.presentation.navigateToScanReport
import com.networkradar.feature.map.presentation.MapRoute
import com.networkradar.feature.map.presentation.mapGraph
import com.networkradar.feature.map.presentation.navigateToMap
import com.networkradar.feature.radar.presentation.RadarRoute
import com.networkradar.feature.radar.presentation.navigateToRadar
import com.networkradar.feature.radar.presentation.radarGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkRadarTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute
                ) {
                    dashboardGraph(
                        onNavigateToRadar = {
                            navController.navigateToRadar()
                        },
                        onNavigateToHistory = {
                            navController.navigateToHistory()
                        },
                        onNavigateToMaps = {
                            navController.navigateToMap()
                        },
                        onNavigateToComparison = {
                            navController.navigateToComparison()
                        }
                    )
                    radarGraph(
                        onViewHeatmap = { sessionId ->
                            navController.navigateToHeatmap(sessionId)
                        }
                    )
                    historyGraph(
                        onNavigateToScan = { sessionId ->
                            navController.navigateToScanReport(sessionId)
                        },
                        onViewHeatmap = { sessionId ->
                            navController.navigateToHeatmap(sessionId)
                        }
                    )
                    mapGraph(
                        onPositionConfirmed = {
                            navController.popBackStack()
                        }
                    )
                    heatmapGraph()
                    comparisonGraph()
                }
            }
        }
    }
}
