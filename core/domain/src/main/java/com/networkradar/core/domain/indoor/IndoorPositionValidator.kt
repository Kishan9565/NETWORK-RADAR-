package com.networkradar.core.domain.indoor

object IndoorPositionValidator {
    fun validate(position: IndoorPosition, map: IndoorMap): Boolean {
        return position.mapId == map.id &&
                position.x in 0f..map.width &&
                position.y in 0f..map.height
    }
}
