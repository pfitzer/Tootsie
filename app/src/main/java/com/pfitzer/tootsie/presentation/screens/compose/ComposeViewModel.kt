package com.pfitzer.tootsie.presentation.screens.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Compose Screen.
 * Manages post composition and submission.
 */
@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val postStatusUseCase: com.pfitzer.tootsie.domain.usecase.status.PostStatusUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val replyToId: String? = savedStateHandle["replyToId"]

    private val _uiState = MutableStateFlow(ComposeUiState(replyToId = replyToId))
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    /**
     * Handle user actions.
     */
    fun onAction(action: ComposeAction) {
        when (action) {
            is ComposeAction.UpdateContent -> updateContent(action.content)
            is ComposeAction.UpdateVisibility -> updateVisibility(action.visibility)
            is ComposeAction.UpdateContentWarning -> updateContentWarning(action.warning)
            is ComposeAction.ToggleContentWarning -> toggleContentWarning()
            is ComposeAction.Post -> postStatus()
            is ComposeAction.DismissError -> dismissError()
        }
    }

    /**
     * Update post content.
     */
    private fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    /**
     * Update post visibility.
     */
    private fun updateVisibility(visibility: StatusVisibility) {
        _uiState.update { it.copy(visibility = visibility) }
    }

    /**
     * Update content warning text.
     */
    private fun updateContentWarning(warning: String) {
        _uiState.update { it.copy(contentWarning = warning) }
    }

    /**
     * Toggle content warning field visibility.
     */
    private fun toggleContentWarning() {
        _uiState.update {
            it.copy(
                showContentWarning = !it.showContentWarning,
                contentWarning = if (!it.showContentWarning) it.contentWarning else ""
            )
        }
    }

    /**
     * Post the status.
     */
    private fun postStatus() {
        val content = _uiState.value.content.trim()

        if (content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Post cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, errorMessage = null) }

            val result = postStatusUseCase(
                content = content,
                visibility = _uiState.value.visibility.value,
                inReplyToId = replyToId,
                spoilerText = _uiState.value.contentWarning.takeIf { it.isNotBlank() }
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPosting = false,
                            isPosted = true
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isPosting = false,
                            errorMessage = "Failed to post: ${result.message}"
                        )
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
 * UI state for Compose Screen.
 */
data class ComposeUiState(
    val content: String = "",
    val visibility: StatusVisibility = StatusVisibility.PUBLIC,
    val contentWarning: String = "",
    val showContentWarning: Boolean = false,
    val isPosting: Boolean = false,
    val isPosted: Boolean = false,
    val errorMessage: String? = null,
    val replyToId: String? = null
) {
    val characterCount: Int get() = content.length
    val canPost: Boolean get() = content.isNotBlank() && !isPosting && characterCount <= 500
    val isReply: Boolean get() = replyToId != null
}

/**
 * User actions for Compose Screen.
 */
sealed class ComposeAction {
    data class UpdateContent(val content: String) : ComposeAction()
    data class UpdateVisibility(val visibility: StatusVisibility) : ComposeAction()
    data class UpdateContentWarning(val warning: String) : ComposeAction()
    data object ToggleContentWarning : ComposeAction()
    data object Post : ComposeAction()
    data object DismissError : ComposeAction()
}

/**
 * Status visibility levels.
 */
enum class StatusVisibility(val value: String, val displayName: String) {
    PUBLIC("public", "Public"),
    UNLISTED("unlisted", "Unlisted"),
    PRIVATE("private", "Followers only"),
    DIRECT("direct", "Direct")
}
