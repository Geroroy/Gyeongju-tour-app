package io.github.geroroy.gyeongjubadge.data.db

import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge
import io.github.geroroy.gyeongjubadge.core.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room 기반 [BadgeRepository]. 중복 판정은 DAO 의 IGNORE 삽입 결과(-1)로 한다. */
class RoomBadgeRepository(private val dao: BadgeDao) : BadgeRepository {
    override suspend fun find(placeId: String): EarnedBadge? = dao.findByPlaceId(placeId)?.toDomain()

    override suspend fun award(badge: EarnedBadge): Boolean = dao.insertIfAbsent(BadgeEntity.fromDomain(badge)) != -1L

    override fun observeAll(): Flow<List<EarnedBadge>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
}
