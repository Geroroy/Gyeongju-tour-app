package io.github.geroroy.gyeongjubadge.core.geo

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class GeoDistanceTest {
    @Test
    fun `same point is zero`() {
        assertEquals(0.0, GeoDistance.haversineMeters(35.8347, 129.2190, 35.8347, 129.2190), 1e-9)
    }

    @Test
    fun `cheomseongdae to donggung-wolji is about 677 m`() {
        // dlon 0.0075° at lat 35.83° ≈ 677 m, dlat 0.0002° ≈ 22 m → ≈ 677.3 m (평면 근사)
        val d = GeoDistance.haversineMeters(35.8347, 129.2190, 35.8349, 129.2265)
        assertEquals(677.0, d, 7.0) // ±1 %
    }

    @Test
    fun `one arcsecond of latitude is about 30_87 m`() {
        val d = GeoDistance.haversineMeters(0.0, 0.0, 1.0 / 3600.0, 0.0)
        assertEquals(GeoDistance.EARTH_RADIUS_METERS * (PI / 180.0) / 3600.0, d, 0.001)
    }

    @Test
    fun `half circumference along equator`() {
        val d = GeoDistance.haversineMeters(0.0, 0.0, 0.0, 180.0)
        assertEquals(PI * GeoDistance.EARTH_RADIUS_METERS, d, 1.0)
    }

    @Test
    fun `is symmetric`() {
        val ab = GeoDistance.haversineMeters(35.7900, 129.3320, 35.8347, 129.2190)
        val ba = GeoDistance.haversineMeters(35.8347, 129.2190, 35.7900, 129.3320)
        assertEquals(ab, ba, 1e-9)
    }
}
