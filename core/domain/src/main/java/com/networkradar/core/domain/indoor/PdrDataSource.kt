package com.networkradar.core.domain.indoor

import kotlinx.coroutines.flow.Flow

interface PdrDataSource {
    val currentPosition: Flow<IndoorPosition>
    val isAvailable: Boolean

    fun startTracking(sessionId: String)
    fun stopTracking()
    fun resetOrigin()
}
