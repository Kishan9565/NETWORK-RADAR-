package com.networkradar.core.domain.location

import kotlinx.coroutines.flow.Flow

interface LocationDataSource {
    fun getLocationUpdates(): Flow<LocationObservation>
}
