package com.networkradar.core.data.networking

import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
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
        } ?: return null

        val registeredCell = allCellInfo.firstOrNull { it.isRegistered } ?: return null
        
        return when (registeredCell) {
            is CellInfoLte -> {
                val cellSignal = registeredCell.cellSignalStrength
                CellularMeasurement(
                    networkType = "LTE",
                    rsrp = cellSignal.rsrp.takeIf { it != CellInfo.UNAVAILABLE },
                    rsrq = cellSignal.rsrq.takeIf { it != CellInfo.UNAVAILABLE },
                    sinr = cellSignal.rssnr.takeIf { it != CellInfo.UNAVAILABLE },
                    rssi = null,
                    timestamp = System.currentTimeMillis()
                )
            }
            is CellInfoNr -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val cellSignal = registeredCell.cellSignalStrength as? android.telephony.CellSignalStrengthNr
                    CellularMeasurement(
                        networkType = "5G NR",
                        rsrp = cellSignal?.ssRsrp?.takeIf { it != CellInfo.UNAVAILABLE },
                        rsrq = cellSignal?.ssRsrq?.takeIf { it != CellInfo.UNAVAILABLE },
                        sinr = cellSignal?.ssSinr?.takeIf { it != CellInfo.UNAVAILABLE },
                        rssi = cellSignal?.csiRsrp?.takeIf { it != CellInfo.UNAVAILABLE },
                        timestamp = System.currentTimeMillis()
                    )
                } else {
                    CellularMeasurement(
                        networkType = "5G NR (Legacy)",
                        rsrp = null,
                        rsrq = null,
                        sinr = null,
                        rssi = null,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
            else -> {
                CellularMeasurement(
                    networkType = "Other (${registeredCell.javaClass.simpleName})",
                    rsrp = null,
                    rsrq = null,
                    sinr = null,
                    rssi = null,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }
}
