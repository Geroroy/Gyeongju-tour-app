# 방문 인증(체크인) 규칙

작성일: 2026-10-31 · 적용 범위: 모든 장소 · 구현 위치: `core` 모듈 `checkin` 패키지

## 1. 원칙

1. **사용자 주도**: 인증은 사용자가 버튼을 눌렀을 때만 수행한다. 백그라운드·상시 추적 없음.
2. **1회 측정 판정**: 요청마다 현재 위치를 **한 번** 얻어 규칙을 적용한다. 실패하면 사유를 알리고 재시도를 허용한다.
3. **보수적 판정**: 정확도를 알 수 없거나 시각이 불일치하면 실패로 처리한다(오탐보다 미탐을 택함).
4. **장소별 1회 지급**: 같은 장소 배지는 한 번만 지급한다. 저장소 수준에서도 중복 삽입을 막는다(기본키 = 장소 ID).
5. **화면·실제 위치 API와 분리**: 규칙은 순수 Kotlin(`core`)에 두고, 위치는 `LocationProvider` 인터페이스로 주입한다. 테스트용 `FakeLocationProvider`를 제공한다.

## 2. 설정값 (`CheckInConfig`)

모든 임계값은 상수가 아니라 **설정 객체**로 관리한다. 초기값은 아래와 같고, 현장 검증 후 조정한다.

| 키 | 초기값 | 의미 | 조정 가이드 |
| --- | --- | --- | --- |
| `minRadiusMeters` | 50 m | 장소별 인증 반경의 하한 | 좁은 가게(맛집)는 50 m 근처 |
| `maxRadiusMeters` | 150 m | 장소별 인증 반경의 상한 | 넓은 유적(불국사 등)은 150 m |
| `maxAccuracyMeters` | 50 m | 허용하는 위치 정확도(반경, 68% 신뢰) 상한 | GPS 없는 워치 실측 후 조정 |
| `maxFixAgeMillis` | 30,000 ms | 위치 측정 시각부터 판정 시각까지 허용 지연 | FLP `maxUpdateAge`에도 동일 적용 |
| `requestTimeoutMillis` | 20,000 ms | 위치 요청 전체 제한 시간 | FLP `duration`에도 동일 적용 |

- 장소 데이터의 `radiusMeters`는 `[minRadiusMeters, maxRadiusMeters]` 범위를 지켜야 하며(데이터 테스트로 강제), 런타임에서는 안전을 위해 범위로 **클램프**한다.
- 설정값은 생성 시 유효성 검사(양수, min ≤ max)를 통과해야 한다.
- 첫 구현에서는 `CheckInConfig.Default`를 코드에서 주입한다. 원격 설정·설정 화면은 범위 밖이다.

## 3. 판정 절차

입력: `Place`(좌표·반경), `BadgeRepository`, `LocationProvider`, `Clock`, `CheckInConfig`
출력: `CheckInResult` (아래 §4)

```
1. 중복 선검사      repository.find(place.id) 가 있으면 → AlreadyEarned (위치 요청 생략, 배터리 절약)
2. 위치 요청        withTimeout(requestTimeoutMillis) { provider.currentFix(spec) }
                    spec = (timeoutMillis = requestTimeoutMillis, maxAgeMillis = maxFixAgeMillis)
   2a. 시간 내 응답 없음                          → Timeout
   2b. provider 가 PermissionDenied 반환           → PermissionDenied
   2c. provider 가 Unavailable 반환(위치 꺼짐 등)  → LocationUnavailable
3. 정확도 검사      accuracy == null 또는 accuracy > maxAccuracyMeters → LowAccuracy
4. 신선도 검사      age = clock.elapsedRealtime - fix.elapsedRealtime
                    age < 0 또는 age > maxFixAgeMillis → StaleLocation
5. 거리 검사        distance = haversine(fix, place) ; radius = clamp(place.radius)
                    distance > radius → OutOfRange
6. 지급             repository.award(badge) 가 true  → Granted
                    false(동시성 등으로 이미 존재)   → AlreadyEarned
```

### 3.1 거리 계산

- 하버사인 공식, 지구 반지름 6,371,008.8 m(평균 반지름). 경주 범위(수 km)에서 오차 < 0.1 %.
- 정확도 오차를 반경에 **더하지 않는다**(즉 `distance ≤ radius`만 본다). 정확도는 별도 임계값으로만 거른다. 현장 검증 후 `distance - accuracy ≤ radius` 완화안을 검토할 수 있다.

### 3.2 시간 기준

- 위치 신선도는 **단조 시계(elapsedRealtime)** 로 비교한다. 벽시계는 사용자가 바꿀 수 있어 사용하지 않는다.
- 배지 획득 시각(`earnedAtEpochMillis`)만 벽시계(epoch)로 기록한다.

## 4. 결과 상태 정의 (`CheckInResult`)

| 상태 | 조건 | 사용자 조치 | 재시도 |
| --- | --- | --- | --- |
| `Granted(badge, distanceMeters)` | 모든 검사 통과, 신규 저장 성공 | — | 불필요 |
| `AlreadyEarned(badge)` | 장소 배지가 이미 존재(선검사 또는 저장 시 충돌) | — | 불필요 |
| `PermissionDenied` | 위치 권한 미허용(요청 거부 포함) | 권한 허용 | 허용 후 가능 |
| `LocationUnavailable(reason)` | 위치 서비스 꺼짐, FLP 응답 null, 예외 | 위치 켜기 | 가능 |
| `Timeout(timeoutMillis)` | 제한 시간 내 위치 미수신 | 개방된 곳에서 재시도 | 가능 |
| `LowAccuracy(accuracyMeters?, maxAccuracyMeters)` | 정확도 없음 또는 임계 초과 | 하늘 보이는 곳에서 재시도 | 가능 |
| `StaleLocation(ageMillis, maxAgeMillis)` | 측정 시각이 너무 오래됨/시계 불일치 | 재시도 | 가능 |
| `OutOfRange(distanceMeters, radiusMeters)` | 반경 밖 | 더 가까이 이동 | 가능 |

UI는 이 상태만 보고 문구를 고른다(문구는 [product-spec.md](product-spec.md) §4.2).

## 5. 위치 제공자 계약 (`LocationProvider`)

```kotlin
interface LocationProvider {
    suspend fun currentFix(spec: LocationRequestSpec): LocationFixResult
}
data class LocationRequestSpec(val timeoutMillis: Long, val maxAgeMillis: Long)
sealed interface LocationFixResult {
    data class Success(val fix: LocationFix) : LocationFixResult
    data object PermissionDenied : LocationFixResult
    data class Unavailable(val reason: String?) : LocationFixResult
}
data class LocationFix(latitude, longitude, accuracyMeters: Double?, elapsedRealtimeMillis: Long)
```

- 구현체는 **예외를 던지지 않고** 결과 타입으로 보고한다(코루틴 취소 예외는 예외).
- 구현체가 자체 타임아웃을 두더라도 `CheckInService`가 `requestTimeoutMillis`로 다시 감싼다.
- Android 구현(`FusedLocationProvider`)은 `getCurrentLocation(CurrentLocationRequest)`에 `PRIORITY_HIGH_ACCURACY`, `durationMillis = timeoutMillis`, `maxUpdateAgeMillis = maxAgeMillis`를 지정한다.
- 테스트 구현(`FakeLocationProvider`)은 결과·지연을 스크립트할 수 있고, `-PuseFakeLocation=true` 빌드에서 앱에 주입되어 에뮬레이터 없이 흐름을 확인할 수 있다.

## 6. 권한 처리

- 필요 권한: `ACCESS_FINE_LOCATION`(정확한 위치). API 31+에서는 `ACCESS_COARSE_LOCATION`과 함께 요청한다.
- 사용자가 "대략적인 위치"만 허용하면 정밀 권한이 없으므로 `PermissionDenied`로 취급한다(50 m 정확도 요건을 만족할 수 없음).
- 권한은 인증 버튼을 누른 **그 시점**에 요청한다(맥락 내 요청). 매 인증마다 `checkSelfPermission`으로 재확인한다.
- "다시 묻지 않음" 상태에서는 설정 앱 안내 문구를 보여준다(첫 구현은 문구만).

## 7. 현장 검증 후 조정 절차

1. 장소별로 반경 가장자리·중심·실내에서 각 3회 이상 인증을 시도해 `accuracy`, `distance`, 소요 시간을 기록한다(배지 기록에 좌표·정확도·거리 저장).
2. 미탐(실제 방문했는데 실패)이 잦으면 `maxAccuracyMeters`↑ 또는 장소 `radiusMeters`↑(상한 150 m).
3. 오탐(인접 장소에서 성공)이 있으면 해당 장소 `radiusMeters`↓(하한 50 m) 또는 좌표 보정.
4. 변경은 `CheckInConfig` 기본값 또는 장소 데이터 수정으로 반영하고, 관련 단위 테스트를 갱신한다.
