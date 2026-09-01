package com.networkradar.feature.dashboard.presentation

sealed interface DashboardEvent {
    data object NavigateToRadar : DashboardEvent
    data object NavigateToHistory : DashboardEvent
    data object NavigateToComparison : DashboardEvent
}
