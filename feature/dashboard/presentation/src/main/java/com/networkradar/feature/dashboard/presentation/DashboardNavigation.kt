package com.networkradar.feature.dashboard.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

fun NavGraphBuilder.dashboardGraph(
    onNavigateToRadar: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToComparison: () -> Unit
) {
    composable<DashboardRoute> {
        DashboardRoot(
            onNavigateToRadar = onNavigateToRadar,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToComparison = onNavigateToComparison
        )
    }
}
