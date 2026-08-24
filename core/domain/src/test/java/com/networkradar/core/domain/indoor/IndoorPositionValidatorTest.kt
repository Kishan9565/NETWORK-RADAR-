package com.networkradar.core.domain.indoor

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class IndoorPositionValidatorTest {

    private val map = IndoorMap(
        id = "test_map",
        name = "Test Map",
        width = 100f,
        height = 100f,
        createdAt = 0L
    )

    @Test
    fun `validate returns true for position inside boundaries`() {
        val position = IndoorPosition("test_map", 50f, 50f)
        assertThat(IndoorPositionValidator.validate(position, map)).isTrue()
    }

    @Test
    fun `validate returns true for position exactly on top-left boundary`() {
        val position = IndoorPosition("test_map", 0f, 0f)
        assertThat(IndoorPositionValidator.validate(position, map)).isTrue()
    }

    @Test
    fun `validate returns true for position exactly on bottom-right boundary`() {
        val position = IndoorPosition("test_map", 100f, 100f)
        assertThat(IndoorPositionValidator.validate(position, map)).isTrue()
    }

    @Test
    fun `validate returns false for position outside X boundary (too small)`() {
        val position = IndoorPosition("test_map", -0.1f, 50f)
        assertThat(IndoorPositionValidator.validate(position, map)).isFalse()
    }

    @Test
    fun `validate returns false for position outside X boundary (too large)`() {
        val position = IndoorPosition("test_map", 100.1f, 50f)
        assertThat(IndoorPositionValidator.validate(position, map)).isFalse()
    }

    @Test
    fun `validate returns false for position outside Y boundary (too small)`() {
        val position = IndoorPosition("test_map", 50f, -0.1f)
        assertThat(IndoorPositionValidator.validate(position, map)).isFalse()
    }

    @Test
    fun `validate returns false for position outside Y boundary (too large)`() {
        val position = IndoorPosition("test_map", 50f, 100.1f)
        assertThat(IndoorPositionValidator.validate(position, map)).isFalse()
    }

    @Test
    fun `validate returns false for position with mismatched map ID`() {
        val position = IndoorPosition("wrong_map", 50f, 50f)
        assertThat(IndoorPositionValidator.validate(position, map)).isFalse()
    }
}
