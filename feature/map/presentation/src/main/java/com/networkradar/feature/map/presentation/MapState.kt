package com.networkradar.feature.map.presentation

import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorPosition

data class MapState(
    val activeMap: IndoorMap? = null,
    val currentPosition: IndoorPosition? = null,
    val error: String? = null
)

sealed interface MapAction {
    data class LoadMap(val map: IndoorMap) : MapAction
    data class SelectPosition(val x: Float, val y: Float) : MapAction
    data object ClearError : MapAction
}

sealed interface MapEvent {
    data object PositionSelected : MapEvent
}
