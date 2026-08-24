package com.networkradar.feature.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.util.Result
import com.networkradar.feature.map.domain.SetIndoorMapUseCase
import com.networkradar.feature.map.domain.SetIndoorPositionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(
    private val indoorDataSource: IndoorDataSource,
    private val setIndoorMapUseCase: SetIndoorMapUseCase,
    private val setIndoorPositionUseCase: SetIndoorPositionUseCase
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<MapState> = combine(
        indoorDataSource.activeMap,
        indoorDataSource.currentPosition,
        _error
    ) { activeMap, currentPosition, error ->
        MapState(
            activeMap = activeMap,
            currentPosition = currentPosition,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapState()
    )

    private val _events = Channel<MapEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.LoadMap -> {
                viewModelScope.launch {
                    setIndoorMapUseCase(action.map)
                }
            }
            is MapAction.SelectPosition -> {
                viewModelScope.launch {
                    val result = setIndoorPositionUseCase(action.x, action.y)
                    if (result is Result.Error) {
                        _error.value = result.error.toString()
                    } else {
                        _events.send(MapEvent.PositionSelected)
                    }
                }
            }
            MapAction.ClearError -> {
                _error.value = null
            }
        }
    }
}
