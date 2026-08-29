package com.networkradar.feature.dashboard.presentation

import com.networkradar.core.domain.measurement.ScanSession

data class DashboardState(
    val lastScan: ScanSession? = null,
    val isLoading: Boolean = false
)
