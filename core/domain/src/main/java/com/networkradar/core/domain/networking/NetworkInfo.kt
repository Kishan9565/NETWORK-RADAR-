package com.networkradar.core.domain.networking

data class NetworkInfo(
    val type: NetworkType,
    val isConnected: Boolean,
    val networkName: String? = null
)
