package com.networkradar.feature.heatmap.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class HeatmapEngineTest {

    private val config = HeatmapEngine.HeatmapConfig(
        cellSize = 1.0f,
        power = 2.0,
        maxRadius = 10.0,
        minPoints = 1
    )

    @Test
    fun `generateGrid with empty points returns empty list`() {
        val result = HeatmapEngine.generateGrid(10f, 10f, emptyList(), config)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateIdw at exact point location returns exact value`() {
        val points = listOf(
            HeatmapEngine.WeightedPoint(5f, 5f, 100.0)
        )
        val result = HeatmapEngine.generateGrid(10f, 10f, points, config)
        
        // Find the cell exactly at 5,5
        val cell = result.find { it.x == 5f && it.y == 5f }
        assertNotNull(cell)
        assertEquals(100.0, cell?.value)
        assertEquals(1.0f, cell?.density)
    }

    @Test
    fun `calculateIdw interpolates between two points`() {
        val points = listOf(
            HeatmapEngine.WeightedPoint(0f, 0f, 0.0),
            HeatmapEngine.WeightedPoint(10f, 0f, 100.0)
        )
        val result = HeatmapEngine.generateGrid(10f, 0f, points, config)
        
        // Point at 5,0 should be exactly in the middle (50.0) for power=2 IDW
        val midCell = result.find { it.x == 5f && it.y == 0f }
        assertNotNull(midCell)
        assertTrue(abs(midCell!!.value!! - 50.0) < 0.001)
    }

    @Test
    fun `maxRadius filters out far points`() {
        val points = listOf(
            HeatmapEngine.WeightedPoint(0f, 0f, 100.0)
        )
        val shortRadiusConfig = config.copy(maxRadius = 2.0)
        val result = HeatmapEngine.generateGrid(10f, 10f, points, shortRadiusConfig)
        
        // Cell at 5,5 is distance ~7.07, which is > 2.0
        val farCell = result.find { it.x == 5f && it.y == 5f }
        assertNotNull(farCell)
        assertNull(farCell?.value)
        assertEquals(0f, farCell?.density)
    }

    @Test
    fun `minPoints returns null value if not enough neighbors`() {
        val points = listOf(
            HeatmapEngine.WeightedPoint(0f, 0f, 100.0)
        )
        val strictConfig = config.copy(minPoints = 2)
        val result = HeatmapEngine.generateGrid(2f, 2f, points, strictConfig)
        
        val cell = result.find { it.x == 1f && it.y == 1f }
        assertNull(cell?.value)
        assertTrue(cell!!.density < 1.0f)
    }

    @Test
    fun `grid boundaries are respected`() {
        val result = HeatmapEngine.generateGrid(5f, 5f, listOf(HeatmapEngine.WeightedPoint(0f, 0f, 10.0)), config)
        
        result.forEach { cell ->
            assertTrue(cell.x in 0f..5f)
            assertTrue(cell.y in 0f..5f)
        }
    }
}
