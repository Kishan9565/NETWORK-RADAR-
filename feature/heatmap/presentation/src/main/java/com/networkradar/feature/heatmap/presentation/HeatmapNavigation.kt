package com.networkradar.feature.heatmap.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class HeatmapRoute(val sessionId: String)

fun NavController.navigateToHeatmap(sessionId: String, navOptions: NavOptions? = null) {
    navigate(HeatmapRoute(sessionId), navOptions)
}

fun NavGraphBuilder.heatmapGraph() {
    composable<HeatmapRoute> { backStackEntry ->
        val route: HeatmapRoute = backStackEntry.toRoute()
        HeatmapRoot(sessionId = route.sessionId)
    }
}
