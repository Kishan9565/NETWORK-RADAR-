package com.networkradar.core.data.location

import com.networkradar.core.domain.location.Location

fun android.location.Location.toDomain(): Location {
    return Location(
        lat = latitude,
        long = longitude,
        accuracy = accuracy,
        timestamp = time
    )
}
