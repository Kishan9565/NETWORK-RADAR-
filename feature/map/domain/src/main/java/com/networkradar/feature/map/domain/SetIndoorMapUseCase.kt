package com.networkradar.feature.map.domain

import com.networkradar.core.domain.indoor.IndoorDataSource
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorMapLocalDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result

class SetIndoorMapUseCase(
    private val indoorDataSource: IndoorDataSource,
    private val indoorMapLocalDataSource: IndoorMapLocalDataSource
) {
    suspend operator fun invoke(map: IndoorMap?): Result<Unit, DataError.Local> {
        indoorDataSource.setActiveMap(map)
        return if (map != null) {
            indoorMapLocalDataSource.saveMap(map)
        } else {
            Result.Success(Unit)
        }
    }
}
