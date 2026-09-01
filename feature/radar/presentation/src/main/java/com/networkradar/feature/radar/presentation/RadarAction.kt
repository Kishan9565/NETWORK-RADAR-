package com.networkradar.feature.radar.presentation

sealed interface RadarAction {
    data class StartQuickScan(val name: String) : RadarAction
    data class StartSpatialScan(val name: String) : RadarAction
    data object StopScan : RadarAction
    data object RunSpeedTest : RadarAction
    data object RecalibratePosition : RadarAction
    data class MarkSpot(val label: String?) : RadarAction
    data class PermissionResult(val granted: Boolean) : RadarAction
    data object RequestPermission : RadarAction
    data object DismissRationale : RadarAction
}
