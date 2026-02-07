package com.pfitzer.tootsie.presentation.screens.followedtags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.domain.usecase.followedtags.FollowTagUseCase
import com.pfitzer.tootsie.domain.usecase.followedtags.GetFollowedTagsUseCase
import com.pfitzer.tootsie.domain.usecase.followedtags.UnfollowTagUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Followed Tags Screen.
 * Manages followed hashtags with follow/unfollow operations.
 */
@HiltViewModel
class FollowedTagsViewModel @Inject constructor(
    private val getFollowedTagsUseCase: GetFollowedTagsUseCase,
    private val followTagUseCase: FollowTagUseCase,
    private val unfollowTagUseCase: UnfollowTagUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowedTagsUiState())
    val uiState: StateFlow<FollowedTagsUiState> = _uiState.asStateFlow()

    init {
        loadFollowedTags(scrollToTopOnSuccess = false)
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: FollowedTagsAction) {
        when (action) {
            is FollowedTagsAction.RequestUnfollowTag -> {
                _uiState.update { it.copy(showUnfollowConfirmation = action.tag) }
            }
            is FollowedTagsAction.UnfollowTag -> {
                _uiState.update { it.copy(showUnfollowConfirmation = null) }
                unfollowTag(action.tag)
            }
            is FollowedTagsAction.DismissUnfollowConfirmation -> {
                _uiState.update { it.copy(showUnfollowConfirmation = null) }
            }
            is FollowedTagsAction.DismissError -> dismissError()
            is FollowedTagsAction.Refresh -> loadFollowedTags(scrollToTopOnSuccess = true)
            is FollowedTagsAction.ScrollToTopConsumed -> {
                _uiState.update { it.copy(scrollToTop = false) }
            }
            is FollowedTagsAction.ShowFollowDialog -> {
                _uiState.update { it.copy(showFollowDialog = true) }
            }
            is FollowedTagsAction.DismissFollowDialog -> {
                _uiState.update { it.copy(showFollowDialog = false) }
            }
            is FollowedTagsAction.FollowTag -> {
                _uiState.update { it.copy(showFollowDialog = false) }
                followTag(action.tagName)
            }
        }
    }

    /**
     * Follow a hashtag.
     */
    private fun followTag(tagName: String) {
        val cleanTagName = tagName.removePrefix("#").trim()
        if (cleanTagName.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = followTagUseCase(cleanTagName)) {
                is Result.Success -> {
                    // Refresh the list to include the new tag
                    loadFollowedTags(scrollToTopOnSuccess = true)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to follow #$cleanTagName",
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Load followed tags from the API.
     */
    private fun loadFollowedTags(scrollToTopOnSuccess: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getFollowedTagsUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            followedTags = result.data,
                            isLoading = false,
                            scrollToTop = scrollToTopOnSuccess
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to load followed tags",
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
     * Unfollow a hashtag.
     */
    private fun unfollowTag(tag: FollowedTag) {
        viewModelScope.launch {
            when (unfollowTagUseCase(tag.name)) {
                is Result.Success -> {
                    // Remove from local list
                    _uiState.update { state ->
                        state.copy(
                            followedTags = state.followedTags.filterNot { it.name == tag.name }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unfollow #${tag.name}")
                    }
                }
                is Result.Loading -> {
                    // Loading state
                }
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
 * UI state for Followed Tags Screen.
 */
data class FollowedTagsUiState(
    val followedTags: List<FollowedTag> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showUnfollowConfirmation: FollowedTag? = null,
    val showFollowDialog: Boolean = false,
    val scrollToTop: Boolean = false
)

/**
 * User actions for Followed Tags Screen.
 */
sealed class FollowedTagsAction {
    data class RequestUnfollowTag(val tag: FollowedTag) : FollowedTagsAction()
    data class UnfollowTag(val tag: FollowedTag) : FollowedTagsAction()
    data object DismissUnfollowConfirmation : FollowedTagsAction()
    data object DismissError : FollowedTagsAction()
    data object Refresh : FollowedTagsAction()
    data object ShowFollowDialog : FollowedTagsAction()
    data object DismissFollowDialog : FollowedTagsAction()
    data class FollowTag(val tagName: String) : FollowedTagsAction()
    data object ScrollToTopConsumed : FollowedTagsAction()
}
