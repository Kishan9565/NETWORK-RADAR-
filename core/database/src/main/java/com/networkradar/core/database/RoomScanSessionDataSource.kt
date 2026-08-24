package com.networkradar.core.database

import com.networkradar.core.database.dao.ScanSessionDao
import com.networkradar.core.database.mappers.toDomain
import com.networkradar.core.database.mappers.toEntity
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.measurement.ScanSessionLocalDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScanSessionDataSource(
    private val scanSessionDao: ScanSessionDao
) : ScanSessionLocalDataSource {

    override suspend fun startSession(session: ScanSession): Result<Unit, DataError.Local> {
        return try {
            scanSessionDao.insertSession(session.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun endSession(id: String, endedAt: Long): Result<Unit, DataError.Local> {
        return try {
            val session = scanSessionDao.getSessionById(id)
            if (session != null) {
                scanSessionDao.updateSession(session.copy(endedAt = endedAt))
                Result.Success(Unit)
            } else {
                Result.Error(DataError.Local.NOT_FOUND)
            }
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getSessionById(id: String): Result<ScanSession?, DataError.Local> {
        return try {
            val entity = scanSessionDao.getSessionById(id)
            Result.Success(entity?.toDomain())
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override fun getAllSessions(): Flow<List<ScanSession>> {
        return scanSessionDao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun incrementMeasurementCount(sessionId: String): Result<Unit, DataError.Local> {
        return try {
            scanSessionDao.incrementMeasurementCount(sessionId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun deleteSession(id: String): Result<Unit, DataError.Local> {
        return try {
            scanSessionDao.deleteSessionById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
