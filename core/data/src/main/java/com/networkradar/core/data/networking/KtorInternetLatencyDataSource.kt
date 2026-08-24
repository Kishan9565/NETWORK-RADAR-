package com.networkradar.core.data.networking

import com.networkradar.core.domain.measurement.InternetLatencyDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import com.networkradar.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException

class KtorInternetLatencyDataSource(
    private val httpClient: HttpClient
) : InternetLatencyDataSource {

    override suspend fun measureLatency(endpoint: String): Result<Double, DataError.Network> {
        return try {
            val startNano = System.nanoTime()
            val response = httpClient.get(endpoint)
            val endNano = System.nanoTime()
            
            if (response.status.value in 200..299) {
                val latencyMs = (endNano - startNano) / 1_000_000.0
                Result.Success(latencyMs)
            } else {
                responseToResult<Unit>(response).map { 0.0 }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(DataError.Network.UNKNOWN)
        }
    }
}
