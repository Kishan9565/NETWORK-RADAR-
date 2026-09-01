package com.networkradar.core.database

import com.networkradar.core.database.dao.SpatialAnnotationDao
import com.networkradar.core.database.mappers.toDomain
import com.networkradar.core.database.mappers.toEntity
import com.networkradar.core.domain.indoor.SpatialAnnotation
import com.networkradar.core.domain.indoor.SpatialAnnotationLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSpatialAnnotationDataSource(
    private val annotationDao: SpatialAnnotationDao
) : SpatialAnnotationLocalDataSource {
    override suspend fun saveAnnotation(annotation: SpatialAnnotation) {
        annotationDao.insertAnnotation(annotation.toEntity())
    }

    override fun getAnnotationsForSession(sessionId: String): Flow<List<SpatialAnnotation>> {
        return annotationDao.getAnnotationsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
