package com.networkradar.feature.dashboard.presentation

sealed interface DashboardAction {
    data object StartScan : DashboardAction
    data object ViewHistory : DashboardAction
    data object CompareScans : DashboardAction
}
