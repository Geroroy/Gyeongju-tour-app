package io.github.geroroy.gyeongjubadge.core.model

/**
 * 획득한 배지. 장소당 최대 1개(docs/data-schema.md §2.2).
 *
 * 판정에 사용한 좌표·정확도·거리는 현장 검증과 임계값 조정을 위해 기록한다. 외부 전송은 없다.
 */
data class EarnedBadge(
    val placeId: String,
    val earnedAtEpochMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    val distanceMeters: Double,
)
