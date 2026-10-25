package com.pfitzer.tootsie.presentation.util

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.withInteractions
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Overlay the interaction states confirmed during this session (favourite, boost,
 * bookmark, pin) on paged statuses. Apply after cachedIn().
 *
 * The result is cached in [scope] again: collectAsLazyPagingItems() only shows the
 * cached items in the first frame if the flow is a cached (shared) flow. Otherwise the
 * list is empty for a frame when navigating back and loses its scroll position.
 */
fun Flow<PagingData<Status>>.applyInteractions(
    store: StatusInteractionStore,
    scope: CoroutineScope
): Flow<PagingData<Status>> =
    combine(store.interactions) { pagingData, interactions ->
        pagingData.map { it.withInteractions(interactions) }
    }.cachedIn(scope)
