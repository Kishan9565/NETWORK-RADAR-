package com.networkradar.feature.heatmap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.indoor.SpatialAnnotationLocalDataSource
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import com.networkradar.core.domain.util.Result
import com.networkradar.feature.heatmap.domain.GenerateHeatmapUseCase
import com.networkradar.feature.heatmap.domain.HeatmapMetric
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HeatmapViewModel(
    private val sessionDataSource: ScanSessionLocalDataSource,
    private val annotationDataSource: SpatialAnnotationLocalDataSource,
    private val generateHeatmapUseCase: GenerateHeatmapUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HeatmapState())
    val state: StateFlow<HeatmapState> = _state.asStateFlow()

    private val _events = Channel<HeatmapEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: HeatmapAction) {
        when (action) {
            is HeatmapAction.SelectMetric -> {
                _state.update { it.copy(selectedMetric = action.metric) }
                generateHeatmap()
            }
            is HeatmapAction.LoadSession -> {
                _state.update { it.copy(selectedSessionId = action.sessionId) }
                loadSession(action.sessionId)
            }
            is HeatmapAction.SetViewMode -> {
                _state.update { it.copy(viewMode = action.mode) }
            }
            HeatmapAction.Refresh -> {
                generateHeatmap()
            }
        }
    }

    private fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val sessionResult = sessionDataSource.getSessionById(sessionId)
            if (sessionResult is Result.Error) {
                _state.update { it.copy(isLoading = false, error = "Failed to load scan session.") }
                return@launch
            }

            val session = (sessionResult as Result.Success).data
            if (session == null) {
                _state.update { it.copy(isLoading = false, error = "Scan session not found.") }
                return@launch
            }

            if (!session.isSpatial) {
                _state.update { it.copy(isLoading = false, error = "This was a Quick Scan — no spatial data to map.") }
                return@launch
            }

            // Load annotations
            val annotations = annotationDataSource.getAnnotationsForSession(sessionId).first()
            _state.update { it.copy(annotations = annotations) }

            generateHeatmap()
        }
    }

    private fun generateHeatmap() {
        val currentState = _state.value
        val sessionId = currentState.selectedSessionId

        if (sessionId == null) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val result = generateHeatmapUseCase(
                sessionId = sessionId,
                metric = currentState.selectedMetric
            )

            when (result) {
                is Result.Success -> {
                    val cells = result.data.cells
                    if (cells.isEmpty() && result.data.sourcePoints.isEmpty()) {
                         _state.update { it.copy(
                            isLoading = false,
                            error = "Not enough movement recorded to build a heatmap."
                        ) }
                        return@launch
                    }

                    val validValues = cells.mapNotNull { it.value }
                    val range = if (validValues.isNotEmpty()) {
                        MetricRange(
                            min = validValues.min(),
                            max = validValues.max(),
                            isFixed = false
                        )
                    } else null

                    _state.update { it.copy(
                        heatmapCells = cells,
                        sourcePoints = result.data.sourcePoints,
                        metricRange = range,
                        isLoading = false
                    ) }
                }
                is Result.Error -> {
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Failed to generate heatmap."
                    ) }
                    _events.send(HeatmapEvent.Error("Failed to generate heatmap"))
                }
            }
        }
    }
}
