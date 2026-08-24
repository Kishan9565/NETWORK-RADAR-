package com.networkradar.core.domain.measurement

import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.StateFlow

interface ScanManager {
    val activeSession: StateFlow<ScanSession?>
    suspend fun startScan(name: String, mapId: String? = null): Result<ScanSession, DataError.Local>
    suspend fun stopScan(): Result<Unit, DataError.Local>
    suspend fun recordMeasurement(point: NetworkMeasurementPoint): Result<Unit, DataError.Local>
}
