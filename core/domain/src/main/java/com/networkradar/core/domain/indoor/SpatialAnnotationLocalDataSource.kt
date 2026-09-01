package com.networkradar.core.domain.indoor

import kotlinx.coroutines.flow.Flow

interface SpatialAnnotationLocalDataSource {
    suspend fun saveAnnotation(annotation: SpatialAnnotation)
    fun getAnnotationsForSession(sessionId: String): Flow<List<SpatialAnnotation>>
}
