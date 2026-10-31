package io.github.geroroy.gyeongjubadge.core.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 두 WGS84 좌표 사이의 대원 거리(하버사인). docs/check-in-rules.md §3.1 */
object GeoDistance {
    /** 지구 평균 반지름(m). */
    const val EARTH_RADIUS_METERS: Double = 6_371_008.8

    fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lon2 - lon1)

        val sinDPhi = sin(dPhi / 2)
        val sinDLambda = sin(dLambda / 2)
        val a = sinDPhi * sinDPhi + cos(phi1) * cos(phi2) * sinDLambda * sinDLambda
        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return EARTH_RADIUS_METERS * c
    }
}
