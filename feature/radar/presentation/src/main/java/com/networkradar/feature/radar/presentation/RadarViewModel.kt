package com.networkradar.feature.radar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.indoor.IndoorMapLocalDataSource
import com.networkradar.core.domain.measurement.ScanManager
import com.networkradar.core.domain.util.Result
import com.networkradar.core.domain.util.onFailure
import com.networkradar.core.domain.util.onSuccess
import com.networkradar.feature.radar.domain.AnalyzeScanUseCase
import com.networkradar.feature.radar.domain.ObserveRadarMeasurementsUseCase
import com.networkradar.feature.speedtest.domain.RunDownloadTestUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RadarViewModel(
    private val observeRadarMeasurementsUseCase: ObserveRadarMeasurementsUseCase,
    private val analyzeScanUseCase: AnalyzeScanUseCase,
    private val runDownloadTestUseCase: RunDownloadTestUseCase,
    private val scanManager: ScanManager,
    private val indoorDataSource: IndoorDataSource,
    private val mapDataSource: IndoorMapLocalDataSource
) : ViewModel() {

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    private val _isSpatialScan = MutableStateFlow(false)
    private val _downloadSpeed = MutableStateFlow<Double?>(null)
    private val _isTestingSpeed = MutableStateFlow(false)

    // Shared measurement stream to avoid duplicate collection
    private val measurements = observeRadarMeasurementsUseCase()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    val state: StateFlow<RadarState> = combine(
        measurements,
        scanManager.activeSession,
        _isSpatialScan,
        indoorDataSource.activeMap,
        _analysisState,
        _downloadSpeed,
        _isTestingSpeed
    ) { measurement, activeSession, isSpatial, activeMap, analysis, speed, isTesting ->
        RadarState(
            measurement = measurement,
            activeSession = activeSession,
            isSpatialScan = isSpatial,
            selectedMap = activeMap,
            intelligenceSummary = (analysis as? AnalysisState.Completed)?.summary,
            analyzedSessionId = (analysis as? AnalysisState.Completed)?.sessionId,
            isAnalyzing = analysis is AnalysisState.Loading,
            downloadSpeedMbps = speed,
            isTestingSpeed = isTesting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RadarState()
    )

    private val _events = Channel<RadarEvent>()
    val events = _events.receiveAsFlow()

    init {
        // Shared stream used for recording
        measurements
            .onEach { measurement ->
                if (scanManager.activeSession.value != null) {
                    scanManager.recordMeasurement(measurement.point)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: RadarAction) {
        when (action) {
            is RadarAction.StartQuickScan -> {
                viewModelScope.launch {
                    _analysisState.value = AnalysisState.Idle
                    _isSpatialScan.value = false
                    scanManager.startScan(action.name, null)
                }
            }
            is RadarAction.StartSpatialScan -> {
                viewModelScope.launch {
                    _analysisState.value = AnalysisState.Idle
                    _isSpatialScan.value = true
                    
                    val mapResult = mapDataSource.getMapById(action.mapId)
                    if (mapResult is Result.Success && mapResult.data != null) {
                        indoorDataSource.setActiveMap(mapResult.data)
                        scanManager.startScan(action.name, action.mapId)
                    } else {
                         _events.send(RadarEvent.Error("Selected floor plan not found."))
                    }
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
            RadarAction.RunSpeedTest -> {
                runSpeedTest()
            }
        }
    }

    private fun runSpeedTest() {
        viewModelScope.launch {
            _isTestingSpeed.value = true
            _downloadSpeed.value = null
            
            // Using a reliable test file URL (e.g. from a known CDN or speed test service)
            // In a real app this would be configurable.
            val testUrl = "https://speed.cloudflare.com/__down?bytes=10000000" 
            
            runDownloadTestUseCase(testUrl)
                .onEach { result ->
                    when (result) {
                        is Result.Success -> {
                            _downloadSpeed.value = result.data
                        }
                        is Result.Error -> {
                            _events.send(RadarEvent.Error("Speed test failed: ${result.error}"))
                        }
                    }
                }
                .launchIn(viewModelScope)
                .invokeOnCompletion {
                    _isTestingSpeed.value = false
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
