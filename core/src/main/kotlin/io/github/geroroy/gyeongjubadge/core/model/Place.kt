package io.github.geroroy.gyeongjubadge.core.model

/** 장소 분류. */
enum class PlaceCategory {
    /** 명소(유적·관광지). */
    LANDMARK,

    /** 맛집·먹거리. */
    FOOD,
}

/**
 * 앱에 내장된 장소. 스키마는 docs/data-schema.md §2.1.
 *
 * @property id 배지 기록과 연결되는 고유 식별자. 배포 후 변경 금지.
 * @property radiusMeters 인증 반경(m). 데이터는 CheckInConfig 의 [min, max] 범위를 지켜야 한다.
 * @property verified 좌표·반경 현장 검증 완료 여부.
 */
data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val description: String,
    val verified: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "Place.id must not be blank" }
        require(name.isNotBlank()) { "Place.name must not be blank" }
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
        require(radiusMeters > 0.0) { "radiusMeters must be positive: $radiusMeters" }
    }
}
