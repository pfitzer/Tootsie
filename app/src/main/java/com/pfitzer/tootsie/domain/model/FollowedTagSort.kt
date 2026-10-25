package com.pfitzer.tootsie.domain.model

/**
 * Sort order of the followed hashtags.
 */
enum class FollowedTagSort {
    /** By name, case-insensitive. */
    ALPHABETICAL,

    /** By posts in the last 7 days, most active first; ties by name. */
    ACTIVITY;

    fun sort(tags: List<FollowedTag>): List<FollowedTag> = when (this) {
        ALPHABETICAL -> tags.sortedByTagName()
        ACTIVITY -> tags.sortedByTagActivity()
    }

    companion object {
        fun fromName(name: String?): FollowedTagSort = entries.firstOrNull { it.name == name } ?: ALPHABETICAL
    }
}
