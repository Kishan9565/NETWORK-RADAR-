package com.networkradar.core.domain.indoor

import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface IndoorMapLocalDataSource {
    suspend fun saveMap(map: IndoorMap): Result<Unit, DataError.Local>
    suspend fun getMapById(id: String): Result<IndoorMap?, DataError.Local>
    fun getAllMaps(): Flow<List<IndoorMap>>
}
