package io.github.geroroy.gyeongjubadge.core.location

import io.github.geroroy.gyeongjubadge.core.model.Place
import io.github.geroroy.gyeongjubadge.core.time.Clock
import kotlinx.coroutines.delay

/**
 * 테스트·개발용 위치 제공자.
 *
 * - [nextResult]로 반환값을 스크립트하고, [delayMillis]로 응답 지연을 흉내낸다.
 *   `delayMillis = Long.MAX_VALUE` 면 응답하지 않아 시간 초과를 재현할 수 있다.
 * - 받은 요청 스펙을 [requests]에 기록한다.
 * - 앱에서는 `-PuseFakeLocation=true` 빌드에서 주입된다(docs/architecture.md §4).
 */
class FakeLocationProvider(
    var delayMillis: Long = 0L,
    var nextResult: () -> LocationFixResult = { LocationFixResult.Unavailable("not scripted") },
) : LocationProvider {
    private val _requests = mutableListOf<LocationRequestSpec>()
    val requests: List<LocationRequestSpec> get() = _requests.toList()

    override suspend fun currentFix(spec: LocationRequestSpec): LocationFixResult {
        _requests += spec
        if (delayMillis > 0L) delay(delayMillis)
        return nextResult()
    }

    companion object {
        /**
         * 특정 장소 중심에서 측정한 것처럼 동작하는 제공자. 측정 시각은 호출 시점의 [clock] 값.
         */
        fun at(
            place: Place,
            clock: Clock,
            accuracyMeters: Double? = 10.0,
            delayMillis: Long = 0L,
        ): FakeLocationProvider =
            FakeLocationProvider(delayMillis = delayMillis) {
                LocationFixResult.Success(
                    LocationFix(
                        latitude = place.latitude,
                        longitude = place.longitude,
                        accuracyMeters = accuracyMeters,
                        elapsedRealtimeMillis = clock.elapsedRealtimeMillis(),
                    ),
                )
            }
    }
}
