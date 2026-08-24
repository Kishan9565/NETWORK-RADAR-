package com.networkradar.core.database

import com.networkradar.core.database.dao.NetworkMeasurementPointDao
import com.networkradar.core.database.mappers.toDomain
import com.networkradar.core.database.mappers.toEntity
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNetworkMeasurementPointDataSource(
    private val measurementPointDao: NetworkMeasurementPointDao
) : NetworkMeasurementPointLocalDataSource {

    override suspend fun saveMeasurementPoint(
        sessionId: String,
        point: NetworkMeasurementPoint
    ): Result<Unit, DataError.Local> {
        return try {
            measurementPointDao.insertMeasurementPoint(point.toEntity(sessionId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override fun getMeasurementsForSession(sessionId: String): Flow<List<NetworkMeasurementPoint>> {
        return measurementPointDao.getMeasurementsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
