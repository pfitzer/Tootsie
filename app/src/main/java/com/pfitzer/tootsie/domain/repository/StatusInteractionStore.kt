package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusInteraction
import com.pfitzer.tootsie.domain.model.toInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory store of interaction states (favourite, boost, bookmark, pin) confirmed by the
 * server during this session. Screens apply it to their already loaded statuses so an
 * interaction is reflected everywhere without reloading.
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
        _interactions.update { it + (target.id to target.toInteraction()) }
    }

    /**
     * Forget all overrides, e.g. when switching accounts.
     */
    fun clear() {
        _interactions.value = emptyMap()
    }
}
