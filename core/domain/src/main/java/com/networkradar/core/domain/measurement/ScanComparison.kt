package com.networkradar.core.domain.measurement

data class ScanComparison(
    val scanA: ScanSession,
    val scanB: ScanSession,
    val results: Map<NetworkMetric, MetricComparison>
)

data class MetricComparison(
    val valueA: Double,
    val valueB: Double,
    val difference: Double,
    val percentageChange: Double?,
    val trend: ComparisonTrend
)

enum class ComparisonTrend {
    IMPROVED, WORSENED, UNCHANGED, UNAVAILABLE
}
