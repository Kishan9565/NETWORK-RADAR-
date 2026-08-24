package com.networkradar.feature.comparison.presentation

import com.networkradar.core.domain.measurement.ScanComparison
import com.networkradar.core.domain.measurement.ScanSession

data class ComparisonState(
    val scans: List<ScanSession> = emptyList(),
    val selectedIdA: String? = null,
    val selectedIdB: String? = null,
    val comparison: ScanComparison? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ComparisonAction {
    data class SelectScanA(val id: String) : ComparisonAction
    data class SelectScanB(val id: String) : ComparisonAction
    data object Compare : ComparisonAction
    data object ClearError : ComparisonAction
}

sealed interface ComparisonEvent {
    data class Error(val message: String) : ComparisonEvent
}
