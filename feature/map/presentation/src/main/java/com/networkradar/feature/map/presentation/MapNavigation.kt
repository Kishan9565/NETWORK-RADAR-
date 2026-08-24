package com.networkradar.feature.map.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object MapRoute

fun NavController.navigateToMap(navOptions: NavOptions? = null) {
    navigate(MapRoute, navOptions)
}

fun NavGraphBuilder.mapGraph(
    onPositionConfirmed: () -> Unit
) {
    composable<MapRoute> {
        MapRoot(
            onPositionConfirmed = onPositionConfirmed
        )
    }
}
