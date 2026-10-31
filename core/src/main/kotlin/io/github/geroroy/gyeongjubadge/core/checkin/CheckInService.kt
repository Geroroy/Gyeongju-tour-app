package io.github.geroroy.gyeongjubadge.core.checkin

import io.github.geroroy.gyeongjubadge.core.geo.GeoDistance
import io.github.geroroy.gyeongjubadge.core.location.LocationFixResult
import io.github.geroroy.gyeongjubadge.core.location.LocationProvider
import io.github.geroroy.gyeongjubadge.core.location.LocationRequestSpec
import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge
import io.github.geroroy.gyeongjubadge.core.model.Place
import io.github.geroroy.gyeongjubadge.core.repository.BadgeRepository
import io.github.geroroy.gyeongjubadge.core.time.Clock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 방문 인증 규칙 구현. 절차는 docs/check-in-rules.md §3 과 1:1 로 대응한다.
 *
 * 화면이나 실제 위치 API에 의존하지 않으며, 모든 외부 요소는 생성자로 주입된다.
 */
class CheckInService(
    private val repository: BadgeRepository,
    private val locationProvider: LocationProvider,
    private val clock: Clock,
    private val config: CheckInConfig = CheckInConfig.Default,
) {
    suspend fun checkIn(place: Place): CheckInResult {
        // 1. 중복 선검사 — 위치 요청을 생략해 배터리를 아낀다.
        repository.find(place.id)?.let { return CheckInResult.AlreadyEarned(it) }

        // 2. 위치 요청(제한 시간 적용)
        val spec = LocationRequestSpec(
            timeoutMillis = config.requestTimeoutMillis,
            maxAgeMillis = config.maxFixAgeMillis,
        )
        val fixResult = withTimeoutOrNull(config.requestTimeoutMillis) { locationProvider.currentFix(spec) }
            ?: return CheckInResult.Timeout(config.requestTimeoutMillis)

        val fix = when (fixResult) {
            is LocationFixResult.Success -> fixResult.fix
            LocationFixResult.PermissionDenied -> return CheckInResult.PermissionDenied
            is LocationFixResult.Unavailable -> return CheckInResult.LocationUnavailable(fixResult.reason)
        }

        // 3. 정확도 검사 — 알 수 없는 정확도는 실패로 처리(보수적 판정)
        val accuracy = fix.accuracyMeters
        if (accuracy == null || accuracy > config.maxAccuracyMeters) {
            return CheckInResult.LowAccuracy(accuracy, config.maxAccuracyMeters)
        }

        // 4. 신선도 검사 — 단조 시계 기준, 음수(미래 시각)도 불일치로 본다
        val age = clock.elapsedRealtimeMillis() - fix.elapsedRealtimeMillis
        if (age < 0L || age > config.maxFixAgeMillis) {
            return CheckInResult.StaleLocation(age, config.maxFixAgeMillis)
        }

        // 5. 거리 검사
        val distance = GeoDistance.haversineMeters(fix.latitude, fix.longitude, place.latitude, place.longitude)
        val radius = config.effectiveRadiusMeters(place)
        if (distance > radius) {
            return CheckInResult.OutOfRange(distance, radius)
        }

        // 6. 지급 — 저장소가 중복을 거부하면 이미 획득으로 처리
        val badge = EarnedBadge(
            placeId = place.id,
            earnedAtEpochMillis = clock.currentTimeMillis(),
            latitude = fix.latitude,
            longitude = fix.longitude,
            accuracyMeters = accuracy,
            distanceMeters = distance,
        )
        return if (repository.award(badge)) {
            CheckInResult.Granted(badge, distance)
        } else {
            CheckInResult.AlreadyEarned(repository.find(place.id) ?: badge)
        }
    }
}
