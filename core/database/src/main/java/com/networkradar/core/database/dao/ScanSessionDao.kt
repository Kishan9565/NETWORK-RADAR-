package com.networkradar.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.networkradar.core.database.entity.ScanSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity)

    @Update
    suspend fun updateSession(session: ScanSessionEntity)

    @Query("SELECT * FROM scan_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): ScanSessionEntity?

    @Query("SELECT * FROM scan_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<ScanSessionEntity>>

    @Query("UPDATE scan_sessions SET measurementCount = measurementCount + 1 WHERE id = :sessionId")
    suspend fun incrementMeasurementCount(sessionId: String)

    @Query("DELETE FROM scan_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
}
