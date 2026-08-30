package com.networkradar.core.domain.location

sealed interface LocationObservation {
    data class Success(val location: Location) : LocationObservation
    data object Loading : LocationObservation
    data object PermissionRequired : LocationObservation
    data object PermissionsDenied : LocationObservation
    data object ServicesDisabled : LocationObservation
    data object Unavailable : LocationObservation
}
