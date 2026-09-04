package com.networkradar.feature.heatmap.presentation

import com.networkradar.core.domain.indoor.SpatialAnnotation
import com.networkradar.feature.heatmap.domain.HeatmapCell
import com.networkradar.feature.heatmap.domain.HeatmapEngine
import com.networkradar.feature.heatmap.domain.HeatmapMetric

enum class HeatmapViewMode {
    SIMPLE, DETAILED
}

data class HeatmapState(
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val sourcePoints: List<HeatmapEngine.WeightedPoint> = emptyList(),
    val annotations: List<SpatialAnnotation> = emptyList(),
    val selectedMetric: HeatmapMetric = HeatmapMetric.WIFI_RSSI,
    val selectedSessionId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val metricRange: MetricRange? = null,
    val viewMode: HeatmapViewMode = HeatmapViewMode.SIMPLE
)

data class MetricRange(
    val min: Double,
    val max: Double,
    val isFixed: Boolean
)

sealed interface HeatmapAction {
    data class SelectMetric(val metric: HeatmapMetric) : HeatmapAction
    data class LoadSession(val sessionId: String) : HeatmapAction
    data class SetViewMode(val mode: HeatmapViewMode) : HeatmapAction
    data object Refresh : HeatmapAction
}

sealed interface HeatmapEvent {
    data class Error(val message: String) : HeatmapEvent
}
