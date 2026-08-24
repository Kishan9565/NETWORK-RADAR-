package com.networkradar.feature.heatmap.domain

data class HeatmapCell(
    val x: Float,
    val y: Float,
    val value: Double?,
    val density: Float
)
