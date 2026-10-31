package io.github.geroroy.gyeongjubadge.core.repository

import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryBadgeRepositoryTest {
    private fun badge(placeId: String, at: Long) = EarnedBadge(placeId, at, 35.0, 129.0, 10.0, 5.0)

    @Test
    fun `award is idempotent per place`() = runTest {
        val repo = InMemoryBadgeRepository()

        assertTrue(repo.award(badge("a", 1L)))
        assertFalse(repo.award(badge("a", 2L)))

        assertEquals(1L, repo.find("a")!!.earnedAtEpochMillis) // 첫 기록 유지
        assertEquals(1, repo.size)
    }

    @Test
    fun `find returns null for unknown place`() = runTest {
        assertNull(InMemoryBadgeRepository().find("nope"))
    }

    @Test
    fun `observeAll is sorted by earned time descending`() = runTest {
        val repo = InMemoryBadgeRepository()
        repo.award(badge("a", 1L))
        repo.award(badge("b", 3L))
        repo.award(badge("c", 2L))

        assertEquals(listOf("b", "c", "a"), repo.observeAll().first().map { it.placeId })
    }
}
