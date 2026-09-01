package com.networkradar.feature.radar.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object RadarRoute

fun NavController.navigateToRadar(navOptions: NavOptions? = null) {
    navigate(RadarRoute, navOptions)
}

fun NavGraphBuilder.radarGraph(
    onViewHeatmap: (String) -> Unit
) {
    composable<RadarRoute> {
        RadarRoot(
            onViewHeatmap = onViewHeatmap
        )
    }
}
