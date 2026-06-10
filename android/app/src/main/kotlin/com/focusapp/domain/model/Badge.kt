package com.focusapp.domain.model

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val awardedAt: Long,
) {
    val badgeId: BadgeId? get() = BadgeId.entries.find { it.name == id }
}
