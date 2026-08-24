package com.networkradar.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.networkradar.core.database.entity.IndoorMapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IndoorMapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMap(map: IndoorMapEntity)

    @Query("SELECT * FROM indoor_maps WHERE id = :id")
    suspend fun getMapById(id: String): IndoorMapEntity?

    @Query("SELECT * FROM indoor_maps ORDER BY createdAt DESC")
    fun getAllMaps(): Flow<List<IndoorMapEntity>>
}
