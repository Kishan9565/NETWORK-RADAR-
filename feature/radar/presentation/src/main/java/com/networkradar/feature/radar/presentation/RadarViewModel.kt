package com.networkradar.feature.radar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.measurement.ScanManager
import com.networkradar.core.domain.util.onFailure
import com.networkradar.core.domain.util.onSuccess
import com.networkradar.feature.radar.domain.AnalyzeScanUseCase
import com.networkradar.feature.radar.domain.ObserveRadarMeasurementsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadarViewModel(
    private val observeRadarMeasurementsUseCase: ObserveRadarMeasurementsUseCase,
    private val analyzeScanUseCase: AnalyzeScanUseCase,
    private val scanManager: ScanManager
) : ViewModel() {

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)

    val state = combine(
        observeRadarMeasurementsUseCase(),
        scanManager.activeSession,
        _analysisState
    ) { measurement, activeSession, analysis ->
        RadarState(
            measurement = measurement,
            activeSession = activeSession,
            intelligenceSummary = (analysis as? AnalysisState.Completed)?.summary,
            analyzedSessionId = (analysis as? AnalysisState.Completed)?.sessionId,
            isAnalyzing = analysis is AnalysisState.Loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RadarState()
    )

    private val _events = Channel<RadarEvent>()
    val events = _events.receiveAsFlow()

    init {
        // Automatic recording of measurements when a scan is active
        observeRadarMeasurementsUseCase()
            .onEach { measurement ->
                if (scanManager.activeSession.value != null) {
                    scanManager.recordMeasurement(measurement.point)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: RadarAction) {
        when (action) {
            is RadarAction.StartScan -> {
                viewModelScope.launch {
                    _analysisState.value = AnalysisState.Idle
                    val activeMapId = state.value.measurement?.point?.indoorPosition?.mapId
                    scanManager.startScan(action.name, activeMapId)
                }
            }
            RadarAction.StopScan -> {
                viewModelScope.launch {
                    val sessionId = scanManager.activeSession.value?.id
                    scanManager.stopScan().onSuccess {
                        if (sessionId != null) {
                            performAnalysis(sessionId)
                        }
                    }
                }
            }
        }
    }

    private fun performAnalysis(sessionId: String) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading
            analyzeScanUseCase(sessionId)
                .onSuccess { summary ->
                    _analysisState.value = AnalysisState.Completed(sessionId, summary)
                }
                .onFailure {
                    _analysisState.value = AnalysisState.Idle
                }
        }
    }

    private sealed interface AnalysisState {
        data object Idle : AnalysisState
        data object Loading : AnalysisState
        data class Completed(
            val sessionId: String,
            val summary: com.networkradar.core.domain.measurement.ScanIntelligenceSummary
        ) : AnalysisState
    }
}
