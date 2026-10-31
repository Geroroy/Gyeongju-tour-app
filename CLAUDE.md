# CLAUDE.md — GyeongjuBadge (경주 배지 수집 Wear OS 앱)

## 프로젝트 한눈에

- 경주 명소·맛집을 방문하면 배지를 주는 **워치 단독 Wear OS 앱**. Kotlin + Compose for Wear OS(Material 3) + Room.
- 서버·회원·휴대폰 앱·결제·랭킹·상시 위치 추적은 **없다**(v0.1 비범위). 제안하지 말 것.
- 문서가 사양의 원본이다. 코드와 문서가 어긋나면 문서를 먼저 고치거나 문서에 맞춰 코드를 고친다.
  - `docs/product-spec.md` 제품 범위·화면·문구
  - `docs/check-in-rules.md` 인증 규칙·임계값·상태 정의
  - `docs/data-schema.md` 도메인 모델·Room 스키마
  - `docs/architecture.md` 모듈·계층·버전 매트릭스
  - `docs/test-plan.md` 테스트 계층·시나리오·검증 현황

## 모듈

- `core/` — 순수 Kotlin JVM. 도메인 모델, `CheckInService`(인증 규칙), `GyeongjuPlaces`, `FakeLocationProvider`, `InMemoryBadgeRepository`. **Android 의존성 금지.**
- `wear/` — Wear OS 앱. Compose UI, Room, `FusedLocationProvider`, `AppContainer`(수동 DI).
- 의존 방향은 `wear → core`만 허용.

## 불변 규칙

1. 인증 로직은 `core`에만 둔다. UI/Android API에서 거리·정확도·시간 판정을 하지 않는다.
2. 임계값(반경 50~150 m, 정확도 ≤ 50 m, 측정 후 ≤ 30 s, 요청 ≤ 20 s)은 `CheckInConfig`로만 다루고 하드코딩하지 않는다.
3. 배지는 장소별 1회. Room 기본키 `place_id` + `OnConflictStrategy.IGNORE` + `award()`의 Boolean 반환을 유지한다.
4. 위치는 사용자가 버튼을 누를 때 **1회**만 요청한다. `requestLocationUpdates`/백그라운드 위치를 추가하지 않는다.
5. `CheckInResult`의 상태(Granted, AlreadyEarned, PermissionDenied, LocationUnavailable, Timeout, LowAccuracy, StaleLocation, OutOfRange)를 바꾸면 `docs/check-in-rules.md` §4와 UI 문구, 테스트를 함께 갱신한다.
6. 새 `core` 분기는 반드시 L1 단위 테스트를 동반한다(`docs/test-plan.md` §2).
7. Wear UI는 `androidx.wear.compose:compose-material3`만 사용. `androidx.compose.material`, `androidx.compose.material3`, `androidx.wear.compose:compose-material`, 모바일 `navigation-compose`를 추가하지 않는다.

## 빌드·테스트

- 툴체인: JDK 17+, Gradle 9.5.0(wrapper), AGP 9.3.0(built-in Kotlin — `kotlin-android` 플러그인 적용 금지), Kotlin 2.4.21, KSP 2.3.10, Room 2.8.5, Compose BOM 2026.09.00, Wear Compose 1.7.1, compileSdk/targetSdk 37, minSdk 30. 버전은 `gradle/libs.versions.toml`에서만 바꾸고, 바꿀 때 `docs/architecture.md` §7의 근거를 갱신한다.
- 라이브러리 버전 변경·추가 시 공식 문서(developer.android.com, kotlinlang.org)에서 호환성을 확인하고 출처를 §7 표에 적는다.

```bash
./gradlew :core:test                       # Android SDK 없이 실행 가능
./gradlew :wear:testDebugUnitTest          # Android SDK 필요
./gradlew :wear:connectedDebugAndroidTest  # 에뮬레이터/실기기
./gradlew :wear:installDebug -PuseFakeLocation=true   # 가짜 위치(첫 장소 중심)로 흐름 확인
```

- Android SDK가 없는 환경에서는 `:wear`를 구성하지 않도록 `--configure-on-demand`로 `:core` 작업만 실행할 수 있다.

## 작업 보고 규칙

- 구현 후 **실행해서 확인한 것**과 **확인하지 못한 것**을 구분해 보고한다(테스트 출력 포함).
- 에뮬레이터·실기기 검증을 하지 않았다면 "미검증"으로 명시한다. 현장 검증 전 장소 좌표·반경은 임시값이다.

## 코드 스타일

- Kotlin 공식 스타일, 불변 데이터 클래스, `sealed interface`로 상태 표현, 예외 대신 결과 타입.
- 사용자 노출 문자열은 `wear/src/main/res/values/strings.xml`(한국어)에만 둔다.
- 커밋 메시지는 변경 의도를 한 줄로(한국어/영어 무관).
