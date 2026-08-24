package com.networkradar.core.domain.measurement

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntelligenceEngineTest {

    @Test
    fun `analyze empty list returns empty summary`() {
        val result = IntelligenceEngine.analyze(emptyList())
        assertEquals(0, result.totalPoints)
        assertEquals(DataSufficiency.NO_DATA, result.dataSufficiency)
        assertNull(result.bestSpot)
    }

    @Test
    fun `analyze single valid point returns basic stats`() {
        val point = createPoint(rssi = -60, download = 50.0)
        val result = IntelligenceEngine.analyze(listOf(point))

        assertEquals(1, result.totalPoints)
        assertEquals(1, result.validPoints)
        assertEquals(50.0, result.statistics[NetworkMetric.DOWNLOAD]?.average)
        assertEquals(-60.0, result.statistics[NetworkMetric.WIFI_RSSI]?.average)
        assertEquals(StabilityLevel.INSUFFICIENT_DATA, result.stability[NetworkMetric.DOWNLOAD]?.level)
    }

    @Test
    fun `analyze multiple points calculates correct averages`() {
        val points = listOf(
            createPoint(download = 10.0),
            createPoint(download = 20.0),
            createPoint(download = 30.0)
        )
        val result = IntelligenceEngine.analyze(points)

        assertEquals(20.0, result.statistics[NetworkMetric.DOWNLOAD]?.average)
        assertEquals(10.0, result.statistics[NetworkMetric.DOWNLOAD]?.min)
        assertEquals(30.0, result.statistics[NetworkMetric.DOWNLOAD]?.max)
        assertEquals(3, result.statistics[NetworkMetric.DOWNLOAD]?.count)
    }

    @Test
    fun `median is calculated correctly for even and odd counts`() {
        // Odd
        val oddPoints = listOf(10.0, 20.0, 30.0).map { createPoint(download = it) }
        assertEquals(20.0, IntelligenceEngine.analyze(oddPoints).statistics[NetworkMetric.DOWNLOAD]?.median)

        // Even
        val evenPoints = listOf(10.0, 20.0, 30.0, 40.0).map { createPoint(download = it) }
        assertEquals(25.0, IntelligenceEngine.analyze(evenPoints).statistics[NetworkMetric.DOWNLOAD]?.median)
    }

    @Test
    fun `stability is calculated correctly for varied values`() {
        // Highly unstable
        val unstablePoints = listOf(
            createPoint(download = 10.0),
            createPoint(download = 100.0)
        )
        val unstableResult = IntelligenceEngine.analyze(unstablePoints)
        assertEquals(StabilityLevel.UNSTABLE, unstableResult.stability[NetworkMetric.DOWNLOAD]?.level)

        // Stable
        val stablePoints = listOf(
            createPoint(download = 50.0),
            createPoint(download = 51.0),
            createPoint(download = 49.0)
        )
        val stableResult = IntelligenceEngine.analyze(stablePoints)
        assertEquals(StabilityLevel.STABLE, stableResult.stability[NetworkMetric.DOWNLOAD]?.level)
    }

    @Test
    fun `confidence increases with valid data points`() {
        val lowData = List(2) { createPoint(download = 50.0) }
        val highData = List(15) { createPoint(download = 50.0) }

        val lowConfidence = IntelligenceEngine.analyze(lowData).overallConfidence
        val highConfidence = IntelligenceEngine.analyze(highData).overallConfidence

        assertTrue(highConfidence > lowConfidence)
    }

    @Test
    fun `best spot selection uses deterministic tie-breaking`() {
        val point1 = createPoint(download = 100.0, timestamp = 1000L)
        val point2 = createPoint(download = 100.0, timestamp = 2000L)
        
        val result = IntelligenceEngine.analyze(listOf(point1, point2))
        
        // Tie-breaker: earliest timestamp
        assertEquals(1000L, result.bestSpot?.timestamp)
    }

    @Test
    fun `invalid values are filtered out`() {
        val points = listOf(
            createPoint(download = -10.0), // Invalid negative throughput
            createPoint(download = 50.0),
            createPoint(download = Double.NaN),
            createPoint(download = Double.POSITIVE_INFINITY)
        )
        val result = IntelligenceEngine.analyze(points)
        assertEquals(1, result.statistics[NetworkMetric.DOWNLOAD]?.count)
        assertEquals(50.0, result.statistics[NetworkMetric.DOWNLOAD]?.average)
    }

    @Test
    fun `negative RF values are handled correctly`() {
        val points = listOf(
            createPoint(rssi = -50),
            createPoint(rssi = -80)
        )
        val result = IntelligenceEngine.analyze(points)
        assertEquals(-65.0, result.statistics[NetworkMetric.WIFI_RSSI]?.average)
        assertNotNull(result.bestSpot)
        // Score should be positive for a valid signal
        assertTrue(result.bestSpot!!.score > 0.0)
    }

    private fun createPoint(
        rssi: Int? = null,
        rsrp: Int? = null,
        download: Double? = null,
        timestamp: Long = System.currentTimeMillis()
    ) = NetworkMeasurementPoint(
        id = null,
        location = null,
        indoorPosition = null,
        wifi = rssi?.let { WifiMeasurement(it, "SSID", 2412, 100, timestamp) },
        cellular = rsrp?.let { CellularMeasurement("LTE", it, -10, 10, -80, timestamp) },
        internet = download?.let { InternetMeasurement(null, it, null, timestamp) },
        timestamp = timestamp
    )
}
