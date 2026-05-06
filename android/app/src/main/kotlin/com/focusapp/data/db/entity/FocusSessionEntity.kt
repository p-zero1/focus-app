package com.focusapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["start_time"]), Index(value = ["tag"])]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    /** Null while session is ACTIVE */
    @ColumnInfo(name = "end_time")
    val endTime: Long?,

    @ColumnInfo(name = "planned_duration")
    val plannedDuration: Int,

    @ColumnInfo(name = "actual_duration")
    val actualDuration: Int,

    /** SessionMode enum name: POMODORO | CUSTOM | STUDY | DEEP_WORK */
    @ColumnInfo(name = "mode")
    val mode: String,

    /** Free-form label e.g. "DSA", "Project X" */
    @ColumnInfo(name = "tag")
    val tag: String?,

    /** SessionStatus enum name: ACTIVE | COMPLETED | PARTIAL */
    @ColumnInfo(name = "status")
    val status: String,

    /** Denormalized from DistractionEvent for fast queries */
    @ColumnInfo(name = "distraction_count")
    val distractionCount: Int = 0,

    @ColumnInfo(name = "distraction_total_seconds")
    val distractionTotalSeconds: Int = 0,

    @ColumnInfo(name = "xp_awarded")
    val xpAwarded: Int = 0,

    /** Computed at session end: 0–100 */
    @ColumnInfo(name = "focus_score")
    val focusScore: Int?,

    /** FocusStrictness enum name: RELAXED | STRICT | HARDCORE */
    @ColumnInfo(name = "focus_strictness", defaultValue = "RELAXED")
    val focusStrictness: String = "RELAXED",

    /** SessionOutcome enum name: CLEAN | INTERRUPTED | FAILED — null until session completes */
    @ColumnInfo(name = "session_outcome")
    val sessionOutcome: String? = null,
)
