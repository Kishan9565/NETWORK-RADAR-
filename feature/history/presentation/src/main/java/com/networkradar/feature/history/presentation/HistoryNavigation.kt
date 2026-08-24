package com.networkradar.feature.history.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

@Serializable
data class ScanReportRoute(val sessionId: String)

fun NavController.navigateToHistory(navOptions: NavOptions? = null) {
    navigate(HistoryRoute, navOptions)
}

fun NavController.navigateToScanReport(sessionId: String, navOptions: NavOptions? = null) {
    navigate(ScanReportRoute(sessionId), navOptions)
}

fun NavGraphBuilder.historyGraph(
    onNavigateToScan: (String) -> Unit,
    onViewHeatmap: (String) -> Unit
) {
    composable<HistoryRoute> {
        HistoryRoot(onNavigateToScan = onNavigateToScan)
    }
    composable<ScanReportRoute> { backStackEntry ->
        val route: ScanReportRoute = backStackEntry.toRoute()
        ScanReportRoot(
            sessionId = route.sessionId,
            onViewHeatmap = onViewHeatmap
        )
    }
}
