# 아키텍처

작성일: 2026-10-31

## 1. 모듈 구성

```
GyeongjuBadge/
├── core/   Kotlin JVM 라이브러리 — 도메인 모델, 인증 규칙, 장소 데이터, 테스트용 위치 제공자 (Android 의존성 없음)
└── wear/   Wear OS 앱 — Compose for Wear OS UI, Room, FusedLocationProvider 어댑터, DI 컨테이너
```

| 모듈 | 의존 | 테스트 |
| --- | --- | --- |
| `core` | kotlinx-coroutines-core | JVM 단위 테스트(JUnit4 + coroutines-test). **Android SDK 없이 실행 가능** |
| `wear` | `core`, Compose/Wear Compose, Room, play-services-location, lifecycle | JVM 단위 테스트(ViewModel), 계측 테스트(Room DAO) |

인증 로직을 `core`에 두는 이유: (1) 요구사항 "화면·실제 위치 API와 분리", (2) 에뮬레이터 없이 규칙을 빠르게 검증, (3) 향후 휴대폰 앱이 생겨도 재사용.

## 2. 계층과 의존 방향

```
 wear/ui (Compose)  →  wear/ui/checkin/CheckInViewModel  →  core/checkin/CheckInService
                                                              ├─ core/location/LocationProvider  ←  wear/location/FusedLocationProvider
                                                              │                                  ←  core/location/FakeLocationProvider
                                                              ├─ core/repository/BadgeRepository ←  wear/data/db/RoomBadgeRepository
                                                              │                                  ←  core/repository/InMemoryBadgeRepository
                                                              ├─ core/time/Clock                 ←  wear/location/AndroidClock
                                                              └─ core/checkin/CheckInConfig
```

의존은 항상 `wear → core` 한 방향이다. `core`는 인터페이스를 정의하고 `wear`가 Android 구현을 주입한다.

## 3. 핵심 컴포넌트

### 3.1 `core`

| 패키지 | 타입 | 역할 |
| --- | --- | --- |
| `model` | `Place`, `PlaceCategory`, `EarnedBadge` | 도메인 모델 |
| `data` | `GyeongjuPlaces` | 내장 장소 데이터 |
| `geo` | `GeoDistance` | 하버사인 거리 |
| `time` | `Clock`, `SystemClock` | 단조 시계·벽시계 추상화 |
| `location` | `LocationProvider`, `LocationRequestSpec`, `LocationFix`, `LocationFixResult`, `FakeLocationProvider` | 위치 추상화와 테스트 구현 |
| `repository` | `BadgeRepository`, `InMemoryBadgeRepository` | 배지 저장 추상화 |
| `checkin` | `CheckInConfig`, `CheckInResult`, `CheckInService` | 인증 규칙([check-in-rules.md](check-in-rules.md)) |

### 3.2 `wear`

| 패키지 | 타입 | 역할 |
| --- | --- | --- |
| (root) | `BadgeApplication` | 앱 진입점, `AppContainer` 보유 |
| `di` | `AppContainer` | 수동 DI. Room DB, 위치 제공자(실제/가짜), `CheckInService` 조립 |
| `data.db` | `BadgeEntity`, `BadgeDao`, `BadgeDatabase`, `RoomBadgeRepository` | Room 영속화 |
| `location` | `FusedLocationProvider`, `AndroidClock` | FLP 어댑터, `SystemClock.elapsedRealtime` |
| `ui` | `MainActivity` | `AppScaffold` + 단일 화면 |
| `ui.checkin` | `CheckInViewModel`, `CheckInUiState`, `PlaceCheckInScreen` | 상태 보유·권한 흐름·화면 |

### 3.3 상태 흐름 (단방향)

```
UI 이벤트(버튼) → ViewModel.checkIn() → CheckInService.checkIn(place) → CheckInResult
                 ↘ 권한 없음 → ActivityResult 권한 요청 → 허용: checkIn() / 거부: ViewModel.onPermissionDenied()
Room Flow(observeAll) → ViewModel(StateFlow<CheckInUiState>) → collectAsStateWithLifecycle → UI
```

- `CheckInUiState(place, earnedBadge, phase)` / `phase ∈ {Idle, Checking, Completed(result)}`.
- 획득 여부는 Room `Flow`를 구독해 반영하므로 재실행 시 자동으로 "획득" 상태가 된다.

## 4. 위치 처리 설계

- `FusedLocationProvider.currentFix()`:
  1. `ContextCompat.checkSelfPermission(ACCESS_FINE_LOCATION)` 미허용 → `PermissionDenied`
  2. `CurrentLocationRequest(priority=HIGH_ACCURACY, durationMillis=timeout, maxUpdateAgeMillis=maxAge)`
  3. `getCurrentLocation(request, token).await(cts)` — 코루틴 취소 시 FLP 요청도 취소
  4. `null` → `Unavailable`, 예외 → `Unavailable(message)`, `SecurityException` → `PermissionDenied`
  5. `Location.elapsedRealtimeNanos/1e6`를 `LocationFix.elapsedRealtimeMillis`로, `hasAccuracy()`일 때만 정확도 전달
- 앱은 GPS 유무(`FEATURE_LOCATION_GPS`)를 요구하지 않는다. FLP가 휴대폰 위치를 쓸 수 있고, 정확도 임계값이 품질을 거른다. 실측 후 안내 문구 추가 여부를 결정한다.
- 테스트 빌드 스위치: `./gradlew :wear:installDebug -PuseFakeLocation=true` → `BuildConfig.USE_FAKE_LOCATION=true` → `AppContainer`가 `FakeLocationProvider`(첫 장소 중심, 정확도 10 m)를 주입.

## 5. 저장 설계

Room 2.8.5, 단일 테이블([data-schema.md](data-schema.md)). DB는 `AppContainer`에서 lazy 싱글턴. 스키마 JSON은 `wear/schemas/`에 출력(Room Gradle Plugin).

## 6. 빌드 구성

- Gradle Kotlin DSL + 버전 카탈로그(`gradle/libs.versions.toml`).
- AGP 9.x **built-in Kotlin**: `wear`에 `org.jetbrains.kotlin.android`를 적용하지 않는다. Compose 컴파일러는 `org.jetbrains.kotlin.plugin.compose`(Kotlin과 동일 버전).
- `core`는 `org.jetbrains.kotlin.jvm`, JVM 타깃 17(= `wear`의 `compileOptions` 17, 변형 속성 일치).
- 루트 `build.gradle.kts`는 Kotlin JVM / Compose / KSP 플러그인만 `apply false`로 선언해 KGP를 루트 클래스패스 한 곳에 둔다. AGP와 Room 플러그인은 `wear`에서만 적용한다(이 덕분에 Android SDK가 없는 환경에서도 `:core:test`를 돌릴 수 있다).
- 저장소: `google()`은 `com.android.*`, `com.google.*`, `androidx.*` 그룹으로 제한(Android Studio 템플릿과 동일), 그 외는 `mavenCentral()`/`gradlePluginPortal()`.

## 7. 버전 매트릭스와 호환성 확인

2026-10-31 기준, 모두 **공식 문서/공식 저장소 메타데이터**로 확인.

| 구성요소 | 버전 | 확인 출처 | 확인 내용 |
| --- | --- | --- | --- |
| JDK | 17 이상 | [AGP release notes](https://developer.android.com/build/releases/gradle-plugin) | AGP 9.x 최소 JDK 17 |
| Gradle | 9.5.0 (wrapper) | [AGP 9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [KGP 호환표](https://kotlinlang.org/docs/gradle-configure-project.html) | AGP 9.3 최소/기본 Gradle 9.5.0; KGP 2.4.21 지원 범위 7.6.3–9.7.0 |
| AGP | 9.3.0 | 위 AGP 9.3 release notes, KGP 호환표 | 최대 API 37 지원; KGP 2.4.21의 완전 지원 AGP 범위 8.5.2–9.3.1 안. (최신 9.5.0은 KGP 호환표 범위 밖이라 보류) |
| Kotlin / KGP | 2.4.21 | [Kotlin releases](https://kotlinlang.org/docs/releases.html) | 2026-10-08 안정 버전 |
| Compose Compiler Gradle Plugin | 2.4.21 | [Compose compiler](https://developer.android.com/develop/ui/compose/compiler) | Kotlin 버전과 반드시 일치 |
| KSP | 2.3.10 | [KSP quickstart](https://kotlinlang.org/docs/ksp-quickstart.html) | 문서 예제가 Kotlin 2.4.21 + KSP 2.3.10 조합 사용. AGP 9 built-in Kotlin은 KSP ≥ 2.2.10-2.0.2 요구 |
| Room (+ Gradle Plugin) | 2.8.5 | [Room releases](https://developer.android.com/jetpack/androidx/releases/room) | 2026-09-09 안정; Kotlin 2.0+, KSP2 권장, minSdk 23, Room Gradle Plugin은 AGP 8.4+ |
| Compose BOM | 2026.09.00 (ui 1.12.1) | [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) | Compose 1.12.0+는 compileSdk 37·AGP 9 필수 |
| Wear Compose (foundation/material3/ui-tooling) | 1.7.1 | [Wear Compose releases](https://developer.android.com/jetpack/androidx/releases/wear-compose) | 2026-10-07 안정; Material 3 권장, `compose-material`·`androidx.compose.material*` 혼용 금지 |
| activity-compose | 1.13.0 | [Compose for Wear OS setup](https://developer.android.com/training/wearables/compose) | 설정 가이드 스니펫 |
| lifecycle (viewmodel-compose, runtime-compose) | 2.11.0 | [Lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle) | 2026-06-17 안정 |
| core-ktx | 1.19.1 | [Core releases](https://developer.android.com/jetpack/androidx/releases/core) | 2026-09-23 안정 |
| play-services-location | 21.4.0 | Google Play services release notes(검색 결과 인용) | **직접 열람 실패**(`developers.google.com` 차단). 빌드 시 재확인 필요 |
| kotlinx-coroutines (core/android/play-services/test) | 1.11.0 | Maven Central 메타데이터 | 최신 release |
| JUnit | 4.13.2 | Maven Central 메타데이터 | |
| androidx.test (ext-junit / runner) | 1.3.0 / 1.7.0 | [Test releases](https://developer.android.com/jetpack/androidx/releases/test) | |
| compileSdk / targetSdk / minSdk | 37 / 37 / 30 | [Wear OS 7 setup](https://developer.android.com/training/wearables/versions/7/setup) | Wear OS 7 = API 37; minSdk 30 = Wear OS 3 |

### 7.1 확인하지 못한 항목 (작업 환경 제약)

- `dl.google.com`(Android SDK, Google Maven 실제 아티팩트 호스트)이 네트워크 정책으로 차단되어 **AGP·AndroidX·Play services 아티팩트를 내려받을 수 없었다.** 따라서 `wear` 모듈은 이 환경에서 컴파일하지 못했다. 위 버전들은 문서상 존재·호환이 확인된 값이다.
- `play-services-location 21.4.0`은 공식 release notes를 직접 열지 못했고 검색 결과로만 확인했다.
- Room 3.0.3(`androidx.room3`, 2026-09-09 안정)이 존재하지만, 2.8.5가 여전히 안정 지원 중이고 API가 널리 검증되어 2.8.5를 택했다. 멀티플랫폼 필요 시 Room 3 이관을 검토한다.

## 8. 확장 포인트 (다음 단계)

- 장소 목록/주변 정렬: `PlaceRepository` + 현재 위치 1회 측정(동일 `LocationProvider`).
- 배지 목록 화면: `BadgeRepository.observeAll()` 이미 제공.
- 설정값 외부화: `CheckInConfig`를 DataStore에서 로드.
- 내비게이션: `androidx.wear.compose:compose-navigation` 추가(모바일 navigation-compose 사용 금지).
