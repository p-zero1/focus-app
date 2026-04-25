package com.focusapp.domain.model

/** Summary of all sessions sharing the same tag. Used in History screen filter chips. */
data class TagAggregate(
    val tag: String,
    val totalMinutes: Int,
    val sessionCount: Int,
)
