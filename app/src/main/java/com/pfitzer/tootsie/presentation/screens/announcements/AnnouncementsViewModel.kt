package com.pfitzer.tootsie.presentation.screens.announcements

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Announcement
import com.pfitzer.tootsie.domain.usecase.announcement.DismissAnnouncementUseCase
import com.pfitzer.tootsie.domain.usecase.announcement.GetAnnouncementsUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Announcements Screen.
 * Manages server announcements with dismiss functionality.
 */
@HiltViewModel
class AnnouncementsViewModel @Inject constructor(
    private val getAnnouncementsUseCase: GetAnnouncementsUseCase,
    private val dismissAnnouncementUseCase: DismissAnnouncementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnnouncementsUiState())
    val uiState: StateFlow<AnnouncementsUiState> = _uiState.asStateFlow()

    init {
        loadAnnouncements()
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: AnnouncementsAction) {
        when (action) {
            is AnnouncementsAction.Refresh -> loadAnnouncements()
            is AnnouncementsAction.DismissAnnouncement -> dismissAnnouncement(action.announcement)
            is AnnouncementsAction.DismissError -> dismissError()
        }
    }

    /**
     * Load announcements from the API.
     */
    private fun loadAnnouncements() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getAnnouncementsUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            announcements = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = UiText.of(R.string.error_load_announcements),
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> {
                    // Already set loading above
                }
            }
        }
    }

    /**
     * Dismiss an announcement (mark as read).
     */
    private fun dismissAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            when (dismissAnnouncementUseCase(announcement.id)) {
                is Result.Success -> {
                    // Update local state to mark as read
                    _uiState.update { state ->
                        state.copy(
                            announcements = state.announcements.map {
                                if (it.id == announcement.id) it.copy(read = true) else it
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = UiText.of(R.string.error_dismiss_announcement))
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Dismiss error message.
     */
    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for Announcements Screen.
 */
data class AnnouncementsUiState(
    val announcements: List<Announcement> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
)

/**
 * User actions for Announcements Screen.
 */
sealed class AnnouncementsAction {
    data object Refresh : AnnouncementsAction()
    data class DismissAnnouncement(val announcement: Announcement) : AnnouncementsAction()
    data object DismissError : AnnouncementsAction()
}
