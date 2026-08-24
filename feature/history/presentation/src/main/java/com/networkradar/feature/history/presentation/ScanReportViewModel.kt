package com.networkradar.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import com.networkradar.core.domain.util.Result
import com.networkradar.feature.history.domain.ExportScanUseCase
import com.networkradar.feature.history.domain.GetScanReportUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanReportViewModel(
    private val sessionId: String,
    private val scanSessionDataSource: ScanSessionLocalDataSource,
    private val getScanReportUseCase: GetScanReportUseCase,
    private val exportScanUseCase: ExportScanUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScanReportState())
    val state = _state.asStateFlow()

    private val _events = Channel<ScanReportEvent>()
    val events = _events.receiveAsFlow()

    init {
        onAction(ScanReportAction.LoadReport)
    }

    fun onAction(action: ScanReportAction) {
        when (action) {
            ScanReportAction.LoadReport -> loadReport()
            ScanReportAction.ExportCsv -> exportCsv()
            ScanReportAction.ExportJson -> exportJson()
            ScanReportAction.ShareHeatmap -> { /* Optional: PNG Export logic */ }
            ScanReportAction.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadReport() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val sessionResult = scanSessionDataSource.getSessionById(sessionId)
            if (sessionResult is Result.Error) {
                _state.update { it.copy(isLoading = false, error = "Scan not found") }
                return@launch
            }
            val session = (sessionResult as Result.Success).data
            
            val reportResult = getScanReportUseCase(sessionId)
            when (reportResult) {
                is Result.Success -> {
                    _state.update { it.copy(
                        scan = session,
                        summary = reportResult.data,
                        isLoading = false
                    ) }
                }
                is Result.Error -> {
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Failed to analyze scan"
                    ) }
                }
            }
        }
    }

    private fun exportCsv() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            val result = exportScanUseCase.toCsv(sessionId)
            when (result) {
                is Result.Success -> {
                    _events.send(ScanReportEvent.ExportReady(result.data, "scan_$sessionId.csv"))
                }
                is Result.Error -> {
                    _events.send(ScanReportEvent.Error("Export failed"))
                }
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    private fun exportJson() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            val result = exportScanUseCase.toJson(sessionId)
            when (result) {
                is Result.Success -> {
                    _events.send(ScanReportEvent.ExportReady(result.data, "scan_$sessionId.json"))
                }
                is Result.Error -> {
                    _events.send(ScanReportEvent.Error("Export failed"))
                }
            }
            _state.update { it.copy(isExporting = false) }
        }
    }
}
