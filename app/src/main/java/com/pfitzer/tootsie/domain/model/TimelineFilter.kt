package com.pfitzer.tootsie.domain.model

/**
 * Timeline filter configuration.
 * Defines which posts to show/hide in the timeline.
 */
data class TimelineFilter(
    val hideBoosts: Boolean = false,
    val hideReplies: Boolean = false,
    val showOnlyMedia: Boolean = false
) {
    companion object {
        /**
         * Default filter (no filtering applied).
         */
        val DEFAULT = TimelineFilter()
    }

    /**
     * Check if any filters are active.
     */
    val hasActiveFilters: Boolean
        get() = hideBoosts || hideReplies || showOnlyMedia
}
