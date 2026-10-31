package io.github.geroroy.gyeongjubadge.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge

/** 테이블 `earned_badges`. 스키마는 docs/data-schema.md §3.1. 기본키 = 장소 ID → 장소별 1회 지급 보장. */
@Entity(tableName = "earned_badges")
data class BadgeEntity(
    @PrimaryKey @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "earned_at") val earnedAtEpochMillis: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "accuracy_m") val accuracyMeters: Double?,
    @ColumnInfo(name = "distance_m") val distanceMeters: Double,
) {
    fun toDomain(): EarnedBadge =
        EarnedBadge(
            placeId = placeId,
            earnedAtEpochMillis = earnedAtEpochMillis,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            distanceMeters = distanceMeters,
        )

    companion object {
        fun fromDomain(badge: EarnedBadge): BadgeEntity =
            BadgeEntity(
                placeId = badge.placeId,
                earnedAtEpochMillis = badge.earnedAtEpochMillis,
                latitude = badge.latitude,
                longitude = badge.longitude,
                accuracyMeters = badge.accuracyMeters,
                distanceMeters = badge.distanceMeters,
            )
    }
}
