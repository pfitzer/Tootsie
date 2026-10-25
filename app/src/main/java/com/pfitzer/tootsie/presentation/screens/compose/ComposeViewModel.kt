package com.pfitzer.tootsie.presentation.screens.compose

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.domain.model.InstanceLimits
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.domain.repository.ScheduledStatusRepository
import com.pfitzer.tootsie.domain.model.NewStatus
import com.pfitzer.tootsie.domain.util.ScheduleTime
import java.time.Instant
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.CustomEmoji
import com.pfitzer.tootsie.domain.usecase.account.GetAllAccountsUseCase
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.SwitchAccountUseCase
import com.pfitzer.tootsie.domain.usecase.instance.GetInstanceLimitsUseCase
import com.pfitzer.tootsie.domain.usecase.search.SearchUseCase
import com.pfitzer.tootsie.domain.util.AutocompleteToken
import java.util.Locale
import com.pfitzer.tootsie.domain.util.StatusLengthCounter
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
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
    private val scheduledStatusRepository: ScheduledStatusRepository,
    private val getCustomEmojisUseCase: com.pfitzer.tootsie.domain.usecase.emoji.GetCustomEmojisUseCase,
    private val getInstanceLimitsUseCase: GetInstanceLimitsUseCase,
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    private val getAllAccountsUseCase: GetAllAccountsUseCase,
    private val switchAccountUseCase: SwitchAccountUseCase,
    private val searchUseCase: SearchUseCase,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val replyToId: String? = savedStateHandle["replyToId"]
    private val draftId: String? = savedStateHandle["draftId"]
    private val quoteId: String? = savedStateHandle["quoteId"]
    private val scheduledId: String? = savedStateHandle["scheduledId"]

    private val _uiState = MutableStateFlow(ComposeUiState(replyToId = replyToId, quoteId = quoteId))
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    // Auto-save job for draft saving
    private var autoSaveJob: Job? = null

    // Pending autocomplete lookup, replaced on every keystroke
    private var autocompleteJob: Job? = null

    init {
        // The posting account; emojis and limits belong to its instance and are
        // (re)loaded whenever it changes
        observeActiveAccount()
        loadAccounts()
        loadLanguage()

        // Load the scheduled post being edited
        if (scheduledId != null) {
            loadScheduledStatus(scheduledId)
        }
        // Load draft if resuming from drafts
        else if (draftId != null) {
            loadDraft(draftId.toLongOrNull() ?: 0L)
        }
        // Load reply context if replying to a status
        else if (replyToId != null) {
            loadReplyContext(replyToId)
        }

        // Load the quoted status for the preview
        if (quoteId != null) {
            loadQuotedStatus(quoteId)
        }
    }

    /**
     * Load a scheduled post to edit it. It is replaced by a new one when posting, since
     * Mastodon can only change the time of a scheduled post.
     */
    private fun loadScheduledStatus(id: String) {
        viewModelScope.launch {
            when (val result = scheduledStatusRepository.getScheduledStatus(id)) {
                is Result.Success -> {
                    val scheduled = result.data
                    _uiState.update {
                        it.copy(
                            editingScheduledId = scheduled.id,
                            scheduledAt = scheduled.scheduledAt,
                            content = scheduled.text,
                            contentWarning = scheduled.spoilerText.orEmpty(),
                            showContentWarning = scheduled.spoilerText != null,
                            visibility = StatusVisibility.entries.find { v -> v.value == scheduled.visibility }
                                ?: StatusVisibility.PUBLIC,
                            language = scheduled.language ?: it.language,
                            replyToId = scheduled.inReplyToId,
                            pollConfig = scheduled.poll,
                            mediaAttachments = scheduled.mediaAttachments
                        )
                    }
                    scheduled.inReplyToId?.let { loadReplyContext(it) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_load_scheduled_post)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Load the status being quoted, for the preview card.
     */
    private fun loadQuotedStatus(statusId: String) {
        viewModelScope.launch {
            val result = statusRepository.getStatus(statusId)
            if (result is Result.Success) {
                _uiState.update { it.copy(quotedStatus = result.data) }
            }
            // Without the preview the quote can still be posted
        }
    }

    /**
     * Observe the active (posting) account and load its instance's emojis and limits.
     */
    private fun observeActiveAccount() {
        viewModelScope.launch {
            observeActiveAccountUseCase()
                .filterNotNull()
                .distinctUntilChanged { old, new -> old.id == new.id }
                .collect { account ->
                    _uiState.update { it.copy(account = account) }
                    loadCustomEmojis()
                    loadInstanceLimits()
                }
        }
    }

    /**
     * Load the logged-in accounts, for switching the posting account.
     */
    private fun loadAccounts() {
        viewModelScope.launch {
            val accounts = runCatching { getAllAccountsUseCase() }.getOrDefault(emptyList())
            _uiState.update { it.copy(accounts = accounts) }
        }
    }

    /**
     * Default post language: the last one used, otherwise the device language.
     */
    private fun loadLanguage() {
        viewModelScope.launch {
            val language = preferencesManager.getPostLanguage() ?: Locale.getDefault().language
            _uiState.update { it.copy(language = language) }
        }
    }

    /**
     * Post as another logged-in account. Only offered for new posts: replies and quotes
     * refer to status IDs of the current account's instance.
     */
    private fun switchAccount(accountId: String) {
        if (!_uiState.value.canSwitchAccount || accountId == _uiState.value.account?.id) return
        viewModelScope.launch {
            _uiState.update { it.copy(suggestions = emptyList()) }
            switchAccountUseCase(accountId)
            // The new account arrives via observeActiveAccount
        }
    }

    private fun selectLanguage(language: String) {
        _uiState.update { it.copy(language = language) }
    }

    /**
     * Look up suggestions for the word being typed (mention, hashtag or custom emoji).
     */
    private fun updateAutocomplete(token: AutocompleteToken?) {
        autocompleteJob?.cancel()
        if (token == null) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }
        if (token.type == AutocompleteToken.Type.EMOJI) {
            val query = token.query.lowercase()
            val emojis = _uiState.value.customEmojis
                .filter { it.visibleInPicker && it.shortcode.lowercase().contains(query) }
                .sortedBy { !it.shortcode.lowercase().startsWith(query) }
                .take(MAX_SUGGESTIONS)
                .map { ComposeSuggestion.Emoji(it) }
            _uiState.update { it.copy(suggestions = emojis) }
            return
        }
        autocompleteJob = viewModelScope.launch {
            // Wait until typing pauses
            delay(AUTOCOMPLETE_DELAY_MS)
            val type = if (token.type == AutocompleteToken.Type.MENTION) "accounts" else "hashtags"
            val result = searchUseCase(query = token.query, type = type, limit = MAX_SUGGESTIONS)
            val suggestions = (result as? Result.Success)?.data?.let { results ->
                if (token.type == AutocompleteToken.Type.MENTION) {
                    results.accounts.map { ComposeSuggestion.Mention(it) }
                } else {
                    results.hashtags.map { ComposeSuggestion.Hashtag(it.name) }
                }
            }.orEmpty()
            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

    /**
     * Load the posting limits (character limit etc.) of the instance.
     */
    private fun loadInstanceLimits() {
        viewModelScope.launch {
            val limits = getInstanceLimitsUseCase()
            _uiState.update { it.copy(limits = limits) }
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
            is ComposeAction.Exit -> exit()
            is ComposeAction.SwitchAccount -> switchAccount(action.accountId)
            is ComposeAction.SelectLanguage -> selectLanguage(action.language)
            is ComposeAction.UpdateAutocomplete -> updateAutocomplete(action.token)
            is ComposeAction.DeleteDraft -> deleteDraft()
            ComposeAction.AddPoll -> addPoll()
            ComposeAction.RemovePoll -> removePoll()
            is ComposeAction.UpdatePollOption -> updatePollOption(action.index, action.text)
            ComposeAction.AddPollOption -> addPollOption()
            is ComposeAction.RemovePollOption -> removePollOption(action.index)
            is ComposeAction.UpdatePollExpiry -> updatePollExpiry(action.expiresIn)
            ComposeAction.TogglePollMultipleChoice -> togglePollMultipleChoice()
            ComposeAction.TogglePollHideTotals -> togglePollHideTotals()
            is ComposeAction.SetSchedule -> _uiState.update { it.copy(scheduledAt = action.scheduledAt) }
            ComposeAction.ClearSchedule -> _uiState.update { it.copy(scheduledAt = null) }
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

        val scheduledAt = _uiState.value.scheduledAt
        if (scheduledAt != null && !ScheduleTime.isValid(scheduledAt, Instant.now())) {
            _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_schedule_too_soon)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, errorMessage = null) }

            val state = _uiState.value
            val mediaIds = state.mediaAttachments.map { it.id }
            val newStatus = NewStatus(
                content = content,
                visibility = state.visibility.value,
                inReplyToId = state.replyToId,
                mediaIds = mediaIds.takeIf { it.isNotEmpty() },
                spoilerText = state.contentWarning.takeIf { it.isNotBlank() },
                pollConfig = state.pollConfig,
                quotedStatusId = quoteId,
                language = state.language
            )

            val result = if (scheduledAt != null) {
                scheduledStatusRepository.scheduleStatus(newStatus, scheduledAt)
            } else {
                postStatusUseCase(
                    content = newStatus.content,
                    visibility = newStatus.visibility,
                    inReplyToId = newStatus.inReplyToId,
                    mediaIds = newStatus.mediaIds,
                    spoilerText = newStatus.spoilerText,
                    pollConfig = newStatus.pollConfig,
                    quotedStatusId = newStatus.quotedStatusId,
                    language = newStatus.language
                )
            }

            when (result) {
                is Result.Success -> {
                    // The edited scheduled post is replaced by the new one; deleted only now,
                    // so it isn't lost if posting fails
                    state.editingScheduledId?.let { scheduledStatusRepository.deleteScheduledStatus(it) }
                    // Default for the next post
                    state.language?.let { preferencesManager.savePostLanguage(it) }
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
                            errorMessage = UiText.of(
                                if (scheduledAt != null) R.string.error_schedule_failed else R.string.error_post_failed,
                                result.message.orEmpty()
                            )
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

        // Only schedule auto-save if there's content to save; a scheduled post being
        // edited is kept on the server, not as a draft
        if (!_uiState.value.hasDraftContent || _uiState.value.isEditingScheduled) {
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
     * Leave the compose screen: save the draft first, so text typed within the last
     * auto-save delay isn't lost. A draft that has been emptied is deleted instead.
     */
    private fun exit() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            val state = _uiState.value
            when {
                state.isPosted || state.isEditingScheduled -> Unit
                state.hasDraftContent -> saveDraft()
                state.currentDraftId != null -> draftRepository.deleteDraft(state.currentDraftId)
            }
            _uiState.update { it.copy(isExited = true) }
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
    val quoteId: String? = null,
    val quotedStatus: com.pfitzer.tootsie.domain.model.Status? = null,
    val currentDraftId: Long? = null,
    val isSavingDraft: Boolean = false,
    val lastDraftSavedAt: Long? = null,
    val customEmojis: List<com.pfitzer.tootsie.domain.model.CustomEmoji> = emptyList(),
    val isLoadingEmojis: Boolean = true,
    val pollConfig: com.pfitzer.tootsie.domain.model.PollConfig? = null,
    val limits: InstanceLimits = InstanceLimits(),
    val isExited: Boolean = false,
    /** The account that posts (the active account). */
    val account: Account? = null,
    val accounts: List<Account> = emptyList(),
    /** ISO 639-1 language of the post. */
    val language: String? = null,
    val suggestions: List<ComposeSuggestion> = emptyList(),
    /** Publication time; null to post right away. */
    val scheduledAt: Instant? = null,
    /** ID of the scheduled post being edited. */
    val editingScheduledId: String? = null
) {
    /** Length as Mastodon counts it (URLs, mentions, content warning). */
    val characterCount: Int by lazy {
        StatusLengthCounter.count(content, contentWarning, limits.charactersReservedPerUrl)
    }
    val maxCharacters: Int get() = limits.maxCharacters
    val isOverCharacterLimit: Boolean get() = characterCount > maxCharacters
    val hasMedia: Boolean get() = mediaAttachments.isNotEmpty()
    val hasPoll: Boolean get() = pollConfig != null
    val canPost: Boolean get() {
        val hasContent = content.isNotBlank()
        val hasCompletePoll = hasPoll && pollConfig?.options?.all { it.isNotBlank() } == true
        return (hasContent || hasMedia || hasCompletePoll) && !isPosting && !isUploadingMedia && !isOverCharacterLimit
    }
    val isReply: Boolean get() = replyToId != null
    val isScheduled: Boolean get() = scheduledAt != null
    val isEditingScheduled: Boolean get() = editingScheduledId != null
    val canSwitchAccount: Boolean get() = accounts.size > 1 && !isReply && quoteId == null && !isEditingScheduled
    val isQuote: Boolean get() = quoteId != null
    val canAddMedia: Boolean get() = mediaAttachments.size < limits.maxMediaAttachments && !isUploadingMedia && !hasPoll
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
    /** Leave the screen; the draft is saved before isExited is set. */
    data object Exit : ComposeAction()
    data class SwitchAccount(val accountId: String) : ComposeAction()
    data class SelectLanguage(val language: String) : ComposeAction()
    /** The word at the cursor changed; null if there is nothing to complete. */
    data class UpdateAutocomplete(val token: AutocompleteToken?) : ComposeAction()
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

    // Scheduling
    data class SetSchedule(val scheduledAt: Instant) : ComposeAction()
    data object ClearSchedule : ComposeAction()
}

/**
 * Autocomplete suggestion while typing.
 */
sealed class ComposeSuggestion {
    /** Text that replaces the typed word. */
    abstract val replacement: String

    data class Mention(val account: Account) : ComposeSuggestion() {
        override val replacement: String get() = "@${account.acct} "
    }

    data class Hashtag(val name: String) : ComposeSuggestion() {
        override val replacement: String get() = "#$name "
    }

    data class Emoji(val emoji: CustomEmoji) : ComposeSuggestion() {
        override val replacement: String get() = ":${emoji.shortcode}: "
    }
}

/**
 * Status visibility levels.
 */
enum class StatusVisibility(val value: String, @StringRes val labelRes: Int) {
    PUBLIC("public", R.string.visibility_public),
    UNLISTED("unlisted", R.string.visibility_unlisted),
    PRIVATE("private", R.string.visibility_private),
    DIRECT("direct", R.string.visibility_direct)
}

private const val MAX_SUGGESTIONS = 5
private const val AUTOCOMPLETE_DELAY_MS = 250L
