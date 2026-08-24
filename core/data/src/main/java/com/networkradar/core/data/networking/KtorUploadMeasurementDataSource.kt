package com.networkradar.core.data.networking

import com.networkradar.core.domain.measurement.UploadMeasurementDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorUploadMeasurementDataSource(
    private val httpClient: HttpClient
) : UploadMeasurementDataSource {

    override fun upload(url: String): Flow<Result<Double, DataError.Network>> = flow {
        // Real implementation requires a specific endpoint that accepts POST/PUT body streams.
        // For Phase 3 foundation, we establish the contract and explicit unavailability
        // until a legitimate measurement endpoint is configured in later phases.
        emit(Result.Error(DataError.Network.SERVICE_UNAVAILABLE))
    }
}
