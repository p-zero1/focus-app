package com.focusapp.domain.model

data class DistractionEvent(
    val id: Long,
    val sessionId: Long,
    val timestamp: Long,
    val type: DistractionType,
    val appPackageName: String?,
    val durationSeconds: Int,
)
