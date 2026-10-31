package io.github.geroroy.gyeongjubadge.core.checkin

import io.github.geroroy.gyeongjubadge.core.data.GyeongjuPlaces
import io.github.geroroy.gyeongjubadge.core.geo.GeoDistance
import io.github.geroroy.gyeongjubadge.core.location.FakeLocationProvider
import io.github.geroroy.gyeongjubadge.core.location.LocationFix
import io.github.geroroy.gyeongjubadge.core.location.LocationFixResult
import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge
import io.github.geroroy.gyeongjubadge.core.model.Place
import io.github.geroroy.gyeongjubadge.core.repository.BadgeRepository
import io.github.geroroy.gyeongjubadge.core.repository.InMemoryBadgeRepository
import io.github.geroroy.gyeongjubadge.core.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckInServiceTest {
    private class FakeClock(var elapsed: Long = 1_000_000L, var epoch: Long = 1_800_000_000_000L) : Clock {
        override fun elapsedRealtimeMillis(): Long = elapsed

        override fun currentTimeMillis(): Long = epoch
    }

    private val place: Place = GyeongjuPlaces.first // radius 100 m
    private val clock = FakeClock()
    private val repository = InMemoryBadgeRepository()
    private val provider = FakeLocationProvider()
    private val config = CheckInConfig.Default

    private fun service(
        repo: BadgeRepository = repository,
        cfg: CheckInConfig = config,
    ) = CheckInService(repo, provider, clock, cfg)

    /** 장소 중심에서 정북으로 [meters] 만큼 떨어진 측정값. 자오선 이동이므로 하버사인 거리가 정확히 meters 가 된다. */
    private fun fixAt(
        meters: Double,
        accuracy: Double? = 10.0,
        ageMillis: Long = 0L,
    ): LocationFixResult {
        val dLat = Math.toDegrees(meters / GeoDistance.EARTH_RADIUS_METERS)
        return LocationFixResult.Success(
            LocationFix(
                latitude = place.latitude + dLat,
                longitude = place.longitude,
                accuracyMeters = accuracy,
                elapsedRealtimeMillis = clock.elapsed - ageMillis,
            ),
        )
    }

    @Test
    fun `grants badge inside radius with good fresh fix`() = runTest {
        provider.nextResult = { fixAt(30.0) }

        val result = service().checkIn(place)

        val granted = result as CheckInResult.Granted
        assertEquals(30.0, granted.distanceMeters, 0.01)
        assertEquals(place.id, granted.badge.placeId)
        assertEquals(clock.epoch, granted.badge.earnedAtEpochMillis)
        assertEquals(10.0, granted.badge.accuracyMeters!!, 0.0)
        assertEquals(1, repository.size)
        assertNotNull(repository.find(place.id))
    }

    @Test
    fun `radius edge - one metre inside grants, one metre outside is OutOfRange`() = runTest {
        provider.nextResult = { fixAt(99.0) }
        assertTrue(service().checkIn(place) is CheckInResult.Granted)

        val fresh = InMemoryBadgeRepository()
        provider.nextResult = { fixAt(101.0) }
        val out = service(repo = fresh).checkIn(place) as CheckInResult.OutOfRange
        assertEquals(101.0, out.distanceMeters, 0.01)
        assertEquals(100.0, out.radiusMeters, 0.0)
        assertEquals(0, fresh.size)
    }

    @Test
    fun `already earned is detected before requesting location`() = runTest {
        val existing = EarnedBadge(place.id, 1L, place.latitude, place.longitude, 5.0, 1.0)
        val repo = InMemoryBadgeRepository(listOf(existing))
        provider.nextResult = { fixAt(10.0) }

        val result = service(repo = repo).checkIn(place)

        assertEquals(CheckInResult.AlreadyEarned(existing), result)
        assertTrue("location must not be requested", provider.requests.isEmpty())
    }

    @Test
    fun `repository rejecting the insert yields AlreadyEarned`() = runTest {
        val stored = EarnedBadge(place.id, 2L, place.latitude, place.longitude, 5.0, 1.0)
        val rejecting = object : BadgeRepository {
            var findCalls = 0

            override suspend fun find(placeId: String): EarnedBadge? = if (findCalls++ == 0) null else stored

            override suspend fun award(badge: EarnedBadge): Boolean = false

            override fun observeAll(): Flow<List<EarnedBadge>> = throw UnsupportedOperationException()
        }
        provider.nextResult = { fixAt(10.0) }

        val result = service(repo = rejecting).checkIn(place)

        assertEquals(CheckInResult.AlreadyEarned(stored), result)
    }

    @Test
    fun `permission denied from provider`() = runTest {
        provider.nextResult = { LocationFixResult.PermissionDenied }
        assertEquals(CheckInResult.PermissionDenied, service().checkIn(place))
        assertEquals(0, repository.size)
    }

    @Test
    fun `unavailable from provider`() = runTest {
        provider.nextResult = { LocationFixResult.Unavailable("location off") }
        assertEquals(CheckInResult.LocationUnavailable("location off"), service().checkIn(place))
    }

    @Test
    fun `accuracy above threshold is LowAccuracy`() = runTest {
        provider.nextResult = { fixAt(10.0, accuracy = 51.0) }
        assertEquals(CheckInResult.LowAccuracy(51.0, 50.0), service().checkIn(place))
        assertEquals(0, repository.size)
    }

    @Test
    fun `unknown accuracy is LowAccuracy`() = runTest {
        provider.nextResult = { fixAt(10.0, accuracy = null) }
        assertEquals(CheckInResult.LowAccuracy(null, 50.0), service().checkIn(place))
    }

    @Test
    fun `accuracy exactly at threshold is accepted`() = runTest {
        provider.nextResult = { fixAt(10.0, accuracy = 50.0) }
        assertTrue(service().checkIn(place) is CheckInResult.Granted)
    }

    @Test
    fun `fix older than max age is StaleLocation`() = runTest {
        provider.nextResult = { fixAt(10.0, ageMillis = 30_001L) }
        assertEquals(CheckInResult.StaleLocation(30_001L, 30_000L), service().checkIn(place))
    }

    @Test
    fun `fix exactly at max age is accepted`() = runTest {
        provider.nextResult = { fixAt(10.0, ageMillis = 30_000L) }
        assertTrue(service().checkIn(place) is CheckInResult.Granted)
    }

    @Test
    fun `fix from the future is StaleLocation`() = runTest {
        provider.nextResult = { fixAt(10.0, ageMillis = -1L) }
        assertEquals(CheckInResult.StaleLocation(-1L, 30_000L), service().checkIn(place))
    }

    @Test
    fun `provider slower than timeout yields Timeout after exactly the timeout`() = runTest {
        provider.delayMillis = 20_001L
        provider.nextResult = { fixAt(10.0) }

        val start = currentTime
        val result = service().checkIn(place)

        assertEquals(CheckInResult.Timeout(20_000L), result)
        assertEquals(20_000L, currentTime - start)
        assertEquals(0, repository.size)
    }

    @Test
    fun `provider that never answers yields Timeout`() = runTest {
        provider.delayMillis = Long.MAX_VALUE
        assertEquals(CheckInResult.Timeout(20_000L), service().checkIn(place))
    }

    @Test
    fun `request spec carries configured timeout and max age`() = runTest {
        provider.nextResult = { fixAt(10.0) }
        service().checkIn(place)

        val spec = provider.requests.single()
        assertEquals(20_000L, spec.timeoutMillis)
        assertEquals(30_000L, spec.maxAgeMillis)
    }

    @Test
    fun `adjusted config is honoured`() = runTest {
        val relaxed = CheckInConfig(maxAccuracyMeters = 80.0, requestTimeoutMillis = 5_000L)
        provider.nextResult = { fixAt(10.0, accuracy = 60.0) }

        assertTrue(service(cfg = relaxed).checkIn(place) is CheckInResult.Granted)
        assertEquals(5_000L, provider.requests.single().timeoutMillis)
    }

    @Test
    fun `place radius outside config bounds is clamped for the distance check`() = runTest {
        val wide = place.copy(radiusMeters = 500.0) // 상한 150 으로 클램프
        provider.nextResult = { fixAt(200.0) }

        val out = service().checkIn(wide) as CheckInResult.OutOfRange
        assertEquals(150.0, out.radiusMeters, 0.0)
    }

    @Test
    fun `second check-in after grant is AlreadyEarned and keeps one record`() = runTest {
        provider.nextResult = { fixAt(10.0) }
        val first = service().checkIn(place) as CheckInResult.Granted

        val second = service().checkIn(place)

        assertEquals(CheckInResult.AlreadyEarned(first.badge), second)
        assertEquals(1, repository.size)
        assertEquals(1, provider.requests.size)
        assertEquals(listOf(first.badge), repository.observeAll().first())
    }

    @Test
    fun `failures do not persist anything`() = runTest {
        provider.nextResult = { fixAt(500.0) }
        service().checkIn(place)
        assertNull(repository.find(place.id))
        assertFalse(repository.observeAll().first().isNotEmpty())
    }
}
