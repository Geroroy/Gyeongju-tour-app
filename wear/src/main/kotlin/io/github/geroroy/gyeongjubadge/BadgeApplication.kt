package io.github.geroroy.gyeongjubadge

import android.app.Application
import io.github.geroroy.gyeongjubadge.di.AppContainer

class BadgeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, useFakeLocation = BuildConfig.USE_FAKE_LOCATION)
    }
}
