package com.networkradar.core.data.measurement

import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.measurement.ScanManager
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class RealScanManager(
    private val sessionDataSource: ScanSessionLocalDataSource,
    private val measurementDataSource: NetworkMeasurementPointLocalDataSource
) : ScanManager {

    private val _activeSession = MutableStateFlow<ScanSession?>(null)
    override val activeSession: StateFlow<ScanSession?> = _activeSession.asStateFlow()

    override suspend fun startScan(name: String, mapId: String?): Result<ScanSession, DataError.Local> {
        val session = ScanSession(
            id = UUID.randomUUID().toString(),
            mapId = mapId,
            name = name,
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            measurementCount = 0
        )
        
        return when (val result = sessionDataSource.startSession(session)) {
            is Result.Success -> {
                _activeSession.value = session
                Result.Success(session)
            }
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun stopScan(): Result<Unit, DataError.Local> {
        val current = _activeSession.value ?: return Result.Success(Unit)
        
        return when (val result = sessionDataSource.endSession(current.id, System.currentTimeMillis())) {
            is Result.Success -> {
                _activeSession.value = null
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun recordMeasurement(point: NetworkMeasurementPoint): Result<Unit, DataError.Local> {
        val session = _activeSession.value ?: return Result.Error(DataError.Local.NOT_FOUND)
        
        val saveResult = measurementDataSource.saveMeasurementPoint(session.id, point)
        if (saveResult is Result.Error) return saveResult
        
        return sessionDataSource.incrementMeasurementCount(session.id)
    }
}
