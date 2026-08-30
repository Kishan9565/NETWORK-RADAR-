package com.networkradar.feature.radar.presentation

import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.feature.radar.domain.RadarMeasurement

data class RadarState(
    val measurement: RadarMeasurement? = null,
    val activeSession: ScanSession? = null,
    val isSpatialScan: Boolean = false,
    val selectedMap: IndoorMap? = null,
    val intelligenceSummary: ScanIntelligenceSummary? = null,
    val analyzedSessionId: String? = null,
    val isAnalyzing: Boolean = false,
    val downloadSpeedMbps: Double? = null,
    val isTestingSpeed: Boolean = false,
    val isLocationPermissionGranted: Boolean = true, // Default to true to avoid flash, will be updated by VM
    val showPermissionRationale: Boolean = false
)
