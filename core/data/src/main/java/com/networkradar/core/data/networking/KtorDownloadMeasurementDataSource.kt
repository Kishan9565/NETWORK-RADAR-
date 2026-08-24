package com.networkradar.core.data.networking

import com.networkradar.core.domain.measurement.DownloadMeasurementDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorDownloadMeasurementDataSource(
    private val httpClient: HttpClient
) : DownloadMeasurementDataSource {

    override fun download(url: String): Flow<Result<Double, DataError.Network>> = flow {
        try {
            httpClient.prepareGet(url).execute { response ->
                val channel = response.bodyAsChannel()
                var totalBytes = 0L
                val startNano = System.nanoTime()
                
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    val bytesRead = packet.remaining
                    totalBytes += bytesRead
                    packet.close()
                    
                    val currentNano = System.nanoTime()
                    val elapsedTimeSeconds = (currentNano - startNano) / 1_000_000_000.0
                    
                    if (elapsedTimeSeconds > 0) {
                        val throughputMbps = (totalBytes * 8.0) / (elapsedTimeSeconds * 1_000_000.0)
                        emit(Result.Success(throughputMbps))
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(DataError.Network.UNKNOWN))
        }
    }
    
    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
