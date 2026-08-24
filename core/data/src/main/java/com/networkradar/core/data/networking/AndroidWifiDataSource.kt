package com.networkradar.core.data.networking

import android.content.Context
import android.net.wifi.WifiManager
import com.networkradar.core.domain.measurement.WifiDataSource
import com.networkradar.core.domain.measurement.WifiMeasurement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AndroidWifiDataSource(
    private val context: Context
) : WifiDataSource {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override fun getWifiMeasurement(): Flow<WifiMeasurement?> = flow {
        while (true) {
            val info = try {
                wifiManager.connectionInfo
            } catch (e: SecurityException) {
                null
            }
            
            if (info != null && info.networkId != -1) {
                val measurement = WifiMeasurement(
                    rssi = info.rssi.takeIf { it != -127 },
                    ssid = if (info.ssid != WifiManager.UNKNOWN_SSID) info.ssid.removeSurrounding("\"") else null,
                    frequency = info.frequency.takeIf { it != -1 },
                    linkSpeed = info.linkSpeed.takeIf { it != -1 },
                    timestamp = System.currentTimeMillis()
                )
                emit(measurement)
            } else {
                emit(null)
            }
            delay(2000)
        }
    }
}
