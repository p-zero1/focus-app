package com.focusapp.domain.model

data class FocusSession(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val plannedDuration: Int,
    val actualDuration: Int,
    val mode: SessionMode,
    val tag: String?,
    val status: SessionStatus,
    val distractionCount: Int,
    val distractionTotalSeconds: Int,
    val xpAwarded: Int,
    val focusScore: Int?,
    val focusStrictness: FocusStrictness = FocusStrictness.RELAXED,
    val sessionOutcome: SessionOutcome? = null,
) {
    val isCompleted: Boolean get() = status == SessionStatus.COMPLETED
    val isActive: Boolean get() = status == SessionStatus.ACTIVE
}
