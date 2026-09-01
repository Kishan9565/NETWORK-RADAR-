package com.networkradar.core.database.mappers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.location.Location
import com.networkradar.core.domain.measurement.CellularMeasurement
import com.networkradar.core.domain.measurement.InternetMeasurement
import com.networkradar.core.domain.measurement.NetworkMeasurementPoint
import com.networkradar.core.domain.measurement.ScanSession
import com.networkradar.core.domain.measurement.WifiMeasurement
import org.junit.jupiter.api.Test

class MappersTest {

    @Test
    fun `ScanSession mapping works`() {
        val domain = ScanSession("id", true, "name", 123L, 456L, 10)
        val entity = domain.toEntity()
        val mappedBack = entity.toDomain()

        assertThat(mappedBack).isEqualTo(domain)
    }

    @Test
    fun `NetworkMeasurementPoint mapping works`() {
        val domain = NetworkMeasurementPoint(
            id = 1L,
            location = Location(1.0, 2.0, 3f, 4L),
            indoorPosition = IndoorPosition("sessionId", 5f, 6f, 12L),
            wifi = WifiMeasurement(-50, "SSID", 2400, 100, 7L),
            cellular = CellularMeasurement("LTE", -100, -10, 15, -90, 8L),
            internet = InternetMeasurement(20.0, 100.0, 50.0, 9L),
            timestamp = 10L
        )
        
        val entity = domain.toEntity("sessionId")
        assertThat(entity.sessionId).isEqualTo("sessionId")
        assertThat(entity.id).isEqualTo(1L)
        
        val mappedBack = entity.toDomain()
        assertThat(mappedBack).isEqualTo(domain)
    }
    
    @Test
    fun `NetworkMeasurementPoint nullable mapping works`() {
        val domain = NetworkMeasurementPoint(
            id = 0L,
            location = null,
            indoorPosition = null,
            wifi = null,
            cellular = null,
            internet = null,
            timestamp = 10L
        )
        
        val entity = domain.toEntity("sessionId")
        val mappedBack = entity.toDomain()
        assertThat(mappedBack).isEqualTo(domain)
    }
}
