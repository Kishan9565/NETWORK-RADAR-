package com.networkradar.feature.radar.presentation

import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.indoor.SpatialAnnotation
import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.feature.radar.domain.RadarMeasurement

data class RadarState(
    val measurement: RadarMeasurement? = null,
    val activeSession: ScanSession? = null,
    val isSpatialScan: Boolean = false,
    val scanStartTime: Long = 0L,
    val currentIndoorPosition: IndoorPosition? = null,
    val spatialPath: List<IndoorPosition> = emptyList(),
    val annotations: List<SpatialAnnotation> = emptyList(),
    val isPdrAvailable: Boolean = true,
    val intelligenceSummary: ScanIntelligenceSummary? = null,
    val analyzedSessionId: String? = null,
    val isAnalyzing: Boolean = false,
    val downloadSpeedMbps: Double? = null,
    val latencyMs: Double? = null,
    val isTestingSpeed: Boolean = false,
    val isLocationPermissionGranted: Boolean = true,
    val isActivityPermissionGranted: Boolean = true,
    val isPhoneStatePermissionGranted: Boolean = true,
    val showPermissionRationale: Boolean = false
)
