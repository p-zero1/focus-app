package com.focusapp.domain.model

/** Stable string identifiers for all predefined badges. */
enum class BadgeId(val displayName: String, val description: String) {
    FIRST_FOCUS("First Focus", "Complete your very first focus session"),
    DEEP_DIVER("Deep Diver", "Complete a single focus session of 2 hours or more"),
    WEEK_WARRIOR("Week Warrior", "Maintain a 7-day consecutive streak"),
    DISTRACTION_FREE("Distraction-Free", "Complete a session with zero distractions"),
    CENTURY("Century", "Complete 100 focus sessions total"),
}
