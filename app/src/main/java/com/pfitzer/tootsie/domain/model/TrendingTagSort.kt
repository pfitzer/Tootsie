package com.pfitzer.tootsie.domain.model

/**
 * Sort order of the trending hashtags.
 */
enum class TrendingTagSort {
    /** Server trend order. */
    TRENDING,

    /** By name, case-insensitive. */
    ALPHABETICAL,

    /** By posts in the last 7 days, most active first; ties by name. */
    ACTIVITY;

    fun sort(tags: List<TrendingTag>): List<TrendingTag> = when (this) {
        TRENDING -> tags
        ALPHABETICAL -> tags.sortedByTagName()
        ACTIVITY -> tags.sortedByTagActivity()
    }

    companion object {
        fun fromName(name: String?): TrendingTagSort = entries.firstOrNull { it.name == name } ?: TRENDING
    }
}
