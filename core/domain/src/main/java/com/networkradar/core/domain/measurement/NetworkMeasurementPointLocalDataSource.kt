package com.networkradar.core.domain.measurement

import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface NetworkMeasurementPointLocalDataSource {
    suspend fun saveMeasurementPoint(sessionId: String, point: NetworkMeasurementPoint): Result<Unit, DataError.Local>
    fun getMeasurementsForSession(sessionId: String): Flow<List<NetworkMeasurementPoint>>
}
