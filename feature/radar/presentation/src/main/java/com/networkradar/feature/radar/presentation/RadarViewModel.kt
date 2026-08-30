package com.networkradar.feature.radar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorMapLocalDataSource
import com.networkradar.core.domain.measurement.ScanManager
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.util.Result
import com.networkradar.core.domain.util.onFailure
import com.networkradar.core.domain.util.onSuccess
import com.networkradar.feature.radar.domain.AnalyzeScanUseCase
import com.networkradar.feature.radar.domain.ObserveRadarMeasurementsUseCase
import com.networkradar.feature.radar.domain.RadarMeasurement
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
    private val _permissionState = MutableStateFlow(PermissionState())

    // Shared measurement stream to avoid duplicate collection
    private val measurements = observeRadarMeasurementsUseCase()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    val state: StateFlow<RadarState> = combine(
        combine(
            measurements,
            scanManager.activeSession,
            _isSpatialScan,
            indoorDataSource.activeMap,
            _analysisState
        ) { measurement, activeSession, isSpatial, activeMap, analysis ->
            RadarState(
                measurement = measurement,
                activeSession = activeSession,
                isSpatialScan = isSpatial,
                selectedMap = activeMap,
                intelligenceSummary = (analysis as? AnalysisState.Completed)?.summary,
                analyzedSessionId = (analysis as? AnalysisState.Completed)?.sessionId,
                isAnalyzing = analysis is AnalysisState.Loading
            )
        },
        _downloadSpeed,
        _isTestingSpeed,
        _permissionState
    ) { baseState, speed, isTesting, permission ->
        baseState.copy(
            downloadSpeedMbps = speed,
            isTestingSpeed = isTesting,
            isLocationPermissionGranted = permission.isGranted,
            showPermissionRationale = permission.showRationale
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
                
                // Reactive permission check: if ObserveRadarMeasurementsUseCase reports PermissionRequired, update state
                val isGranted = measurement.locationStatus !is com.networkradar.core.domain.location.LocationObservation.PermissionRequired
                if (_permissionState.value.isGranted != isGranted) {
                    _permissionState.update { it.copy(isGranted = isGranted) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: RadarAction) {
        when (action) {
            is RadarAction.StartQuickScan -> {
                if (!_permissionState.value.isGranted) {
                    _permissionState.update { it.copy(showRationale = true) }
                    return
                }
                viewModelScope.launch {
                    _analysisState.value = AnalysisState.Idle
                    _isSpatialScan.value = false
                    scanManager.startScan(action.name, null)
                }
            }
            is RadarAction.StartSpatialScan -> {
                if (!_permissionState.value.isGranted) {
                    _permissionState.update { it.copy(showRationale = true) }
                    return
                }
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
            is RadarAction.PermissionResult -> {
                _permissionState.update { it.copy(isGranted = action.granted, showRationale = false) }
                if (!action.granted) {
                    // Logic to show "Open Settings" button is handled in UI based on state
                }
            }
            RadarAction.RequestPermission -> {
                viewModelScope.launch {
                    _events.send(RadarEvent.RequestLocationPermission)
                }
            }
            RadarAction.DismissRationale -> {
                _permissionState.update { it.copy(showRationale = false) }
            }
        }
    }

    private fun runSpeedTest() {
        viewModelScope.launch {
            _isTestingSpeed.value = true
            _downloadSpeed.value = null
            
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
    
    private data class PermissionState(
        val isGranted: Boolean = true,
        val showRationale: Boolean = false
    )
}
