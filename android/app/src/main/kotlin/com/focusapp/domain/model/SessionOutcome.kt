package com.focusapp.domain.model

enum class SessionOutcome {
    CLEAN,        // Zero distractions
    INTERRUPTED,  // 1–3 distractions
    FAILED;       // >3 distractions or session forcibly ended (HARDCORE)
}
