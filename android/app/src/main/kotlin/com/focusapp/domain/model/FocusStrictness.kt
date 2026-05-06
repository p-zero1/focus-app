package com.focusapp.domain.model

enum class FocusStrictness {
    RELAXED,   // Log distraction only
    STRICT,    // Auto-pause session on distraction
    HARDCORE;  // End session on first distraction
}
