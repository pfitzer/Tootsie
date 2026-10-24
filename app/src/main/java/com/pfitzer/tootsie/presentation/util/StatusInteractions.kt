package com.pfitzer.tootsie.presentation.util

import androidx.paging.PagingData
import androidx.paging.map
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.withInteractions
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Overlay the interaction states confirmed during this session (favourite, boost,
 * bookmark, pin) on paged statuses. Apply after cachedIn().
 */
fun Flow<PagingData<Status>>.applyInteractions(
    store: StatusInteractionStore
): Flow<PagingData<Status>> =
    combine(store.interactions) { pagingData, interactions ->
        pagingData.map { it.withInteractions(interactions) }
    }
