package com.networkradar.feature.radar.presentation

sealed interface RadarAction {
    data class StartScan(val name: String) : RadarAction
    data object StopScan : RadarAction
}
