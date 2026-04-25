package com.focusapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** ReminderType enum name: HABIT | GOAL | BREAK */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "trigger_time_hour")
    val triggerTimeHour: Int,

    @ColumnInfo(name = "trigger_time_minute")
    val triggerTimeMinute: Int,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    /** Comma-separated day numbers e.g. "1,2,3,4,5" (Mon–Fri) */
    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: String,
)
