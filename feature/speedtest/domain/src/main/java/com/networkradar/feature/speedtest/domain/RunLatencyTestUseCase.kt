package com.networkradar.feature.speedtest.domain

import com.networkradar.core.domain.measurement.InternetLatencyDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result

class RunLatencyTestUseCase(
    private val latencyDataSource: InternetLatencyDataSource
) {
    suspend operator fun invoke(endpoint: String): Result<Double, DataError.Network> {
        return latencyDataSource.measureLatency(endpoint)
    }
}
