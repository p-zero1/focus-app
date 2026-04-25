package com.focusapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.TagAggregate
import com.focusapp.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<FocusSession> = emptyList(),
    val tagAggregates: List<TagAggregate> = emptyList(),
    val selectedTag: String? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _selectedTag = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeSessions()
        observeTagAggregates()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSessions() {
        _selectedTag
            .flatMapLatest { tag ->
                if (tag == null) {
                    sessionRepository.getAllSessions()
                } else {
                    sessionRepository.getSessionsByTag(tag)
                }
            }
            .onEach { sessions ->
                _uiState.value = _uiState.value.copy(
                    sessions = sessions,
                    selectedTag = _selectedTag.value,
                    isLoading = false,
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeTagAggregates() {
        sessionRepository.getTagAggregates()
            .onEach { aggregates ->
                _uiState.value = _uiState.value.copy(tagAggregates = aggregates)
            }
            .launchIn(viewModelScope)
    }

    fun onTagSelected(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }
}
