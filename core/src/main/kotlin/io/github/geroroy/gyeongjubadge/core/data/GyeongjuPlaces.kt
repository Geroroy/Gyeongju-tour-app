package io.github.geroroy.gyeongjubadge.core.data

import io.github.geroroy.gyeongjubadge.core.model.Place
import io.github.geroroy.gyeongjubadge.core.model.PlaceCategory

/**
 * 앱 내장 장소 데이터. docs/data-schema.md §5
 *
 * 좌표·반경은 공개 지도 기준 근사값이며 현장 검증 전까지 `verified = false` 다.
 * 첫 구현 UI는 [first] 만 표시한다.
 */
object GyeongjuPlaces {
    val all: List<Place> = listOf(
        Place(
            id = "cheomseongdae",
            name = "첨성대",
            category = PlaceCategory.LANDMARK,
            latitude = 35.8347,
            longitude = 129.2190,
            radiusMeters = 100.0,
            description = "신라의 천문 관측대. 동양에서 가장 오래된 천문대로 알려져 있어요.",
        ),
        Place(
            id = "bulguksa",
            name = "불국사",
            category = PlaceCategory.LANDMARK,
            latitude = 35.7900,
            longitude = 129.3320,
            radiusMeters = 150.0,
            description = "토함산 자락의 유네스코 세계유산 사찰. 다보탑과 석가탑이 있어요.",
        ),
        Place(
            id = "donggung-wolji",
            name = "동궁과 월지",
            category = PlaceCategory.LANDMARK,
            latitude = 35.8349,
            longitude = 129.2265,
            radiusMeters = 150.0,
            description = "신라 왕궁의 별궁 터와 연못. 밤 풍경이 특히 아름다워요.",
        ),
        Place(
            id = "hwangnidan-gil",
            name = "황리단길",
            category = PlaceCategory.FOOD,
            latitude = 35.8370,
            longitude = 129.2110,
            radiusMeters = 150.0,
            description = "한옥 사이로 카페와 맛집이 이어지는 거리. 황남빵 한 입은 필수!",
        ),
    )

    /** 첫 구현에서 표시하는 단일 장소. */
    val first: Place get() = all.first()

    fun findById(id: String): Place? = all.firstOrNull { it.id == id }
}
