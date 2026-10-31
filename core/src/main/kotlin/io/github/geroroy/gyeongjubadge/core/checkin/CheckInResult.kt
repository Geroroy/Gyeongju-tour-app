package io.github.geroroy.gyeongjubadge.core.checkin

import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge

/**
 * 인증 결과 상태. 정의와 사용자 조치는 docs/check-in-rules.md §4.
 * 상태를 추가·변경하면 문서 §4, UI 문구, 테스트를 함께 갱신한다.
 */
sealed interface CheckInResult {
    /** 모든 검사 통과, 배지 신규 저장. */
    data class Granted(val badge: EarnedBadge, val distanceMeters: Double) : CheckInResult

    /** 장소 배지가 이미 존재(선검사 또는 저장 충돌). */
    data class AlreadyEarned(val badge: EarnedBadge) : CheckInResult

    /** 위치 권한 미허용. */
    data object PermissionDenied : CheckInResult

    /** 위치 서비스 꺼짐·응답 없음·예외. */
    data class LocationUnavailable(val reason: String?) : CheckInResult

    /** 제한 시간 내 위치 미수신. */
    data class Timeout(val timeoutMillis: Long) : CheckInResult

    /** 정확도 없음 또는 임계 초과. */
    data class LowAccuracy(val accuracyMeters: Double?, val maxAccuracyMeters: Double) : CheckInResult

    /** 측정 시각이 너무 오래됨 또는 시계 불일치. */
    data class StaleLocation(val ageMillis: Long, val maxAgeMillis: Long) : CheckInResult

    /** 인증 반경 밖. */
    data class OutOfRange(val distanceMeters: Double, val radiusMeters: Double) : CheckInResult
}
