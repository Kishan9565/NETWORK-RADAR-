package com.networkradar.feature.comparison.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object ComparisonRoute

fun NavController.navigateToComparison(navOptions: NavOptions? = null) {
    navigate(ComparisonRoute, navOptions)
}

fun NavGraphBuilder.comparisonGraph() {
    composable<ComparisonRoute> {
        ComparisonRoot()
    }
}
