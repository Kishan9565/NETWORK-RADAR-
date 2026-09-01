package com.networkradar.core.data.indoor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.networkradar.core.domain.indoor.IndoorPosition
import com.networkradar.core.domain.indoor.PdrDataSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.math.cos
import kotlin.math.sin

class AndroidPdrDataSource(
    context: Context
) : PdrDataSource {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    override val isAvailable: Boolean = stepDetector != null && rotationVector != null

    private val trackingSessionId = MutableStateFlow<String?>(null)
    
    private var currentX = 0f
    private var currentY = 0f
    private var currentHeading = 0f
    private val stepLength = 0.75f // Default meters

    override val currentPosition: Flow<IndoorPosition> = trackingSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) {
            flowOf()
        } else {
            callbackFlow {
                val sensorListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        when (event.sensor.type) {
                            Sensor.TYPE_ROTATION_VECTOR -> {
                                val rotationMatrix = FloatArray(9)
                                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                                val orientation = FloatArray(3)
                                SensorManager.getOrientation(rotationMatrix, orientation)
                                currentHeading = orientation[0] // Azimuth
                            }
                            Sensor.TYPE_STEP_DETECTOR -> {
                                // Update position
                                currentX += stepLength * sin(currentHeading)
                                currentY += stepLength * cos(currentHeading)
                                
                                trySend(
                                    IndoorPosition(
                                        sessionId = sessionId,
                                        x = currentX,
                                        y = currentY,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                if (isAvailable) {
                    sensorManager.registerListener(sensorListener, stepDetector, SensorManager.SENSOR_DELAY_FASTEST)
                    sensorManager.registerListener(sensorListener, rotationVector, SensorManager.SENSOR_DELAY_FASTEST)
                }

                awaitClose {
                    sensorManager.unregisterListener(sensorListener)
                }
            }
        }
    }

    override fun startTracking(sessionId: String) {
        currentX = 0f
        currentY = 0f
        trackingSessionId.value = sessionId
    }

    override fun stopTracking() {
        trackingSessionId.value = null
    }

    override fun resetOrigin() {
        currentX = 0f
        currentY = 0f
    }
}
