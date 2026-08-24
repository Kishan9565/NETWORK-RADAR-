package com.networkradar.feature.history.presentation

import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.core.domain.measurement.ScanSession

data class ScanReportState(
    val scan: ScanSession? = null,
    val summary: ScanIntelligenceSummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isExporting: Boolean = false,
    val exportFileUri: String? = null
)

sealed interface ScanReportAction {
    data object LoadReport : ScanReportAction
    data object ExportCsv : ScanReportAction
    data object ExportJson : ScanReportAction
    data object ShareHeatmap : ScanReportAction
    data object ClearError : ScanReportAction
}

sealed interface ScanReportEvent {
    data class ExportReady(val uri: String, val filename: String) : ScanReportEvent
    data class Error(val message: String) : ScanReportEvent
}
