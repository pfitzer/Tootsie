package com.pfitzer.tootsie.presentation.screens.compose

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val draftRepository: DraftRepository,
    private val getCustomEmojisUseCase: com.pfitzer.tootsie.domain.usecase.emoji.GetCustomEmojisUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val replyToId: String? = savedStateHandle["replyToId"]
    private val draftId: String? = savedStateHandle["draftId"]

    private val _uiState = MutableStateFlow(ComposeUiState(replyToId = replyToId))
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    // Auto-save job for draft saving
    private var autoSaveJob: Job? = null

    init {
        // Load custom emojis from instance
        loadCustomEmojis()

        // Load draft if resuming from drafts
        if (draftId != null) {
            loadDraft(draftId.toLongOrNull() ?: 0L)
        }
        // Load reply context if replying to a status
        else if (replyToId != null) {
            loadReplyContext(replyToId)
        }
    }

    /**
     * Load custom emojis available on the instance.
     */
    private fun loadCustomEmojis() {
        viewModelScope.launch {
            when (val result = getCustomEmojisUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(customEmojis = result.data, isLoadingEmojis = false)
                    }
                }
                is Result.Error -> {
                    // Emojis are optional, so we don't show error
                    _uiState.update { it.copy(isLoadingEmojis = false) }
                }
                is Result.Loading -> Unit
            }
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
            is ComposeAction.LoadDraft -> loadDraft(action.draftId)
            is ComposeAction.SaveDraftNow -> saveDraftNow()
            is ComposeAction.DeleteDraft -> deleteDraft()
            ComposeAction.AddPoll -> addPoll()
            ComposeAction.RemovePoll -> removePoll()
            is ComposeAction.UpdatePollOption -> updatePollOption(action.index, action.text)
            ComposeAction.AddPollOption -> addPollOption()
            is ComposeAction.RemovePollOption -> removePollOption(action.index)
            is ComposeAction.UpdatePollExpiry -> updatePollExpiry(action.expiresIn)
            ComposeAction.TogglePollMultipleChoice -> togglePollMultipleChoice()
            ComposeAction.TogglePollHideTotals -> togglePollHideTotals()
        }
    }

    /**
     * Update post content.
     */
    private fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
        scheduleAutoSave()
    }

    /**
     * Update post visibility.
     */
    private fun updateVisibility(visibility: StatusVisibility) {
        _uiState.update { it.copy(visibility = visibility) }
        scheduleAutoSave()
    }

    /**
     * Update content warning text.
     */
    private fun updateContentWarning(warning: String) {
        _uiState.update { it.copy(contentWarning = warning) }
        scheduleAutoSave()
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
            _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_max_media)) }
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
                    scheduleAutoSave()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingMedia = false,
                            errorMessage = UiText.of(R.string.error_upload_media, result.message.orEmpty())
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
        scheduleAutoSave()
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
            _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_post_empty)) }
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
                spoilerText = _uiState.value.contentWarning.takeIf { it.isNotBlank() },
                pollConfig = _uiState.value.pollConfig
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPosting = false,
                            isPosted = true
                        )
                    }
                    // Delete the draft since the post was successful
                    deleteDraft()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isPosting = false,
                            errorMessage = UiText.of(R.string.error_post_failed, result.message.orEmpty())
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

    /**
     * Schedule auto-save after a delay.
     * Cancels any existing auto-save job and starts a new one.
     */
    private fun scheduleAutoSave() {
        // Cancel existing job
        autoSaveJob?.cancel()

        // Only schedule auto-save if there's content to save
        if (!_uiState.value.hasDraftContent) {
            return
        }

        // Schedule new auto-save after 5 seconds
        autoSaveJob = viewModelScope.launch {
            delay(5000) // 5 seconds delay
            saveDraft()
        }
    }

    /**
     * Save draft immediately (without delay).
     */
    private fun saveDraftNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            saveDraft()
        }
    }

    /**
     * Save the current draft to the database.
     */
    private suspend fun saveDraft() {
        val state = _uiState.value

        // Don't save if there's no content
        if (!state.hasDraftContent) {
            return
        }

        _uiState.update { it.copy(isSavingDraft = true) }

        // Convert media IDs to JSON
        val mediaIdsJson = if (state.mediaAttachments.isNotEmpty()) {
            Json.encodeToString(state.mediaAttachments.map { it.id })
        } else {
            null
        }

        val draft = DraftEntity(
            id = state.currentDraftId ?: 0L,
            content = state.content,
            mediaIds = mediaIdsJson,
            visibility = state.visibility.value,
            sensitive = false, // TODO: Add sensitive toggle to UI
            spoilerText = state.contentWarning.takeIf { it.isNotBlank() },
            inReplyToId = state.replyToId,
            createdAt = state.currentDraftId?.let { state.lastDraftSavedAt } ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        when (val result = draftRepository.saveDraft(draft)) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        currentDraftId = result.data,
                        isSavingDraft = false,
                        lastDraftSavedAt = System.currentTimeMillis()
                    )
                }
            }
            is Result.Error -> {
                _uiState.update { it.copy(isSavingDraft = false) }
                // Silently fail - don't interrupt user's compose flow
            }
            is Result.Loading -> Unit
        }
    }

    /**
     * Load a draft by ID.
     */
    private fun loadDraft(draftId: Long) {
        viewModelScope.launch {
            when (val result = draftRepository.getDraft(draftId)) {
                is Result.Success -> {
                    val draft = result.data ?: return@launch

                    // Parse media IDs from JSON
                    val mediaIds = draft.mediaIds?.let {
                        try {
                            Json.decodeFromString<List<String>>(it)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList()

                    // TODO: Load media attachments from IDs
                    // For now, we'll just load the text content

                    _uiState.update {
                        it.copy(
                            content = draft.content,
                            visibility = StatusVisibility.entries.find { v -> v.value == draft.visibility }
                                ?: StatusVisibility.PUBLIC,
                            contentWarning = draft.spoilerText ?: "",
                            showContentWarning = draft.spoilerText != null,
                            currentDraftId = draft.id,
                            lastDraftSavedAt = draft.updatedAt
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_load_draft)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Delete the current draft.
     */
    private fun deleteDraft() {
        val draftId = _uiState.value.currentDraftId ?: return

        viewModelScope.launch {
            draftRepository.deleteDraft(draftId)
            _uiState.update {
                it.copy(
                    currentDraftId = null,
                    lastDraftSavedAt = null
                )
            }
        }

        // Cancel auto-save job
        autoSaveJob?.cancel()
    }

    /**
     * Add a poll to the post.
     */
    private fun addPoll() {
        if (_uiState.value.mediaAttachments.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_poll_with_media)) }
            return
        }
        _uiState.update {
            it.copy(
                pollConfig = com.pfitzer.tootsie.domain.model.PollConfig(
                    options = listOf("", ""),
                    expiresIn = 86400 // 1 day
                )
            )
        }
        scheduleAutoSave()
    }

    /**
     * Remove the poll from the post.
     */
    private fun removePoll() {
        _uiState.update { it.copy(pollConfig = null) }
        scheduleAutoSave()
    }

    /**
     * Update a poll option text.
     */
    private fun updatePollOption(index: Int, text: String) {
        _uiState.update { state ->
            state.copy(
                pollConfig = state.pollConfig?.copy(
                    options = state.pollConfig.options.mapIndexed { i, s ->
                        if (i == index) text else s
                    }
                )
            )
        }
        scheduleAutoSave()
    }

    /**
     * Add a new option to the poll.
     */
    private fun addPollOption() {
        _uiState.update { state ->
            val options = state.pollConfig?.options ?: return@update state
            if (options.size >= 4) return@update state
            state.copy(
                pollConfig = state.pollConfig.copy(
                    options = options + ""
                )
            )
        }
        scheduleAutoSave()
    }

    /**
     * Remove an option from the poll.
     */
    private fun removePollOption(index: Int) {
        _uiState.update { state ->
            val options = state.pollConfig?.options ?: return@update state
            if (options.size <= 2) return@update state
            state.copy(
                pollConfig = state.pollConfig.copy(
                    options = options.filterIndexed { i, _ -> i != index }
                )
            )
        }
        scheduleAutoSave()
    }

    /**
     * Update poll expiry time.
     */
    private fun updatePollExpiry(expiresIn: Int) {
        _uiState.update { state ->
            state.copy(
                pollConfig = state.pollConfig?.copy(expiresIn = expiresIn)
            )
        }
        scheduleAutoSave()
    }

    /**
     * Toggle poll multiple choice setting.
     */
    private fun togglePollMultipleChoice() {
        _uiState.update { state ->
            state.copy(
                pollConfig = state.pollConfig?.copy(multiple = !state.pollConfig.multiple)
            )
        }
        scheduleAutoSave()
    }

    /**
     * Toggle poll hide totals setting.
     */
    private fun togglePollHideTotals() {
        _uiState.update { state ->
            state.copy(
                pollConfig = state.pollConfig?.copy(hideTotals = !state.pollConfig.hideTotals)
            )
        }
        scheduleAutoSave()
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
    val errorMessage: UiText? = null,
    val replyToId: String? = null,
    val replyToAccount: String? = null,
    val currentDraftId: Long? = null,
    val isSavingDraft: Boolean = false,
    val lastDraftSavedAt: Long? = null,
    val customEmojis: List<com.pfitzer.tootsie.domain.model.CustomEmoji> = emptyList(),
    val isLoadingEmojis: Boolean = true,
    val pollConfig: com.pfitzer.tootsie.domain.model.PollConfig? = null
) {
    val characterCount: Int get() = content.length
    val hasMedia: Boolean get() = mediaAttachments.isNotEmpty()
    val hasPoll: Boolean get() = pollConfig != null
    val canPost: Boolean get() {
        val hasContent = content.isNotBlank()
        val hasCompletePoll = hasPoll && pollConfig?.options?.all { it.isNotBlank() } == true
        return (hasContent || hasMedia || hasCompletePoll) && !isPosting && !isUploadingMedia && characterCount <= 500
    }
    val isReply: Boolean get() = replyToId != null
    val canAddMedia: Boolean get() = mediaAttachments.size < 4 && !isUploadingMedia && !hasPoll
    val canAddPoll: Boolean get() = !hasPoll && !hasMedia
    val hasDraftContent: Boolean get() = content.isNotBlank() || hasMedia || contentWarning.isNotBlank() || hasPoll
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
    data class LoadDraft(val draftId: Long) : ComposeAction()
    data object SaveDraftNow : ComposeAction()
    data object DeleteDraft : ComposeAction()

    // Poll actions
    data object AddPoll : ComposeAction()
    data object RemovePoll : ComposeAction()
    data class UpdatePollOption(val index: Int, val text: String) : ComposeAction()
    data object AddPollOption : ComposeAction()
    data class RemovePollOption(val index: Int) : ComposeAction()
    data class UpdatePollExpiry(val expiresIn: Int) : ComposeAction()
    data object TogglePollMultipleChoice : ComposeAction()
    data object TogglePollHideTotals : ComposeAction()
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
