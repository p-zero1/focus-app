package com.focusapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusapp.data.db.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    /** Ignores duplicates so callers don't need to pre-check. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(badge: BadgeEntity)

    @Query("SELECT * FROM badges ORDER BY awarded_at ASC")
    fun getAll(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE id = :id")
    suspend fun getById(id: String): BadgeEntity?
}
