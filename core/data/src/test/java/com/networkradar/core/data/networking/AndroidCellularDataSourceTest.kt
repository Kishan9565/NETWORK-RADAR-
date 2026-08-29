package com.networkradar.core.data.networking

import android.content.Context
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AndroidCellularDataSourceTest {

    private lateinit var dataSource: AndroidCellularDataSource
    private val context = mockk<Context>(relaxed = true)
    private val telephonyManager = mockk<TelephonyManager>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { context.getSystemService(Context.TELEPHONY_SERVICE) } returns telephonyManager
        dataSource = AndroidCellularDataSource(context)
    }

    @Test
    fun `getCellularMeasurement returns LTE data when available`() = runTest {
        val mockCellInfo = mockk<CellInfoLte>()
        val mockSignalStrength = mockk<CellSignalStrengthLte>()
        
        every { mockCellInfo.isRegistered } returns true
        every { mockCellInfo.cellSignalStrength } returns mockSignalStrength
        every { mockSignalStrength.rsrp } returns -90
        every { mockSignalStrength.rsrq } returns -10
        every { mockSignalStrength.rssnr } returns 15
        
        every { telephonyManager.allCellInfo } returns listOf(mockCellInfo)

        val measurement = dataSource.getCellularMeasurement().first()

        assertThat(measurement).isNotNull()
        assertThat(measurement?.networkType).isEqualTo("LTE")
        assertThat(measurement?.rsrp).isEqualTo(-90)
        assertThat(measurement?.rsrq).isEqualTo(-10)
        assertThat(measurement?.sinr).isEqualTo(15)
    }

    @Test
    fun `getCellularMeasurement returns 5G NR data when available`() = runTest {
        // This test assumes it's running on API 29+ or the code handles it gracefully
        val mockCellInfo = mockk<CellInfoNr>()
        val mockSignalStrength = mockk<CellSignalStrengthNr>()
        
        every { mockCellInfo.isRegistered } returns true
        every { mockCellInfo.cellSignalStrength } returns mockSignalStrength
        
        // Mocking values that are only available on Q+
        every { mockSignalStrength.ssRsrp } returns -85
        every { mockSignalStrength.ssRsrq } returns -12
        every { mockSignalStrength.ssSinr } returns 20
        every { mockSignalStrength.csiRsrp } returns -80
        
        every { telephonyManager.allCellInfo } returns listOf(mockCellInfo)

        val measurement = dataSource.getCellularMeasurement().first()

        assertThat(measurement).isNotNull()
        assertThat(measurement?.networkType).isEqualTo("5G NR")
        assertThat(measurement?.rsrp).isEqualTo(-85)
        assertThat(measurement?.rsrq).isEqualTo(-12)
        assertThat(measurement?.sinr).isEqualTo(20)
        assertThat(measurement?.rssi).isEqualTo(-80)
    }

    @Test
    fun `getCellularMeasurement returns null when no registered cell info`() = runTest {
        every { telephonyManager.allCellInfo } returns emptyList()

        val measurement = dataSource.getCellularMeasurement().first()

        assertThat(measurement).isNull()
    }
    
    @Test
    fun `getCellularMeasurement returns UNAVAILABLE values as null`() = runTest {
        val mockCellInfo = mockk<CellInfoLte>()
        val mockSignalStrength = mockk<CellSignalStrengthLte>()
        
        every { mockCellInfo.isRegistered } returns true
        every { mockCellInfo.cellSignalStrength } returns mockSignalStrength
        every { mockSignalStrength.rsrp } returns CellInfo.UNAVAILABLE
        every { mockSignalStrength.rsrq } returns CellInfo.UNAVAILABLE
        every { mockSignalStrength.rssnr } returns CellInfo.UNAVAILABLE
        
        every { telephonyManager.allCellInfo } returns listOf(mockCellInfo)

        val measurement = dataSource.getCellularMeasurement().first()

        assertThat(measurement).isNotNull()
        assertThat(measurement?.rsrp).isNull()
        assertThat(measurement?.rsrq).isNull()
        assertThat(measurement?.sinr).isNull()
    }
}
