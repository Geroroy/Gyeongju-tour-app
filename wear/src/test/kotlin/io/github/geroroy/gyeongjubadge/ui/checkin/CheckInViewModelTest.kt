package io.github.geroroy.gyeongjubadge.ui.checkin

import io.github.geroroy.gyeongjubadge.core.checkin.CheckInConfig
import io.github.geroroy.gyeongjubadge.core.checkin.CheckInResult
import io.github.geroroy.gyeongjubadge.core.checkin.CheckInService
import io.github.geroroy.gyeongjubadge.core.data.GyeongjuPlaces
import io.github.geroroy.gyeongjubadge.core.location.FakeLocationProvider
import io.github.geroroy.gyeongjubadge.core.location.LocationFixResult
import io.github.geroroy.gyeongjubadge.core.repository.InMemoryBadgeRepository
import io.github.geroroy.gyeongjubadge.core.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** L2: ViewModel 상태 전이 (docs/test-plan.md §3). Android 프레임워크 없이 JVM 에서 실행. */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckInViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val place = GyeongjuPlaces.first
    private val clock = object : Clock {
        override fun elapsedRealtimeMillis() = 10_000L

        override fun currentTimeMillis() = 1_800_000_000_000L
    }
    private lateinit var repository: InMemoryBadgeRepository
    private lateinit var provider: FakeLocationProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = InMemoryBadgeRepository()
        provider = FakeLocationProvider.at(place, clock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        CheckInViewModel(
            place = place,
            checkInService = CheckInService(repository, provider, clock, CheckInConfig.Default),
            repository = repository,
        )

    @Test
    fun `initial state is idle and not earned`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(place, s.place)
        assertNull(s.earnedBadge)
        assertEquals(CheckInPhase.Idle, s.phase)
        assertTrue(s.canCheckIn)
    }

    @Test
    fun `checkIn goes Checking then Completed Granted and earnedBadge updates from repository`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.checkIn()
        assertEquals(CheckInPhase.Checking, vm.state.value.phase)
        assertFalse(vm.state.value.canCheckIn)

        advanceUntilIdle()
        val phase = vm.state.value.phase as CheckInPhase.Completed
        assertTrue(phase.result is CheckInResult.Granted)
        assertNotNull(vm.state.value.earnedBadge)
        assertTrue(vm.state.value.isEarned)
        assertFalse(vm.state.value.canCheckIn)
    }

    @Test
    fun `duplicate checkIn while checking is ignored`() = runTest(dispatcher) {
        provider.delayMillis = 1_000L
        val vm = viewModel()
        advanceUntilIdle()

        vm.checkIn()
        vm.checkIn()
        advanceUntilIdle()

        assertEquals(1, provider.requests.size)
    }

    @Test
    fun `permission denied from launcher completes with PermissionDenied`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onPermissionDenied()

        assertEquals(CheckInPhase.Completed(CheckInResult.PermissionDenied), vm.state.value.phase)
        assertTrue(provider.requests.isEmpty())
    }

    @Test
    fun `failure result keeps canCheckIn true for retry`() = runTest(dispatcher) {
        provider.nextResult = { LocationFixResult.Unavailable("off") }
        val vm = viewModel()
        advanceUntilIdle()

        vm.checkIn()
        advanceUntilIdle()

        assertEquals(CheckInPhase.Completed(CheckInResult.LocationUnavailable("off")), vm.state.value.phase)
        assertTrue(vm.state.value.canCheckIn)
    }

    @Test
    fun `dismissResult returns to Idle`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onPermissionDenied()

        vm.dismissResult()

        assertEquals(CheckInPhase.Idle, vm.state.value.phase)
    }
}
