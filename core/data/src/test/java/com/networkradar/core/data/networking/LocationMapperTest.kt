package com.networkradar.core.data.networking

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.networkradar.core.domain.location.Location
import org.junit.jupiter.api.Test

class LocationMapperTest {

    @Test
    fun `toDomain should map DTO correctly`() {
        val dto = LocationDto(
            lat = 50.0,
            long = 20.0,
            accuracy = 10.0f,
            timestamp = 123456789L
        )
        
        val domain = dto.toDomain()
        
        assertThat(domain.lat).isEqualTo(50.0)
        assertThat(domain.long).isEqualTo(20.0)
        assertThat(domain.accuracy).isEqualTo(10.0f)
        assertThat(domain.timestamp).isEqualTo(123456789L)
    }

    @Test
    fun `toDto should map Domain correctly`() {
        val domain = Location(
            lat = 50.0,
            long = 20.0,
            accuracy = 10.0f,
            timestamp = 123456789L
        )
        
        val dto = domain.toDto()
        
        assertThat(dto.lat).isEqualTo(50.0)
        assertThat(dto.long).isEqualTo(20.0)
        assertThat(dto.accuracy).isEqualTo(10.0f)
        assertThat(dto.timestamp).isEqualTo(123456789L)
    }
}
