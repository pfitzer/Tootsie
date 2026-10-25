package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusInteraction
import com.pfitzer.tootsie.domain.model.toEditedContent
import com.pfitzer.tootsie.domain.model.toInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory store of interaction states (favourite, boost, bookmark, pin) and edits
 * confirmed by the server during this session. Screens apply it to their already loaded
 * statuses so an interaction or edit is reflected everywhere without reloading.
 */
@Singleton
class StatusInteractionStore @Inject constructor() {

    private val _interactions = MutableStateFlow<Map<String, StatusInteraction>>(emptyMap())
    val interactions: StateFlow<Map<String, StatusInteraction>> = _interactions.asStateFlow()

    /**
     * Record the state of a status returned by an interaction endpoint.
     * Boost responses wrap the original status, whose state is what the UI shows.
     */
    fun update(status: Status) {
        val target = status.reblog ?: status
        _interactions.update {
            // An edit made earlier in this session stays applied
            val interaction = target.toInteraction().copy(editedContent = it[target.id]?.editedContent)
            it + (target.id to interaction)
        }
    }

    /**
     * Record an own status after editing it.
     */
    fun updateEdited(status: Status) {
        _interactions.update {
            it + (status.id to status.toInteraction().copy(editedContent = status.toEditedContent()))
        }
    }

    /**
     * Forget all overrides, e.g. when switching accounts.
     */
    fun clear() {
        _interactions.value = emptyMap()
    }
}
