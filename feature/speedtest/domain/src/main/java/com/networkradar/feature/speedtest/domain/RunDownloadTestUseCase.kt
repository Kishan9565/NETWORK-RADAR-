package com.networkradar.feature.speedtest.domain

import com.networkradar.core.domain.measurement.DownloadMeasurementDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

class RunDownloadTestUseCase(
    private val downloadDataSource: DownloadMeasurementDataSource
) {
    operator fun invoke(url: String): Flow<Result<Double, DataError.Network>> {
        return downloadDataSource.download(url)
    }
}
