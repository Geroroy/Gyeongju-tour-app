# 데이터 스키마

작성일: 2026-10-31

## 1. 개요

| 데이터 | 저장 위치 | 변경 주체 | 비고 |
| --- | --- | --- | --- |
| 장소(Place) | 앱 내장 Kotlin 상수(`core` 모듈 `GyeongjuPlaces`) | 개발자(앱 업데이트) | 읽기 전용 |
| 획득 배지(EarnedBadge) | Room DB `gyeongju-badges.db`, 테이블 `earned_badges` | 앱(인증 성공 시) | 기기 로컬, 백업 제외 권장 |
| 인증 설정(CheckInConfig) | 코드 기본값 | 개발자 | 현장 검증 후 조정 |

서버·동기화가 없으므로 사용자 식별자는 없다.

## 2. 도메인 모델 (`core` 모듈, 순수 Kotlin)

### 2.1 `Place`

| 필드 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `String` | 고유, 소문자-kebab, 변경 금지 | 배지 기록의 외래키 역할. 예: `cheomseongdae` |
| `name` | `String` | 비어 있지 않음 | 표시 이름(한국어) |
| `category` | `PlaceCategory` | `LANDMARK` \| `FOOD` | 명소/맛집 |
| `latitude` | `Double` | 35.6 ~ 36.1 (경주 범위) | WGS84 |
| `longitude` | `Double` | 128.9 ~ 129.6 | WGS84 |
| `radiusMeters` | `Double` | 50 ~ 150 | 인증 반경([check-in-rules.md](check-in-rules.md) §2) |
| `description` | `String` | 1~2문장 | 워치 화면용 짧은 설명 |
| `verified` | `Boolean` | 기본 `false` | 좌표·반경 현장 검증 완료 여부 |

- `id`는 한 번 배포되면 바꾸지 않는다(배지 기록과 연결).
- 좌표·반경은 현장 검증 전까지 임시값이며 `verified=false`로 둔다.

### 2.2 `EarnedBadge`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `placeId` | `String` | `Place.id` |
| `earnedAtEpochMillis` | `Long` | 획득 시각(벽시계) |
| `latitude`, `longitude` | `Double` | 판정에 사용한 위치 |
| `accuracyMeters` | `Double?` | 판정 시 정확도 |
| `distanceMeters` | `Double` | 장소 중심까지 거리 |

좌표·정확도·거리는 **현장 검증과 임계값 조정**을 위해 저장한다. 외부 전송은 없다.

### 2.3 `CheckInConfig`

[check-in-rules.md](check-in-rules.md) §2의 5개 값. 기본값 `CheckInConfig.Default`.

## 3. Room 스키마 (`wear` 모듈)

- DB 이름: `gyeongju-badges.db`, 버전 `1`, `exportSchema = true` (`wear/schemas/`에 JSON 출력, Room Gradle Plugin `schemaDirectory`).
- Room 2.8.x, KSP 컴파일러.

### 3.1 테이블 `earned_badges`

| 컬럼 | SQL 타입 | 널 | 제약 | 매핑 |
| --- | --- | --- | --- | --- |
| `place_id` | TEXT | NOT NULL | **PRIMARY KEY** | `EarnedBadge.placeId` |
| `earned_at` | INTEGER | NOT NULL | | `earnedAtEpochMillis` |
| `latitude` | REAL | NOT NULL | | |
| `longitude` | REAL | NOT NULL | | |
| `accuracy_m` | REAL | NULL | | `accuracyMeters` |
| `distance_m` | REAL | NOT NULL | | `distanceMeters` |

- 기본키가 `place_id`이므로 **장소별 1회 지급**이 DB 수준에서 보장된다.
- 삽입은 `OnConflictStrategy.IGNORE`로 수행하고 반환값 `-1`을 "이미 존재"로 해석한다 → `BadgeRepository.award()`가 `false` 반환.

### 3.2 DAO 계약

```kotlin
@Dao interface BadgeDao {
    @Query("SELECT * FROM earned_badges WHERE place_id = :placeId LIMIT 1")
    suspend fun findByPlaceId(placeId: String): BadgeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: BadgeEntity): Long   // -1 이면 이미 존재

    @Query("SELECT * FROM earned_badges ORDER BY earned_at DESC")
    fun observeAll(): Flow<List<BadgeEntity>>
}
```

### 3.3 저장소 인터페이스 (`core`)

```kotlin
interface BadgeRepository {
    suspend fun find(placeId: String): EarnedBadge?
    suspend fun award(badge: EarnedBadge): Boolean   // true = 신규 저장, false = 이미 존재
    fun observeAll(): Flow<List<EarnedBadge>>
}
```

구현: `RoomBadgeRepository`(wear), `InMemoryBadgeRepository`(core, 테스트·프리뷰용).

## 4. 마이그레이션 정책

- 스키마 변경 시 버전을 올리고 `Migration` 또는 `AutoMigration`을 제공한다. `fallbackToDestructiveMigration`은 사용하지 않는다(배지 기록 유실 금지).
- 장소 `id` 변경이 불가피하면 Room 마이그레이션에서 `place_id`를 함께 갱신한다.
- 장소가 데이터에서 삭제되어도 배지 기록은 유지하고, UI에서 "종료된 장소"로 표시한다(다음 단계).

## 5. 초기 장소 데이터 (임시)

| id | 이름 | 분류 | 위도 | 경도 | 반경 | 검증 |
| --- | --- | --- | --- | --- | --- | --- |
| `cheomseongdae` | 첨성대 | LANDMARK | 35.8347 | 129.2190 | 100 | ✗ |
| `bulguksa` | 불국사 | LANDMARK | 35.7900 | 129.3320 | 150 | ✗ |
| `donggung-wolji` | 동궁과 월지 | LANDMARK | 35.8349 | 129.2265 | 150 | ✗ |
| `hwangnidan-gil` | 황리단길 | FOOD | 35.8370 | 129.2110 | 150 | ✗ |

첫 구현 UI는 첫 항목(첨성대)만 표시한다. 좌표는 공개 지도 기준 근사값이며 **현장 검증 필요**.
