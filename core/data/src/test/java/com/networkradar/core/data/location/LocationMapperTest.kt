package com.networkradar.core.data.location

import android.location.Location
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class LocationMapperTest {

    @Test
    fun `toDomain should map Android Location to Domain Location correctly`() {
        val androidLocation = mockk<Location>()
        every { androidLocation.latitude } returns 52.5200
        every { androidLocation.longitude } returns 13.4050
        every { androidLocation.accuracy } returns 5.0f
        every { androidLocation.time } returns 1620000000000L

        val domainLocation = androidLocation.toDomain()

        assertThat(domainLocation.lat).isEqualTo(52.5200)
        assertThat(domainLocation.long).isEqualTo(13.4050)
        assertThat(domainLocation.accuracy).isEqualTo(5.0f)
        assertThat(domainLocation.timestamp).isEqualTo(1620000000000L)
    }
}
