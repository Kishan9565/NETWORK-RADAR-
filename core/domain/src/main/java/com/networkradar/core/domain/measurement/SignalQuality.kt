package com.networkradar.core.domain.measurement

enum class SignalQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNAVAILABLE
}

object SignalQualityStrategy {
    fun getWifiQuality(rssi: Int?): SignalQuality {
        return when {
            rssi == null -> SignalQuality.UNAVAILABLE
            rssi >= -50 -> SignalQuality.EXCELLENT
            rssi >= -60 -> SignalQuality.GOOD
            rssi >= -70 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    }

    fun getCellularQuality(rsrp: Int?): SignalQuality {
        return when {
            rsrp == null -> SignalQuality.UNAVAILABLE
            rsrp >= -80 -> SignalQuality.EXCELLENT
            rsrp >= -90 -> SignalQuality.GOOD
            rsrp >= -100 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    }

    fun getDownloadQuality(mbps: Double?): SignalQuality {
        return when {
            mbps == null -> SignalQuality.UNAVAILABLE
            mbps >= 50.0 -> SignalQuality.EXCELLENT
            mbps >= 20.0 -> SignalQuality.GOOD
            mbps >= 5.0 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    }

    fun getLatencyQuality(ms: Double?): SignalQuality {
        return when {
            ms == null -> SignalQuality.UNAVAILABLE
            ms <= 20.0 -> SignalQuality.EXCELLENT
            ms <= 50.0 -> SignalQuality.GOOD
            ms <= 100.0 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    }
}
