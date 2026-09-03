package com.networkradar.feature.heatmap.domain

import com.networkradar.core.domain.measurement.NetworkMeasurementPoint

enum class HeatmapMetric {
    WIFI_RSSI,
    CELLULAR_RSRP,
    CELLULAR_RSRQ,
    CELLULAR_SINR;

    fun extract(point: NetworkMeasurementPoint): Double? {
        return when (this) {
            WIFI_RSSI -> point.wifi?.rssi?.toDouble()?.takeIf { it < 0 && it > -150 }
            CELLULAR_RSRP -> point.cellular?.rsrp?.toDouble()?.takeIf { it < 0 && it > -200 }
            CELLULAR_RSRQ -> point.cellular?.rsrq?.toDouble()?.takeIf { it < 0 }
            CELLULAR_SINR -> point.cellular?.sinr?.toDouble()
        }
    }

    fun isHigherBetter(): Boolean {
        return true // All current passive metrics are higher-is-better
    }
}
