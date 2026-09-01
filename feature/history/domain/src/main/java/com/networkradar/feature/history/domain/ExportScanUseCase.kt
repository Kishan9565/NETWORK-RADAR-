package com.networkradar.feature.history.domain

import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.NetworkMeasurementPointLocalDataSource
import com.networkradar.core.domain.util.DataError
import com.networkradar.core.domain.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExportScanUseCase(
    private val pointDataSource: NetworkMeasurementPointLocalDataSource
) {
    suspend fun toCsv(sessionId: String): Result<String, DataError.Local> {
        return try {
            val points = pointDataSource.getMeasurementsForSession(sessionId).first()
            val csv = StringBuilder()
            csv.append("timestamp,latitude,longitude,accuracy,indoorX,indoorY,indoorTimestamp,downloadMbps,uploadMbps,latencyMs,wifiRssi,wifiSsid,cellType,cellRsrp\n")
            
            points.forEach { p ->
                csv.append("${p.timestamp},")
                csv.append("${p.location?.lat ?: ""},")
                csv.append("${p.location?.long ?: ""},")
                csv.append("${p.location?.accuracy ?: ""},")
                csv.append("${p.indoorPosition?.x ?: ""},")
                csv.append("${p.indoorPosition?.y ?: ""},")
                csv.append("${p.indoorPosition?.timestamp ?: ""},")
                csv.append("${p.internet?.downloadMbps ?: ""},")
                csv.append("${p.internet?.uploadMbps ?: ""},")
                csv.append("${p.internet?.latencyMs ?: ""},")
                csv.append("${p.wifi?.rssi ?: ""},")
                csv.append("${p.wifi?.ssid ?: ""},")
                csv.append("${p.cellular?.networkType ?: ""},")
                csv.append("${p.cellular?.rsrp ?: ""}\n")
            }
            Result.Success(csv.toString())
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    suspend fun toJson(sessionId: String): Result<String, DataError.Local> {
        return try {
            val points = pointDataSource.getMeasurementsForSession(sessionId).first()
            val exportData = ScanExportDto(
                sessionId = sessionId,
                measurements = points
            )
            val json = Json { prettyPrint = true }
            Result.Success(json.encodeToString(exportData))
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    @Serializable
    private data class ScanExportDto(
        val sessionId: String,
        val measurements: List<NetworkMeasurementPoint>
    )
}
