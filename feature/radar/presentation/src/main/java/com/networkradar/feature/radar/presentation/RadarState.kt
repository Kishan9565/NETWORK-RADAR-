package com.networkradar.feature.radar.presentation

import com.networkradar.core.domain.measurement.ScanIntelligenceSummary
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.feature.radar.domain.RadarMeasurement

data class RadarState(
    val measurement: RadarMeasurement? = null,
    val activeSession: ScanSession? = null,
    val intelligenceSummary: ScanIntelligenceSummary? = null,
    val analyzedSessionId: String? = null,
    val isAnalyzing: Boolean = false
)
