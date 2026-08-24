package com.networkradar.core.data.networking

import android.content.Context
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.networkradar.core.domain.measurement.CellularDataSource
import com.networkradar.core.domain.measurement.CellularMeasurement
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidCellularDataSource(
    private val context: Context
) : CellularDataSource {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    override fun getCellularMeasurement(): Flow<CellularMeasurement?> = callbackFlow {
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onSignalStrengthsChanged(signalStrength: android.telephony.SignalStrength?) {
                trySend(getCurrentCellularMeasurement())
            }
        }

        telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        trySend(getCurrentCellularMeasurement())

        awaitClose {
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
        }
    }
        .distinctUntilChanged()

    private fun getCurrentCellularMeasurement(): CellularMeasurement? {
        val allCellInfo = try {
            telephonyManager.allCellInfo
        } catch (e: SecurityException) {
            null
        }

        val lteInfo = allCellInfo?.filterIsInstance<CellInfoLte>()?.firstOrNull { it.isRegistered }
        
        return if (lteInfo != null) {
            val cellSignal = lteInfo.cellSignalStrength
            CellularMeasurement(
                networkType = "LTE",
                rsrp = cellSignal.rsrp.takeIf { it != CellInfo.UNAVAILABLE },
                rsrq = cellSignal.rsrq.takeIf { it != CellInfo.UNAVAILABLE },
                sinr = cellSignal.rssnr.takeIf { it != CellInfo.UNAVAILABLE },
                rssi = null,
                timestamp = System.currentTimeMillis()
            )
        } else {
            null
        }
    }
}
