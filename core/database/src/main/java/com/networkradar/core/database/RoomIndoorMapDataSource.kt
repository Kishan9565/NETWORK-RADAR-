package com.networkradar.core.database

import com.networkradar.core.database.dao.IndoorMapDao
import com.networkradar.core.database.mappers.toDomain
import com.networkradar.core.database.mappers.toEntity
import com.networkradar.core.domain.indoor.IndoorMap
import com.networkradar.core.domain.indoor.IndoorMapLocalDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomIndoorMapDataSource(
    private val indoorMapDao: IndoorMapDao
) : IndoorMapLocalDataSource {

    override suspend fun saveMap(map: IndoorMap): Result<Unit, DataError.Local> {
        return try {
            indoorMapDao.insertMap(map.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getMapById(id: String): Result<IndoorMap?, DataError.Local> {
        return try {
            val entity = indoorMapDao.getMapById(id)
            Result.Success(entity?.toDomain())
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override fun getAllMaps(): Flow<List<IndoorMap>> {
        return indoorMapDao.getAllMaps().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
