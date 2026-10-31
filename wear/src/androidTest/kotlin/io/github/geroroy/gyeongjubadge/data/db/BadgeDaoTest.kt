package io.github.geroroy.gyeongjubadge.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** L3: Room DAO / Repository 영속성 (docs/test-plan.md §4). 에뮬레이터/실기기에서 실행. */
@RunWith(AndroidJUnit4::class)
class BadgeDaoTest {
    private lateinit var db: BadgeDatabase
    private lateinit var repository: RoomBadgeRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, BadgeDatabase::class.java).build()
        repository = RoomBadgeRepository(db.badgeDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun badge(placeId: String, at: Long) = EarnedBadge(placeId, at, 35.8347, 129.2190, 12.0, 30.0)

    @Test
    fun insertIfAbsent_isIdempotentPerPlace() = runTest {
        assertTrue(repository.award(badge("cheomseongdae", 1L)))
        assertFalse(repository.award(badge("cheomseongdae", 2L)))

        val stored = repository.find("cheomseongdae")!!
        assertEquals(1L, stored.earnedAtEpochMillis)
        assertEquals(12.0, stored.accuracyMeters!!, 0.0)
    }

    @Test
    fun find_returnsNullWhenMissing() = runTest {
        assertNull(repository.find("bulguksa"))
    }

    @Test
    fun observeAll_sortedByEarnedAtDescending() = runTest {
        repository.award(badge("a", 1L))
        repository.award(badge("b", 3L))
        repository.award(badge("c", 2L))

        assertEquals(listOf("b", "c", "a"), repository.observeAll().first().map { it.placeId })
    }
}
