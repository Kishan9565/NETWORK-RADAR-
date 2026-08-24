package com.networkradar.feature.heatmap.domain

import com.networkradar.core.domain.measurement.NetworkMeasurementPoint

enum class HeatmapMetric {
    DOWNLOAD,
    UPLOAD,
    LATENCY,
    WIFI_RSSI,
    CELLULAR_RSRP,
    CELLULAR_RSRQ,
    CELLULAR_SINR;

    fun extract(point: NetworkMeasurementPoint): Double? {
        return when (this) {
            DOWNLOAD -> point.internet?.downloadMbps?.takeIf { it > 0 }
            UPLOAD -> point.internet?.uploadMbps?.takeIf { it > 0 }
            LATENCY -> point.internet?.latencyMs?.takeIf { it > 0 }
            WIFI_RSSI -> point.wifi?.rssi?.toDouble()?.takeIf { it < 0 && it > -150 }
            CELLULAR_RSRP -> point.cellular?.rsrp?.toDouble()?.takeIf { it < 0 && it > -200 }
            CELLULAR_RSRQ -> point.cellular?.rsrq?.toDouble()?.takeIf { it < 0 }
            CELLULAR_SINR -> point.cellular?.sinr?.toDouble()
        }
    }

    fun isHigherBetter(): Boolean {
        return when (this) {
            DOWNLOAD, UPLOAD, WIFI_RSSI, CELLULAR_RSRP, CELLULAR_RSRQ, CELLULAR_SINR -> true
            LATENCY -> false
        }
    }
}
