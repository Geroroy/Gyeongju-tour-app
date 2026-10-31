package io.github.geroroy.gyeongjubadge.core.data

import io.github.geroroy.gyeongjubadge.core.checkin.CheckInConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 내장 장소 데이터 유효성 (docs/data-schema.md §2.1 제약). */
class GyeongjuPlacesTest {
    private val idPattern = Regex("^[a-z0-9-]+$")

    @Test
    fun `ids are unique and kebab-case`() {
        val ids = GyeongjuPlaces.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue("bad id: $it", idPattern.matches(it)) }
    }

    @Test
    fun `coordinates are within Gyeongju bounds`() {
        GyeongjuPlaces.all.forEach {
            assertTrue("${it.id} latitude ${it.latitude}", it.latitude in 35.6..36.1)
            assertTrue("${it.id} longitude ${it.longitude}", it.longitude in 128.9..129.6)
        }
    }

    @Test
    fun `radius is within configured bounds`() {
        val cfg = CheckInConfig.Default
        GyeongjuPlaces.all.forEach {
            assertTrue("${it.id} radius ${it.radiusMeters}", it.radiusMeters in cfg.minRadiusMeters..cfg.maxRadiusMeters)
        }
    }

    @Test
    fun `names and descriptions are present`() {
        GyeongjuPlaces.all.forEach {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.description.isNotBlank())
        }
    }

    @Test
    fun `first place is Cheomseongdae and lookup works`() {
        assertEquals("cheomseongdae", GyeongjuPlaces.first.id)
        assertEquals(GyeongjuPlaces.first, GyeongjuPlaces.findById("cheomseongdae"))
        assertEquals(null, GyeongjuPlaces.findById("missing"))
    }
}
