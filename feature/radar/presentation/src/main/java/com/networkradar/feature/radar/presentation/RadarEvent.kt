package com.networkradar.feature.radar.presentation

sealed interface RadarEvent {
    data class Error(val message: String) : RadarEvent
}
