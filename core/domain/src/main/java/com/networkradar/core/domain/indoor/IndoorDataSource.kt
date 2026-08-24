package com.networkradar.core.domain.indoor

import kotlinx.coroutines.flow.StateFlow

interface IndoorDataSource {
    val activeMap: StateFlow<IndoorMap?>
    val currentPosition: StateFlow<IndoorPosition?>
    
    fun setActiveMap(map: IndoorMap?)
    fun setCurrentPosition(position: IndoorPosition?)
}
