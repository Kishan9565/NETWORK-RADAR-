package com.networkradar.core.domain.measurement

import com.networkradar.core.domain.location.Location
import com.networkradar.core.domain.indoor.IndoorPosition

data class ScanIntelligenceSummary(
    val totalPoints: Int,
    val validPoints: Int,
    val statistics: Map<NetworkMetric, MetricStatistics>,
    val stability: Map<NetworkMetric, StabilityInfo>,
    val overallConfidence: Float,
    val dataSufficiency: DataSufficiency,
    val bestSpot: RankedSpot?,
    val worstSpot: RankedSpot?
)

enum class NetworkMetric {
    DOWNLOAD, UPLOAD, LATENCY, WIFI_RSSI, CELLULAR_RSRP
}

data class MetricStatistics(
    val count: Int,
    val min: Double,
    val max: Double,
    val average: Double,
    val median: Double? = null
)

data class StabilityInfo(
    val level: StabilityLevel,
    val variationCoefficient: Double
)

enum class StabilityLevel {
    STABLE, MODERATELY_STABLE, UNSTABLE, INSUFFICIENT_DATA
}

enum class DataSufficiency {
    NO_DATA, INSUFFICIENT, VALID, HIGH_CONFIDENCE
}

data class RankedSpot(
    val pointId: Long?, // If available from persistence
    val timestamp: Long,
    val location: Location?,
    val indoorPosition: IndoorPosition?,
    val score: Double,
    val contributingMetrics: List<NetworkMetric>
)
