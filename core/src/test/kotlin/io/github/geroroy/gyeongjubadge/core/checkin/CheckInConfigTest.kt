package io.github.geroroy.gyeongjubadge.core.checkin

import io.github.geroroy.gyeongjubadge.core.data.GyeongjuPlaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CheckInConfigTest {
    @Test
    fun `defaults match check-in-rules section 2`() {
        val c = CheckInConfig.Default
        assertEquals(50.0, c.minRadiusMeters, 0.0)
        assertEquals(150.0, c.maxRadiusMeters, 0.0)
        assertEquals(50.0, c.maxAccuracyMeters, 0.0)
        assertEquals(30_000L, c.maxFixAgeMillis)
        assertEquals(20_000L, c.requestTimeoutMillis)
    }

    @Test
    fun `effective radius keeps in-range value`() {
        val place = GyeongjuPlaces.first.copy(radiusMeters = 100.0)
        assertEquals(100.0, CheckInConfig.Default.effectiveRadiusMeters(place), 0.0)
    }

    @Test
    fun `effective radius clamps out-of-range values`() {
        val tooSmall = GyeongjuPlaces.first.copy(radiusMeters = 10.0)
        val tooLarge = GyeongjuPlaces.first.copy(radiusMeters = 999.0)
        assertEquals(50.0, CheckInConfig.Default.effectiveRadiusMeters(tooSmall), 0.0)
        assertEquals(150.0, CheckInConfig.Default.effectiveRadiusMeters(tooLarge), 0.0)
    }

    @Test
    fun `rejects invalid values`() {
        assertThrows(IllegalArgumentException::class.java) { CheckInConfig(minRadiusMeters = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { CheckInConfig(minRadiusMeters = 100.0, maxRadiusMeters = 50.0) }
        assertThrows(IllegalArgumentException::class.java) { CheckInConfig(maxAccuracyMeters = -1.0) }
        assertThrows(IllegalArgumentException::class.java) { CheckInConfig(maxFixAgeMillis = 0L) }
        assertThrows(IllegalArgumentException::class.java) { CheckInConfig(requestTimeoutMillis = 0L) }
    }
}
