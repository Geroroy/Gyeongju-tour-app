# 테스트 계획

작성일: 2026-10-31

## 1. 전략

| 계층 | 대상 | 도구 | 실행 환경 | 속도 |
| --- | --- | --- | --- | --- |
| L1 단위(JVM) | `core` 인증 규칙·거리·설정·데이터 유효성 | JUnit4, kotlinx-coroutines-test(가상 시간) | Android SDK 불필요 | 초 단위 |
| L2 단위(JVM) | `wear` `CheckInViewModel` 상태 전이 | JUnit4, coroutines-test, `Dispatchers.setMain` | Android SDK 필요(컴파일) | 초 단위 |
| L3 계측 | Room DAO/Repository 영속성 | AndroidJUnitRunner, 인메모리 Room | 에뮬레이터/실기기 | 분 단위 |
| L4 수동(에뮬레이터) | 전체 흐름·권한 UI·시간 초과 | Wear OS 에뮬레이터 위치 시뮬레이션 | 에뮬레이터 | 수동 |
| L5 현장 | 실제 좌표·정확도·반경 조정 | 실기기, 배지 기록의 좌표/정확도/거리 | 경주 현장 | 수동 |

원칙: 규칙의 모든 분기는 L1에서 커버한다. L2~L4는 연결(와이어링)만 확인한다.

## 2. L1 — `core` 단위 테스트

### 2.1 `GeoDistanceTest`
- 같은 점 → 0 m
- 첨성대↔동궁과 월지(약 0.68 km) 등 알려진 거리와 ±1 % 이내
- 위도 1″ ≈ 30.9 m 근사 확인, 대칭성(a→b == b→a)

### 2.2 `CheckInConfigTest`
- 기본값: 50/150/50/30 000/20 000
- `effectiveRadiusMeters`: 범위 내 그대로, 범위 밖 클램프
- 잘못된 값(음수, min > max) → `IllegalArgumentException`

### 2.3 `CheckInServiceTest` (각 결과 상태 1개 이상)
| 케이스 | 준비 | 기대 |
| --- | --- | --- |
| 반경 안·정확도 좋음·신선 | fix = 장소 중심에서 30 m, acc 10, age 0 | `Granted`, 저장소에 1건, 거리 ≈ 30 |
| 반경 가장자리 | 거리 = radius − 1 m / radius + 1 m | `Granted` / `OutOfRange` |
| 이미 획득(선검사) | 저장소에 배지 선삽입, provider 호출 횟수 0 | `AlreadyEarned`, 위치 요청 없음 |
| 저장 충돌 | `award()`가 false 반환하는 저장소 | `AlreadyEarned` |
| 권한 거부 | provider → `PermissionDenied` | `PermissionDenied` |
| 위치 불가 | provider → `Unavailable` | `LocationUnavailable` |
| 낮은 정확도 | acc 51 | `LowAccuracy(51, 50)` |
| 정확도 없음 | acc null | `LowAccuracy(null, 50)` |
| 경계 정확도 | acc 50.0 | `Granted` |
| 오래된 위치 | age 30 001 ms | `StaleLocation` |
| 미래 시각(시계 불일치) | age −1 | `StaleLocation` |
| 시간 초과 | provider 지연 20 001 ms(가상 시간) | `Timeout(20000)`, 20 000 ms 경과 후 반환 |
| 요청 스펙 전달 | provider가 받은 spec | timeout 20 000, maxAge 30 000 |
| 설정 조정 반영 | config(maxAccuracy 80) + acc 60 | `Granted` |
| 반복 인증 | Granted 후 재호출 | `AlreadyEarned`, 저장소 여전히 1건 |

### 2.4 `InMemoryBadgeRepositoryTest`
- `award` 첫 호출 true, 둘째 false, `find`·`observeAll` 반영

### 2.5 `GyeongjuPlacesTest` (데이터 유효성)
- id 고유·형식(`^[a-z0-9-]+$`), 좌표가 경주 범위, 반경 50~150, 이름/설명 비어 있지 않음, 첫 장소 id = `cheomseongdae`

### 2.6 `FakeLocationProviderTest`
- 스크립트한 결과 반환, 호출 기록, 지연 반영

## 3. L2 — `CheckInViewModelTest`
- 초기 상태 `Idle`, `earnedBadge == null`
- `checkIn()` → `Checking` → `Completed(Granted)`; Room Flow 대신 `InMemoryBadgeRepository` 사용 시 `earnedBadge` 갱신
- 진행 중 중복 `checkIn()` 무시
- `onPermissionDenied()` → `Completed(PermissionDenied)`
- `dismissResult()` → `Idle`

## 4. L3 — 계측 테스트 `BadgeDaoTest`
- 인메모리 DB. `insertIfAbsent` 첫 호출 ≥ 0, 둘째 −1; `findByPlaceId`; `observeAll` 정렬
- (선택) 실제 파일 DB로 프로세스 재시작 시 유지 확인은 L4에서 수동 수행

## 5. L4 — 에뮬레이터 수동 시나리오

준비: Wear OS 에뮬레이터(API 34+), Extended controls ▸ Location.

| # | 단계 | 기대 |
| --- | --- | --- |
| M1 | 위치 35.8347, 129.2190 설정 → 앱 실행 → "방문 인증" | 권한 다이얼로그 → 허용 → "배지를 획득했어요!" |
| M2 | 앱 종료(스와이프/강제 종료) 후 재실행 | 획득 상태 유지, 버튼 비활성 |
| M3 | 다시 인증 시도 | "이미 획득한 배지예요" (위치 요청 없이 즉시) |
| M4 | 데이터 삭제 → 권한 거부 선택 | "위치 권한이 필요해요…" |
| M5 | 위치 35.8450, 129.2190(≈1.1 km) | "…NNNm 떨어져 있어요" |
| M6 | 에뮬레이터 위치 전송 중지 후 인증 | 20초 후 "위치를 찾지 못했어요(20초)" |
| M7 | `-PuseFakeLocation=true` 빌드 | 에뮬레이터 위치 설정 없이 M1 성공 |

낮은 정확도·오래된 위치는 에뮬레이터에서 재현이 어려우므로 L1에서만 보장하고 L5에서 관찰한다.

## 6. L5 — 현장 검증

- 장소별 중심/반경 가장자리/실내 각 3회 이상, 결과·정확도·거리·소요 시간 기록(배지 레코드 + 로그).
- GPS 없는 워치 + 휴대폰 연결/비연결 조합 확인.
- 결과로 `CheckInConfig`·장소 반경·좌표를 조정하고 `verified=true`로 전환.

## 7. 실행 명령

```bash
./gradlew :core:test
```

```bash
./gradlew :wear:testDebugUnitTest
```

```bash
./gradlew :wear:connectedDebugAndroidTest
```

## 8. 검증 현황 (2026-10-31, 첫 구현)

| 계층 | 상태 | 비고 |
| --- | --- | --- |
| L1 `core` 단위 테스트 | **통과 — 39개 / 실패 0 / 오류 0** (`./gradlew :core:test --configure-on-demand`, Gradle 9.5.0, JDK 21) | CheckInServiceTest 19, CheckInConfigTest 4, GyeongjuPlacesTest 5, GeoDistanceTest 5, FakeLocationProviderTest 3, InMemoryBadgeRepositoryTest 3 |
| `wear` 컴파일 | **미확인** | 작업 환경에서 `dl.google.com` 차단으로 AGP/AndroidX/Play services 아티팩트 수신 불가. Kotlin 2.4.21 컴파일러로 Android 클래스패스 없이 구문 검사만 수행(구문 오류 없음, 타입 검사는 불가) |
| L2 ViewModel 단위 테스트 | **미실행** | 작성 완료(`CheckInViewModelTest`, 6개). `wear` 컴파일 불가로 실행 못 함 |
| L3 계측 테스트 | **미실행** | 작성 완료(`BadgeDaoTest`, 3개). 에뮬레이터 없음 |
| L4 수동 시나리오 | **미실행** | 에뮬레이터 없음 |
| L5 현장 | **미실행** | |

재검증 방법: Android SDK가 있는 환경에서 `./gradlew :wear:assembleDebug :wear:testDebugUnitTest` → 에뮬레이터에서 `:wear:connectedDebugAndroidTest` 와 §5 시나리오.
