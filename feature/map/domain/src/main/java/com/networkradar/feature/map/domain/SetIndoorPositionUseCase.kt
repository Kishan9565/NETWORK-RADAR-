package com.networkradar.feature.map.domain

import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.indoor.IndoorPositionValidator
import com.networkradar.core.domain.util.Result
import com.networkradar.core.domain.util.Error

class SetIndoorPositionUseCase(
    private val indoorDataSource: IndoorDataSource
) {
    operator fun invoke(x: Float, y: Float): Result<Unit, MapError> {
        val activeMap = indoorDataSource.activeMap.value ?: return Result.Error(MapError.NoActiveMap)
        
        val newPosition = IndoorPosition(
            mapId = activeMap.id,
            x = x,
            y = y
        )
        
        return if (IndoorPositionValidator.validate(newPosition, activeMap)) {
            indoorDataSource.setCurrentPosition(newPosition)
            Result.Success(Unit)
        } else {
            Result.Error(MapError.InvalidPosition)
        }
    }
}

sealed interface MapError : Error {
    data object NoActiveMap : MapError
    data object InvalidPosition : MapError
}
