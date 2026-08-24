package com.networkradar.core.domain.measurement

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure Kotlin intelligence engine for network measurement analysis.
 * Operates on real persisted data without Android dependencies.
 */
object IntelligenceEngine {

    fun analyze(points: List<NetworkMeasurementPoint>): ScanIntelligenceSummary {
        val totalPoints = points.size
        if (totalPoints == 0) {
            return emptySummary()
        }

        val metricsData = extractMetrics(points)
        val stats = metricsData.mapValues { calculateStatistics(it.value) }
        val stability = metricsData.mapValues { calculateStability(it.value) }
        
        val validPointsCount = points.count { p ->
            p.wifi != null || p.cellular != null || p.internet != null
        }

        val dataSufficiency = calculateSufficiency(totalPoints, validPointsCount, stats)
        val confidence = calculateConfidence(totalPoints, stats, stability, dataSufficiency)

        val (best, worst) = findBestAndWorstSpots(points)

        return ScanIntelligenceSummary(
            totalPoints = totalPoints,
            validPoints = validPointsCount,
            statistics = stats,
            stability = stability,
            overallConfidence = confidence,
            dataSufficiency = dataSufficiency,
            bestSpot = best,
            worstSpot = worst
        )
    }

    private fun extractMetrics(points: List<NetworkMeasurementPoint>): Map<NetworkMetric, List<Double>> {
        val download = points.mapNotNull { it.internet?.downloadMbps }.filter { it > 0 && it.isFinite() }
        val upload = points.mapNotNull { it.internet?.uploadMbps }.filter { it > 0 && it.isFinite() }
        val latency = points.mapNotNull { it.internet?.latencyMs }.filter { it > 0 && it.isFinite() }
        val wifiRssi = points.mapNotNull { it.wifi?.rssi?.toDouble() }.filter { it < 0 && it > -150 }
        val cellRsrp = points.mapNotNull { it.cellular?.rsrp?.toDouble() }.filter { it < 0 && it > -200 }

        return buildMap {
            if (download.isNotEmpty()) put(NetworkMetric.DOWNLOAD, download)
            if (upload.isNotEmpty()) put(NetworkMetric.UPLOAD, upload)
            if (latency.isNotEmpty()) put(NetworkMetric.LATENCY, latency)
            if (wifiRssi.isNotEmpty()) put(NetworkMetric.WIFI_RSSI, wifiRssi)
            if (cellRsrp.isNotEmpty()) put(NetworkMetric.CELLULAR_RSRP, cellRsrp)
        }
    }

    private fun calculateStatistics(values: List<Double>): MetricStatistics {
        if (values.isEmpty()) return MetricStatistics(0, 0.0, 0.0, 0.0, null)
        
        val count = values.size
        val min = values.min()
        val max = values.max()
        val avg = values.average()
        val sorted = values.sorted()
        val median = if (count > 0) {
            if (count % 2 == 0) (sorted[count / 2] + sorted[count / 2 - 1]) / 2.0 else sorted[count / 2]
        } else null

        return MetricStatistics(
            count = count,
            min = if (min.isFinite()) min else 0.0,
            max = if (max.isFinite()) max else 0.0,
            average = if (avg.isFinite()) avg else 0.0,
            median = if (median != null && median.isFinite()) median else null
        )
    }

    private fun calculateStability(values: List<Double>): StabilityInfo {
        if (values.size < 2) return StabilityInfo(StabilityLevel.INSUFFICIENT_DATA, 0.0)

        val avg = values.average()
        if (avg == 0.0 || !avg.isFinite()) return StabilityInfo(StabilityLevel.UNSTABLE, 1.0)

        val variance = values.map { (it - avg).pow(2) }.average()
        val stdDev = sqrt(variance)
        val cv = stdDev / Math.abs(avg) // Coefficient of Variation

        if (!cv.isFinite()) return StabilityInfo(StabilityLevel.UNSTABLE, 1.0)

        val level = when {
            cv < 0.1 -> StabilityLevel.STABLE
            cv < 0.3 -> StabilityLevel.MODERATELY_STABLE
            else -> StabilityLevel.UNSTABLE
        }

        return StabilityInfo(level, cv)
    }

    private fun calculateSufficiency(total: Int, valid: Int, stats: Map<NetworkMetric, MetricStatistics>): DataSufficiency {
        if (total == 0) return DataSufficiency.NO_DATA
        if (valid < 3) return DataSufficiency.INSUFFICIENT
        
        val hasInternet = stats.containsKey(NetworkMetric.DOWNLOAD)
        return if (valid >= 10 && hasInternet) DataSufficiency.HIGH_CONFIDENCE else DataSufficiency.VALID
    }

    private fun calculateConfidence(
        total: Int,
        stats: Map<NetworkMetric, MetricStatistics>,
        stability: Map<NetworkMetric, StabilityInfo>,
        sufficiency: DataSufficiency
    ): Float {
        if (total == 0) return 0f
        
        var score = 0f
        
        // Base score on sufficiency
        score += when(sufficiency) {
            DataSufficiency.NO_DATA -> 0f
            DataSufficiency.INSUFFICIENT -> 0.2f
            DataSufficiency.VALID -> 0.5f
            DataSufficiency.HIGH_CONFIDENCE -> 0.7f
        }

        // Stability bonus
        if (stability.isNotEmpty()) {
            val stableCount = stability.values.count { it.level == StabilityLevel.STABLE }
            score += (stableCount.toFloat() / stability.size) * 0.3f
        }

        return score.coerceIn(0f, 1f)
    }

    private fun findBestAndWorstSpots(points: List<NetworkMeasurementPoint>): Pair<RankedSpot?, RankedSpot?> {
        if (points.isEmpty()) return null to null

        val scoredPoints = points.map { point ->
            val score = calculatePointScore(point)
            val metrics = mutableListOf<NetworkMetric>()
            if (point.internet?.downloadMbps?.let { it > 0 && it.isFinite() } == true) metrics.add(NetworkMetric.DOWNLOAD)
            if (point.internet?.latencyMs?.let { it > 0 && it.isFinite() } == true) metrics.add(NetworkMetric.LATENCY)
            if (point.wifi?.rssi != null) metrics.add(NetworkMetric.WIFI_RSSI)
            if (point.cellular?.rsrp != null) metrics.add(NetworkMetric.CELLULAR_RSRP)

            RankedSpot(
                pointId = point.id,
                timestamp = point.timestamp,
                location = point.location,
                indoorPosition = point.indoorPosition,
                score = score,
                contributingMetrics = metrics
            )
        }.filter { it.contributingMetrics.isNotEmpty() }

        if (scoredPoints.isEmpty()) return null to null

        // Deterministic sorting: highest score first, tie-break with earliest timestamp
        val sorted = scoredPoints.sortedWith(
            compareByDescending<RankedSpot> { it.score }
                .thenBy { it.timestamp }
        )
        
        return sorted.first() to sorted.last()
    }

    private fun calculatePointScore(point: NetworkMeasurementPoint): Double {
        var score = 0.0
        var totalWeight = 0.0

        // Download (Higher is better, Weight: 10)
        point.internet?.downloadMbps?.let {
            if (it > 0 && it.isFinite()) {
                score += (it.coerceIn(0.0, 100.0) / 100.0) * 10.0
                totalWeight += 10.0
            }
        }

        // Latency (Lower is better, Weight: 10)
        point.internet?.latencyMs?.let {
            if (it > 0 && it.isFinite()) {
                val latencyScore = (1.0 - (it.coerceIn(0.0, 500.0) / 500.0)) * 10.0
                score += latencyScore
                totalWeight += 10.0
            }
        }

        // Wifi RSSI (Higher is better, Weight: 5)
        point.wifi?.rssi?.let {
            val rssiScore = ((it.toDouble().coerceIn(-100.0, -30.0) + 100.0) / 70.0) * 5.0
            score += rssiScore
            totalWeight += 5.0
        }

        // Cellular RSRP (Higher is better, Weight: 5)
        point.cellular?.rsrp?.let {
            val rsrpScore = ((it.toDouble().coerceIn(-120.0, -50.0) + 120.0) / 70.0) * 5.0
            score += rsrpScore
            totalWeight += 5.0
        }

        return if (totalWeight > 0) score / totalWeight else 0.0
    }

    private fun emptySummary() = ScanIntelligenceSummary(
        totalPoints = 0,
        validPoints = 0,
        statistics = emptyMap(),
        stability = emptyMap(),
        overallConfidence = 0f,
        dataSufficiency = DataSufficiency.NO_DATA,
        bestSpot = null,
        worstSpot = null
    )
}
