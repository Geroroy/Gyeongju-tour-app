package io.github.geroroy.gyeongjubadge.core.repository

import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 배지 저장소. docs/data-schema.md §3.3 */
interface BadgeRepository {
    suspend fun find(placeId: String): EarnedBadge?

    /**
     * 배지를 저장한다. 같은 placeId 가 이미 있으면 덮어쓰지 않는다.
     * @return 새로 저장했으면 true, 이미 존재해 무시했으면 false.
     */
    suspend fun award(badge: EarnedBadge): Boolean

    /** 획득 시각 내림차순. */
    fun observeAll(): Flow<List<EarnedBadge>>
}

/** 테스트·프리뷰용 인메모리 구현. */
class InMemoryBadgeRepository(initial: Collection<EarnedBadge> = emptyList()) : BadgeRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow(initial.associateBy { it.placeId })

    override suspend fun find(placeId: String): EarnedBadge? = state.value[placeId]

    override suspend fun award(badge: EarnedBadge): Boolean =
        mutex.withLock {
            if (state.value.containsKey(badge.placeId)) {
                false
            } else {
                state.update { it + (badge.placeId to badge) }
                true
            }
        }

    override fun observeAll(): Flow<List<EarnedBadge>> =
        state.map { map -> map.values.sortedByDescending { it.earnedAtEpochMillis } }

    /** 현재 저장된 개수(테스트 편의). */
    val size: Int get() = state.value.size
}
