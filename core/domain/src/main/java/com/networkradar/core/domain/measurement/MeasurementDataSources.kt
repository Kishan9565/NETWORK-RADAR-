package com.networkradar.core.domain.measurement

import com.networkradar.core.domain.networking.ConnectivityState
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface WifiDataSource {
    fun getWifiMeasurement(): Flow<WifiMeasurement?>
}

interface CellularDataSource {
    fun getCellularMeasurement(): Flow<CellularMeasurement?>
}

interface ConnectivityDataSource {
    fun getConnectivityState(): Flow<ConnectivityState>
}

interface InternetLatencyDataSource {
    suspend fun measureLatency(endpoint: String): Result<Double, DataError.Network>
}

interface DownloadMeasurementDataSource {
    fun download(url: String): Flow<Result<Double, DataError.Network>>
}

interface UploadMeasurementDataSource {
    fun upload(url: String): Flow<Result<Double, DataError.Network>>
}
