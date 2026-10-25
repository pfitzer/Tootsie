package com.pfitzer.tootsie.presentation.screens.scheduled

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.ScheduledStatus
import com.pfitzer.tootsie.domain.repository.ScheduledStatusRepository
import com.pfitzer.tootsie.domain.util.ScheduleTime
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for Scheduled Posts Screen.
 * Lists the scheduled posts; they can be moved to another time or deleted.
 */
@HiltViewModel
class ScheduledPostsViewModel @Inject constructor(
    private val scheduledStatusRepository: ScheduledStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduledPostsUiState())
    val uiState: StateFlow<ScheduledPostsUiState> = _uiState.asStateFlow()

    /**
     * Handle user actions.
     */
    fun onAction(action: ScheduledPostsAction) {
        when (action) {
            is ScheduledPostsAction.Load -> load()
            is ScheduledPostsAction.Reschedule -> reschedule(action.id, action.scheduledAt)
            is ScheduledPostsAction.Delete -> delete(action.id)
            is ScheduledPostsAction.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    /**
     * Load the scheduled posts, soonest first. Called whenever the screen is shown, so
     * posts edited or published meanwhile are up to date.
     */
    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = scheduledStatusRepository.getScheduledStatuses()) {
                is Result.Success -> {
                    _uiState.update { it.copy(scheduledPosts = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = UiText.of(R.string.error_load_scheduled_posts))
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Move a scheduled post to another time.
     */
    private fun reschedule(id: String, scheduledAt: Instant) {
        if (!ScheduleTime.isValid(scheduledAt, Instant.now())) {
            _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_schedule_too_soon)) }
            return
        }
        processing(id) {
            when (val result = scheduledStatusRepository.reschedule(id, scheduledAt)) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(
                        scheduledPosts = state.scheduledPosts
                            .map { if (it.id == id) result.data else it }
                            .sortedBy { it.scheduledAt }
                    )
                }
                is Result.Error -> _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_reschedule_post)) }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Delete a scheduled post, so it is not published.
     */
    private fun delete(id: String) {
        processing(id) {
            when (scheduledStatusRepository.deleteScheduledStatus(id)) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(scheduledPosts = state.scheduledPosts.filter { it.id != id })
                }
                is Result.Error -> _uiState.update {
                    it.copy(errorMessage = UiText.of(R.string.error_delete_scheduled_post))
                }
                is Result.Loading -> Unit
            }
        }
    }

    /** Runs [block] for a post, ignoring further actions on it until it is done. */
    private fun processing(id: String, block: suspend () -> Unit) {
        if (id in _uiState.value.processingIds) return
        _uiState.update { it.copy(processingIds = it.processingIds + id) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _uiState.update { it.copy(processingIds = it.processingIds - id) }
            }
        }
    }
}

/**
 * UI state for Scheduled Posts Screen.
 */
data class ScheduledPostsUiState(
    val scheduledPosts: List<ScheduledStatus> = emptyList(),
    val isLoading: Boolean = false,
    val processingIds: Set<String> = emptySet(),
    val errorMessage: UiText? = null
)

/**
 * User actions for Scheduled Posts Screen.
 */
sealed class ScheduledPostsAction {
    data object Load : ScheduledPostsAction()
    data class Reschedule(val id: String, val scheduledAt: Instant) : ScheduledPostsAction()
    data class Delete(val id: String) : ScheduledPostsAction()
    data object DismissError : ScheduledPostsAction()
}
