package io.github.geroroy.gyeongjubadge.core.location

import io.github.geroroy.gyeongjubadge.core.data.GyeongjuPlaces
import io.github.geroroy.gyeongjubadge.core.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeLocationProviderTest {
    private val spec = LocationRequestSpec(timeoutMillis = 20_000L, maxAgeMillis = 30_000L)

    @Test
    fun `returns scripted result and records requests`() = runTest {
        val provider = FakeLocationProvider { LocationFixResult.PermissionDenied }

        assertEquals(LocationFixResult.PermissionDenied, provider.currentFix(spec))
        assertEquals(listOf(spec), provider.requests)
    }

    @Test
    fun `honours delay`() = runTest {
        val provider = FakeLocationProvider(delayMillis = 1_500L) { LocationFixResult.Unavailable(null) }
        val start = currentTime
        provider.currentFix(spec)
        assertEquals(1_500L, currentTime - start)
    }

    @Test
    fun `at() reports the place centre with the clock time`() = runTest {
        val clock = object : Clock {
            override fun elapsedRealtimeMillis() = 42L

            override fun currentTimeMillis() = 0L
        }
        val place = GyeongjuPlaces.first
        val result = FakeLocationProvider.at(place, clock, accuracyMeters = 7.0).currentFix(spec)

        val fix = (result as LocationFixResult.Success).fix
        assertEquals(place.latitude, fix.latitude, 0.0)
        assertEquals(place.longitude, fix.longitude, 0.0)
        assertEquals(7.0, fix.accuracyMeters!!, 0.0)
        assertEquals(42L, fix.elapsedRealtimeMillis)
    }
}
