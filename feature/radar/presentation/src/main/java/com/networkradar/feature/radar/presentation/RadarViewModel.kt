package com.networkradar.feature.radar.presentation

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.indoor.PdrDataSource
import com.networkradar.core.domain.indoor.SpatialAnnotation
import com.networkradar.core.domain.indoor.SpatialAnnotationLocalDataSource
import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.core.domain.measurement.ScanManager
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.util.Result
import com.networkradar.core.domain.util.onFailure
import com.networkradar.core.domain.util.onSuccess
import com.networkradar.feature.radar.domain.AnalyzeScanUseCase
import com.networkradar.feature.radar.domain.ObserveRadarMeasurementsUseCase
import com.networkradar.feature.radar.domain.RadarMeasurement
import com.networkradar.feature.speedtest.domain.RunDownloadTestUseCase
import com.networkradar.feature.speedtest.domain.RunLatencyTestUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val runLatencyTestUseCase: RunLatencyTestUseCase,
    private val scanManager: ScanManager,
    private val pdrDataSource: PdrDataSource,
    private val annotationDataSource: SpatialAnnotationLocalDataSource
) : ViewModel() {

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    private val _isSpatialScan = MutableStateFlow(false)
    private val _downloadSpeed = MutableStateFlow<Double?>(null)
    private val _latencyMs = MutableStateFlow<Double?>(null)
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

    private val annotations = scanManager.activeSession.flatMapLatest { session ->
        if (session != null) {
            annotationDataSource.getAnnotationsForSession(session.id)
        } else {
            flowOf(emptyList())
        }
    }

    // Grouping flows to avoid complex combine overloads
    private val coreFlow = combine(
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
            intelligenceSummary = (analysis as? AnalysisState.Completed)?.summary,
            analyzedSessionId = (analysis as? AnalysisState.Completed)?.sessionId,
            isAnalyzing = analysis is AnalysisState.Loading
        )
    }

    private val auxiliaryFlow = combine(
        annotations,
        _downloadSpeed,
        _latencyMs,
        _isTestingSpeed,
        _permissionState
    ) { annoList, speed, latency, isTesting, permission ->
        AuxState(annoList, speed, latency, isTesting, permission)
    }

    val state: StateFlow<RadarState> = combine(coreFlow, auxiliaryFlow) { core, aux ->
        core.copy(
            annotations = aux.annotations,
            downloadSpeedMbps = aux.downloadSpeed,
            latencyMs = aux.latency,
            isTestingSpeed = aux.isTesting,
            isLocationPermissionGranted = aux.permission.locationGranted,
            isActivityPermissionGranted = aux.permission.activityGranted,
            isPhoneStatePermissionGranted = aux.permission.phoneStateGranted,
            showPermissionRationale = aux.permission.showRationale,
            isPdrAvailable = pdrDataSource.isAvailable
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
                
                // Reactive permission check (location)
                val locGranted = measurement.locationStatus !is com.networkradar.core.domain.location.LocationObservation.PermissionRequired
                if (_permissionState.value.locationGranted != locGranted) {
                    _permissionState.update { it.copy(locationGranted = locGranted) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: RadarAction) {
        when (action) {
            is RadarAction.StartQuickScan -> {
                if (!_permissionState.value.locationGranted) {
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
                if (!_permissionState.value.locationGranted) {
                    _permissionState.update { it.copy(showRationale = true) }
                    return
                }
                
                if (!pdrDataSource.isHardwareAvailable) {
                    viewModelScope.launch {
                        _events.send(RadarEvent.Error("Step tracking sensors are not available on this device."))
                    }
                    return
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !_permissionState.value.activityGranted) {
                    viewModelScope.launch {
                        _events.send(RadarEvent.Error("Physical Activity permission is required for Spatial Scan."))
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
                val locGranted = action.results[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                                action.results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    action.results[Manifest.permission.ACTIVITY_RECOGNITION] == true
                } else true
                val phoneGranted = action.results[Manifest.permission.READ_PHONE_STATE] == true
                
                _permissionState.update { 
                    it.copy(
                        locationGranted = locGranted, 
                        activityGranted = activityGranted,
                        phoneStateGranted = phoneGranted,
                        showRationale = false 
                    ) 
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
            _latencyMs.value = null
            
            // Run Latency first (it's quick)
            val latencyResult = runLatencyTestUseCase("8.8.8.8")
            if (latencyResult is Result.Success) {
                _latencyMs.value = latencyResult.data
            }

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
            val summary: ScanIntelligenceSummary
        ) : AnalysisState
    }
    
    private data class PermissionState(
        val locationGranted: Boolean = true,
        val activityGranted: Boolean = true,
        val phoneStateGranted: Boolean = true,
        val showRationale: Boolean = false
    )

    private data class AuxState(
        val annotations: List<SpatialAnnotation>,
        val downloadSpeed: Double?,
        val latency: Double?,
        val isTesting: Boolean,
        val permission: PermissionState
    )
}
