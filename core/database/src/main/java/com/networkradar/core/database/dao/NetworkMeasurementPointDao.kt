package com.networkradar.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.networkradar.core.database.entity.NetworkMeasurementPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkMeasurementPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurementPoint(point: NetworkMeasurementPointEntity)

    @Query("SELECT * FROM measurement_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMeasurementsForSession(sessionId: String): Flow<List<NetworkMeasurementPointEntity>>

    @Query("SELECT * FROM measurement_points WHERE id = :id")
    suspend fun getMeasurementById(id: Long): NetworkMeasurementPointEntity?

    @Query("DELETE FROM measurement_points WHERE sessionId = :sessionId")
    suspend fun deleteMeasurementsForSession(sessionId: String)
}
