package io.github.geroroy.gyeongjubadge.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Query("SELECT * FROM earned_badges WHERE place_id = :placeId LIMIT 1")
    suspend fun findByPlaceId(placeId: String): BadgeEntity?

    /** 이미 같은 place_id 가 있으면 무시하고 -1 을 반환한다. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: BadgeEntity): Long

    @Query("SELECT * FROM earned_badges ORDER BY earned_at DESC")
    fun observeAll(): Flow<List<BadgeEntity>>
}
