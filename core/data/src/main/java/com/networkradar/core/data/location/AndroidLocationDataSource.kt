package com.networkradar.core.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.networkradar.core.domain.location.Location
import com.networkradar.core.domain.location.LocationDataSource
import com.networkradar.core.domain.location.LocationObservation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidLocationDataSource(
    private val context: Context,
    private val locationClient: FusedLocationProviderClient
) : LocationDataSource {

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<LocationObservation> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(LocationObservation.PermissionRequired)

            awaitClose { }
            return@callbackFlow
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGpsEnabled && !isNetworkEnabled) {
            trySend(LocationObservation.ServicesDisabled)

        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(1.0f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.lastOrNull()?.let { location ->
                    trySend(
                        LocationObservation.Success(
                            Location(
                                lat = location.latitude,
                                long = location.longitude,
                                accuracy = location.accuracy,
                                timestamp = location.time
                            )
                        )
                    )
                } ?: run {
                    trySend(LocationObservation.Unavailable)
                }
            }
        }

        try {
            locationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            trySend(LocationObservation.PermissionsDenied)
        }

        awaitClose {
            locationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }
}
