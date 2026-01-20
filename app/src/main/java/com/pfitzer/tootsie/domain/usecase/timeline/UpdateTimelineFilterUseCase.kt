package com.pfitzer.tootsie.domain.usecase.timeline

import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.TimelineFilter
import javax.inject.Inject

/**
 * Use case to update timeline filter preferences.
 */
class UpdateTimelineFilterUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(filter: TimelineFilter) {
        preferencesManager.saveTimelineFilter(
            hideBoosts = filter.hideBoosts,
            hideReplies = filter.hideReplies,
            showOnlyMedia = filter.showOnlyMedia
        )
    }
}
