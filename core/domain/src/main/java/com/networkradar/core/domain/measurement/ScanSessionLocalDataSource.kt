package com.networkradar.core.domain.measurement

import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface ScanSessionLocalDataSource {
    suspend fun startSession(session: ScanSession): Result<Unit, DataError.Local>
    suspend fun endSession(id: String, endedAt: Long): Result<Unit, DataError.Local>
    suspend fun getSessionById(id: String): Result<ScanSession?, DataError.Local>
    fun getAllSessions(): Flow<List<ScanSession>>
    suspend fun incrementMeasurementCount(sessionId: String): Result<Unit, DataError.Local>
    suspend fun deleteSession(id: String): Result<Unit, DataError.Local>
}
