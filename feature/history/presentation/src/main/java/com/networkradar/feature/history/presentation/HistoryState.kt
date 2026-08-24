package com.networkradar.feature.history.presentation

import com.networkradar.core.domain.measurement.ScanSession

data class HistoryState(
    val scans: List<ScanSession> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface HistoryAction {
    data class DeleteScan(val sessionId: String) : HistoryAction
    data class OpenScan(val sessionId: String) : HistoryAction
    data object ClearError : HistoryAction
}

sealed interface HistoryEvent {
    data class NavigateToScan(val sessionId: String) : HistoryEvent
    data class ShowMessage(val message: String) : HistoryEvent
}
