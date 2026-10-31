package io.github.geroroy.gyeongjubadge.core.location

/**
 * 위치 1회 측정 결과. docs/check-in-rules.md §5
 *
 * @property accuracyMeters 68% 신뢰 반경(m). 알 수 없으면 null (보수적으로 실패 처리됨).
 * @property elapsedRealtimeMillis 측정 시각(단조 시계, ms). [io.github.geroroy.gyeongjubadge.core.time.Clock.elapsedRealtimeMillis] 와 같은 기준이어야 한다.
 */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    val elapsedRealtimeMillis: Long,
)

/**
 * 위치 요청 조건. 구현체는 가능하면 자체 요청에도 반영한다(FLP: durationMillis / maxUpdateAgeMillis).
 */
data class LocationRequestSpec(
    val timeoutMillis: Long,
    val maxAgeMillis: Long,
)

/** 위치 제공자의 응답. 예외 대신 결과 타입으로 보고한다. */
sealed interface LocationFixResult {
    data class Success(val fix: LocationFix) : LocationFixResult

    data object PermissionDenied : LocationFixResult

    data class Unavailable(val reason: String?) : LocationFixResult
}

/**
 * 현재 위치를 1회 제공한다. 화면·실제 위치 API와 인증 규칙을 분리하는 경계.
 *
 * 구현 규약:
 * - 코루틴 취소(CancellationException) 외의 예외를 던지지 않는다.
 * - 자체 타임아웃을 두어도 좋지만, 호출자(CheckInService)가 [LocationRequestSpec.timeoutMillis]로 다시 감싼다.
 */
interface LocationProvider {
    suspend fun currentFix(spec: LocationRequestSpec): LocationFixResult
}
