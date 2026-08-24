package com.networkradar.core.domain.networking

data class ConnectivityState(
    val isConnected: Boolean,
    val networkType: NetworkType,
    val isInternetAvailable: Boolean = false
)
