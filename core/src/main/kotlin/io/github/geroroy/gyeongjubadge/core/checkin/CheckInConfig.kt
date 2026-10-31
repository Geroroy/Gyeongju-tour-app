package io.github.geroroy.gyeongjubadge.core.checkin

import io.github.geroroy.gyeongjubadge.core.model.Place

/**
 * 인증 임계값. 초기값은 docs/check-in-rules.md §2 — 현장 검증 후 조정 가능한 설정으로 관리한다.
 *
 * @property minRadiusMeters 장소별 인증 반경 하한(m)
 * @property maxRadiusMeters 장소별 인증 반경 상한(m)
 * @property maxAccuracyMeters 허용 위치 정확도 상한(m)
 * @property maxFixAgeMillis 위치 측정 후 허용 지연(ms)
 * @property requestTimeoutMillis 위치 요청 제한 시간(ms)
 */
data class CheckInConfig(
    val minRadiusMeters: Double = 50.0,
    val maxRadiusMeters: Double = 150.0,
    val maxAccuracyMeters: Double = 50.0,
    val maxFixAgeMillis: Long = 30_000L,
    val requestTimeoutMillis: Long = 20_000L,
) {
    init {
        require(minRadiusMeters > 0.0) { "minRadiusMeters must be positive" }
        require(maxRadiusMeters >= minRadiusMeters) { "maxRadiusMeters must be >= minRadiusMeters" }
        require(maxAccuracyMeters > 0.0) { "maxAccuracyMeters must be positive" }
        require(maxFixAgeMillis > 0L) { "maxFixAgeMillis must be positive" }
        require(requestTimeoutMillis > 0L) { "requestTimeoutMillis must be positive" }
    }

    /** 장소 반경을 [minRadiusMeters, maxRadiusMeters] 로 클램프한 실제 판정 반경. */
    fun effectiveRadiusMeters(place: Place): Double = place.radiusMeters.coerceIn(minRadiusMeters, maxRadiusMeters)

    companion object {
        val Default: CheckInConfig = CheckInConfig()
    }
}
