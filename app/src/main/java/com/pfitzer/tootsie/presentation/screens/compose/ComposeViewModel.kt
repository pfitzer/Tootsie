package com.pfitzer.tootsie.presentation.screens.compose

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Compose Screen.
 * Manages post composition and submission.
 */
@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val postStatusUseCase: com.pfitzer.tootsie.domain.usecase.status.PostStatusUseCase,
    private val uploadMediaUseCase: com.pfitzer.tootsie.domain.usecase.status.UploadMediaUseCase,
    private val statusRepository: com.pfitzer.tootsie.domain.repository.StatusRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val replyToId: String? = savedStateHandle["replyToId"]

    private val _uiState = MutableStateFlow(ComposeUiState(replyToId = replyToId))
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    init {
        // Load reply context if replying to a status
        if (replyToId != null) {
            loadReplyContext(replyToId)
        }
    }

    /**
     * Load the status being replied to, to extract account information.
     */
    private fun loadReplyContext(statusId: String) {
        viewModelScope.launch {
            when (val result = statusRepository.getStatus(statusId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(replyToAccount = result.data.account.acct)
                    }
                }
                is Result.Error -> {
                    // If we can't load the status, we can still reply
                    // Just won't show the account name
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: ComposeAction) {
        when (action) {
            is ComposeAction.UpdateContent -> updateContent(action.content)
            is ComposeAction.UpdateVisibility -> updateVisibility(action.visibility)
            is ComposeAction.UpdateContentWarning -> updateContentWarning(action.warning)
            is ComposeAction.ToggleContentWarning -> toggleContentWarning()
            is ComposeAction.AddMedia -> uploadMedia(action.file)
            is ComposeAction.RemoveMedia -> removeMedia(action.attachmentId)
            is ComposeAction.UpdateMediaDescription -> updateMediaDescription(action.attachmentId, action.description)
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
     * Upload a media file.
     */
    private fun uploadMedia(file: File) {
        // Don't allow more than 4 attachments
        if (_uiState.value.mediaAttachments.size >= 4) {
            _uiState.update { it.copy(errorMessage = "Maximum 4 media attachments allowed") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingMedia = true, errorMessage = null) }

            val result = uploadMediaUseCase(file = file)

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isUploadingMedia = false,
                            mediaAttachments = it.mediaAttachments + result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingMedia = false,
                            errorMessage = "Failed to upload media: ${result.message}"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Remove a media attachment.
     */
    private fun removeMedia(attachmentId: String) {
        _uiState.update {
            it.copy(
                mediaAttachments = it.mediaAttachments.filter { attachment ->
                    attachment.id != attachmentId
                }
            )
        }
    }

    /**
     * Update alt text description for a media attachment.
     */
    private fun updateMediaDescription(attachmentId: String, description: String) {
        _uiState.update {
            it.copy(
                mediaAttachments = it.mediaAttachments.map { attachment ->
                    if (attachment.id == attachmentId) {
                        attachment.copy(description = description)
                    } else {
                        attachment
                    }
                }
            )
        }
    }

    /**
     * Post the status.
     */
    private fun postStatus() {
        val content = _uiState.value.content.trim()
        val hasMedia = _uiState.value.mediaAttachments.isNotEmpty()

        // Allow posting if there's content OR media (can post media-only)
        if (content.isBlank() && !hasMedia) {
            _uiState.update { it.copy(errorMessage = "Post cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, errorMessage = null) }

            val mediaIds = _uiState.value.mediaAttachments.map { it.id }

            val result = postStatusUseCase(
                content = content,
                visibility = _uiState.value.visibility.value,
                inReplyToId = replyToId,
                mediaIds = mediaIds.takeIf { it.isNotEmpty() },
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
    val mediaAttachments: List<MediaAttachment> = emptyList(),
    val isUploadingMedia: Boolean = false,
    val isPosting: Boolean = false,
    val isPosted: Boolean = false,
    val errorMessage: String? = null,
    val replyToId: String? = null,
    val replyToAccount: String? = null
) {
    val characterCount: Int get() = content.length
    val hasMedia: Boolean get() = mediaAttachments.isNotEmpty()
    val canPost: Boolean get() = (content.isNotBlank() || hasMedia) && !isPosting && !isUploadingMedia && characterCount <= 500
    val isReply: Boolean get() = replyToId != null
    val canAddMedia: Boolean get() = mediaAttachments.size < 4 && !isUploadingMedia
}

/**
 * User actions for Compose Screen.
 */
sealed class ComposeAction {
    data class UpdateContent(val content: String) : ComposeAction()
    data class UpdateVisibility(val visibility: StatusVisibility) : ComposeAction()
    data class UpdateContentWarning(val warning: String) : ComposeAction()
    data object ToggleContentWarning : ComposeAction()
    data class AddMedia(val file: File) : ComposeAction()
    data class RemoveMedia(val attachmentId: String) : ComposeAction()
    data class UpdateMediaDescription(val attachmentId: String, val description: String) : ComposeAction()
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
