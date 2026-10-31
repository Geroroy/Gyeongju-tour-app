package io.github.geroroy.gyeongjubadge.di

import android.content.Context
import androidx.room.Room
import io.github.geroroy.gyeongjubadge.core.checkin.CheckInConfig
import io.github.geroroy.gyeongjubadge.core.checkin.CheckInService
import io.github.geroroy.gyeongjubadge.core.data.GyeongjuPlaces
import io.github.geroroy.gyeongjubadge.core.location.FakeLocationProvider
import io.github.geroroy.gyeongjubadge.core.location.LocationProvider
import io.github.geroroy.gyeongjubadge.core.model.Place
import io.github.geroroy.gyeongjubadge.core.repository.BadgeRepository
import io.github.geroroy.gyeongjubadge.core.time.Clock
import io.github.geroroy.gyeongjubadge.data.db.BadgeDatabase
import io.github.geroroy.gyeongjubadge.data.db.RoomBadgeRepository
import io.github.geroroy.gyeongjubadge.location.AndroidClock
import io.github.geroroy.gyeongjubadge.location.FusedLocationProvider

/**
 * 수동 DI 컨테이너. docs/architecture.md §3.2
 *
 * @param useFakeLocation true 면 첫 장소 중심에 서 있는 것처럼 동작하는 [FakeLocationProvider] 를 주입한다
 *   (`-PuseFakeLocation=true` 빌드). 에뮬레이터 위치 설정 없이 전체 흐름을 확인하는 용도.
 */
class AppContainer(
    context: Context,
    useFakeLocation: Boolean = false,
) {
    private val appContext = context.applicationContext

    /** 첫 구현에서 표시하는 단일 장소. */
    val place: Place = GyeongjuPlaces.first

    val clock: Clock = AndroidClock

    val checkInConfig: CheckInConfig = CheckInConfig.Default

    private val database: BadgeDatabase by lazy {
        Room.databaseBuilder(appContext, BadgeDatabase::class.java, BadgeDatabase.NAME).build()
    }

    val badgeRepository: BadgeRepository by lazy { RoomBadgeRepository(database.badgeDao()) }

    val locationProvider: LocationProvider by lazy {
        if (useFakeLocation) {
            FakeLocationProvider.at(place, clock, accuracyMeters = 10.0, delayMillis = 800L)
        } else {
            FusedLocationProvider(appContext)
        }
    }

    val checkInService: CheckInService by lazy {
        CheckInService(
            repository = badgeRepository,
            locationProvider = locationProvider,
            clock = clock,
            config = checkInConfig,
        )
    }
}
