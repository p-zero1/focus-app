package com.focusapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "distraction_events",
    indices = [Index(value = ["session_id"])],
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class DistractionEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /** DistractionType enum name: APP_SWITCH | SCREEN_UNLOCK */
    @ColumnInfo(name = "type")
    val type: String,

    /** Package name when type == APP_SWITCH; null for SCREEN_UNLOCK */
    @ColumnInfo(name = "app_package_name")
    val appPackageName: String?,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,
)
