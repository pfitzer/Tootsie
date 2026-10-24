package com.pfitzer.tootsie.data.paging

import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineFilter

/**
 * Applies the timeline filter (boosts, replies, media only) and keyword filters to the
 * statuses of a timeline. Runs on the paged data from the cache, so a filter change needs
 * no reload and filtered posts leave no gap: Paging keeps loading until the screen is full.
 */
class TimelineStatusFilter(
    private val filter: TimelineFilter,
    keywordFilters: List<Filter>
) {

    private val keywordFilterMatcher = KeywordFilterMatcher(keywordFilters)

    /**
     * Returns null if the status is hidden, otherwise the status (marked with the filter
     * title if a keyword filter only warns).
     */
    fun apply(status: Status): Status? {
        if (filter.hideBoosts && status.reblog != null) return null
        if (filter.hideReplies && status.inReplyToId != null) return null
        if (filter.showOnlyMedia && status.mediaAttachments.isEmpty()) return null
        return keywordFilterMatcher.apply(status)
    }
}
