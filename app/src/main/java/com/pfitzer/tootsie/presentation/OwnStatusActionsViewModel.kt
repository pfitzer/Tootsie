package com.pfitzer.tootsie.presentation

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.data.local.entities.DraftPoll
import com.pfitzer.tootsie.domain.model.PollConfig
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.presentation.screens.compose.remainingPollSeconds
import com.pfitzer.tootsie.presentation.util.htmlToPlainText
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Deletes own statuses from any screen (used by the nav host, so status cards everywhere
 * can offer it). Deleted statuses disappear from all lists via the StatusInteractionStore.
 */
@HiltViewModel
class OwnStatusActionsViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val draftRepository: DraftRepository
) : ViewModel() {

    private val _events = Channel<OwnStatusEvent>(Channel.BUFFERED)
    val events: Flow<OwnStatusEvent> = _events.receiveAsFlow()

    /**
     * Delete a status, including its media.
     */
    fun delete(status: Status) {
        viewModelScope.launch {
            if (statusRepository.deleteStatus(status.id, deleteMedia = true) is Result.Error) {
                _events.send(OwnStatusEvent.Error(UiText.of(R.string.error_delete_post)))
            }
        }
    }

    /**
     * Delete a status and open it as a draft: text as written, content warning, media
     * (kept on the server for this), poll, visibility, language and reply.
     */
    fun deleteAndRedraft(status: Status) {
        viewModelScope.launch {
            when (val result = statusRepository.deleteStatus(status.id, deleteMedia = false)) {
                is Result.Success -> {
                    val now = System.currentTimeMillis()
                    val draft = DraftEntity(
                        content = result.data.text ?: htmlToPlainText(status.content),
                        mediaIds = status.mediaAttachments.takeIf { it.isNotEmpty() }
                            ?.let { media -> Json.encodeToString(media.map { it.id }) },
                        visibility = status.visibility.name.lowercase(),
                        sensitive = status.sensitive,
                        spoilerText = status.spoilerText.takeIf { it.isNotBlank() },
                        inReplyToId = status.inReplyToId,
                        createdAt = now,
                        updatedAt = now,
                        pollJson = status.poll?.let { poll ->
                            val expiresIn = if (poll.expired) DEFAULT_POLL_SECONDS else remainingPollSeconds(poll.expiresAt)
                            Json.encodeToString(
                                DraftPoll.from(
                                    PollConfig(
                                        options = poll.options.map { it.title },
                                        expiresIn = expiresIn,
                                        multiple = poll.multiple
                                    )
                                )
                            )
                        },
                        language = status.language
                    )
                    when (val saved = draftRepository.saveDraft(draft)) {
                        is Result.Success -> _events.send(OwnStatusEvent.Redraft(saved.data))
                        is Result.Error -> _events.send(OwnStatusEvent.Error(UiText.of(R.string.error_redraft)))
                        is Result.Loading -> Unit
                    }
                }
                is Result.Error -> _events.send(OwnStatusEvent.Error(UiText.of(R.string.error_delete_post)))
                is Result.Loading -> Unit
            }
        }
    }

    private companion object {
        /** Poll duration of a redraft whose poll has ended. */
        const val DEFAULT_POLL_SECONDS = 86400
    }
}

/**
 * One-time results of own status actions, handled by the nav host.
 */
sealed class OwnStatusEvent {
    /** Open the compose screen with this draft. */
    data class Redraft(val draftId: Long) : OwnStatusEvent()
    data class Error(val message: UiText) : OwnStatusEvent()
}
