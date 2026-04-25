package com.focusapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.focusapp.data.db.dao.BadgeDao
import com.focusapp.data.db.dao.DistractionEventDao
import com.focusapp.data.db.dao.FocusSessionDao
import com.focusapp.data.db.dao.ReminderDao
import com.focusapp.data.db.dao.UserProfileDao
import com.focusapp.data.db.entity.BadgeEntity
import com.focusapp.data.db.entity.DistractionEventEntity
import com.focusapp.data.db.entity.FocusSessionEntity
import com.focusapp.data.db.entity.ReminderEntity
import com.focusapp.data.db.entity.UserProfileEntity

@Database(
    entities = [
        FocusSessionEntity::class,
        DistractionEventEntity::class,
        UserProfileEntity::class,
        BadgeEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun distractionEventDao(): DistractionEventDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun badgeDao(): BadgeDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "focus_app.db"
    }
}
