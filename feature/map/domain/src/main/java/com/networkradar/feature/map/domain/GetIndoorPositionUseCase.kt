package com.networkradar.feature.map.domain

import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.indoor.IndoorPosition
import kotlinx.coroutines.flow.StateFlow

class GetIndoorPositionUseCase(
    private val indoorDataSource: IndoorDataSource
) {
    operator fun invoke(): StateFlow<IndoorPosition?> {
        return indoorDataSource.currentPosition
    }
}
