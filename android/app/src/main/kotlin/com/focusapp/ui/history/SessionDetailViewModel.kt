package com.focusapp.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.domain.model.DistractionEvent
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.repository.DistractionRepository
import com.focusapp.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SessionDetailUiState(
    val session: FocusSession? = null,
    val distractions: List<DistractionEvent> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val distractionRepository: DistractionRepository,
) : ViewModel() {

    // Navigation Compose passes path args as Strings; convert to Long
    private val sessionId: Long = checkNotNull(
        savedStateHandle.get<String>("id")?.toLongOrNull()
    ) { "SessionDetailViewModel requires a numeric session id argument" }

    val uiState: StateFlow<SessionDetailUiState> = combine(
        flow { emit(sessionRepository.getSessionById(sessionId)) },
        distractionRepository.getBySessionId(sessionId),
    ) { session, distractions ->
        SessionDetailUiState(
            session = session,
            distractions = distractions,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionDetailUiState(),
    )
}
