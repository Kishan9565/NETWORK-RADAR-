package com.networkradar.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.networkradar.core.database.entity.SpatialAnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpatialAnnotationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: SpatialAnnotationEntity)

    @Query("SELECT * FROM spatial_annotations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getAnnotationsForSession(sessionId: String): Flow<List<SpatialAnnotationEntity>>
}
