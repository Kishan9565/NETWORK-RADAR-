package com.networkradar.feature.dashboard.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

fun NavGraphBuilder.dashboardGraph(
    onNavigateToRadar: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMaps: () -> Unit,
    onNavigateToComparison: () -> Unit
) {
    composable<DashboardRoute> {
        DashboardRoot(
            onNavigateToRadar = onNavigateToRadar,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToMaps = onNavigateToMaps,
            onNavigateToComparison = onNavigateToComparison
        )
    }
}
