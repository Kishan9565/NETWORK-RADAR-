package com.networkradar.core.data.indoor

import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryIndoorDataSource : IndoorDataSource {
    private val _activeMap = MutableStateFlow<IndoorMap?>(null)
    override val activeMap: StateFlow<IndoorMap?> = _activeMap.asStateFlow()

    private val _currentPosition = MutableStateFlow<IndoorPosition?>(null)
    override val currentPosition: StateFlow<IndoorPosition?> = _currentPosition.asStateFlow()

    override fun setActiveMap(map: IndoorMap?) {
        _activeMap.value = map
    }

    override fun setCurrentPosition(position: IndoorPosition?) {
        _currentPosition.value = position
    }
}
