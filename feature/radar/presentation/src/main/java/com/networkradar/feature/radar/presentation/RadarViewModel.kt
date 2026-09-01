package com.networkradar.feature.radar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.indoor.PdrDataSource
import com.networkradar.core.domain.indoor.SpatialAnnotation
import com.networkradar.core.domain.indoor.SpatialAnnotationLocalDataSource
import com.networkradar.core.domain.measurement.ScanManager
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
import java.util.UUID

class RadarViewModel(
    private val observeRadarMeasurementsUseCase: ObserveRadarMeasurementsUseCase,
    private val analyzeScanUseCase: AnalyzeScanUseCase,
    private val runDownloadTestUseCase: RunDownloadTestUseCase,
    private val scanManager: ScanManager,
    private val pdrDataSource: PdrDataSource,
    private val annotationDataSource: SpatialAnnotationLocalDataSource
) : ViewModel() {

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    private val _isSpatialScan = MutableStateFlow(false)
    private val _downloadSpeed = MutableStateFlow<Double?>(null)
    private val _isTestingSpeed = MutableStateFlow(false)
    private val _permissionState = MutableStateFlow(PermissionState())
    private val _spatialPath = MutableStateFlow<List<IndoorPosition>>(emptyList())

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
            _spatialPath,
            _analysisState
        ) { measurement, activeSession, isSpatial, spatialPath, analysis ->
            RadarState(
                measurement = measurement,
                activeSession = activeSession,
                isSpatialScan = isSpatial,
                currentIndoorPosition = measurement.point.indoorPosition,
                spatialPath = spatialPath,
                isPdrAvailable = pdrDataSource.isAvailable,
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
                    
                    // Accumulate path for spatial scan
                    if (_isSpatialScan.value) {
                        measurement.point.indoorPosition?.let { pos ->
                            _spatialPath.update { it + pos }
                        }
                    }
                }
                
                // Reactive permission check
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
                    _spatialPath.value = emptyList()
                    scanManager.startScan(action.name, isSpatial = false)
                }
            }
            is RadarAction.StartSpatialScan -> {
                if (!_permissionState.value.isGranted) {
                    _permissionState.update { it.copy(showRationale = true) }
                    return
                }
                if (!pdrDataSource.isAvailable) {
                    viewModelScope.launch {
                        _events.send(RadarEvent.Error("Step tracking sensors are not available on this device."))
                    }
                    return
                }
                viewModelScope.launch {
                    _analysisState.value = AnalysisState.Idle
                    _isSpatialScan.value = true
                    _spatialPath.value = emptyList()
                    
                    val result = scanManager.startScan(action.name, isSpatial = true)
                    result.onSuccess { session ->
                        pdrDataSource.startTracking(session.id)
                    }
                }
            }
            RadarAction.StopScan -> {
                viewModelScope.launch {
                    val sessionId = scanManager.activeSession.value?.id
                    pdrDataSource.stopTracking()
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
            RadarAction.RecalibratePosition -> {
                pdrDataSource.resetOrigin()
                _spatialPath.value = emptyList()
            }
            is RadarAction.MarkSpot -> {
                val sessionId = scanManager.activeSession.value?.id ?: return
                val currentPos = state.value.currentIndoorPosition ?: return
                viewModelScope.launch {
                    annotationDataSource.saveAnnotation(
                        SpatialAnnotation(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            x = currentPos.x,
                            y = currentPos.y,
                            label = action.label,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            is RadarAction.PermissionResult -> {
                _permissionState.update { it.copy(isGranted = action.granted, showRationale = false) }
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
