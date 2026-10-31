package io.github.geroroy.gyeongjubadge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BadgeEntity::class], version = 1, exportSchema = true)
abstract class BadgeDatabase : RoomDatabase() {
    abstract fun badgeDao(): BadgeDao

    companion object {
        const val NAME = "gyeongju-badges.db"
    }
}
