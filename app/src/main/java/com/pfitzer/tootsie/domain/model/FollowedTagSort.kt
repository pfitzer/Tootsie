package com.pfitzer.tootsie.domain.model

import java.util.Locale

/**
 * Sort order of the followed hashtags.
 */
enum class FollowedTagSort {
    /** By name, case-insensitive. */
    ALPHABETICAL,

    /** By posts in the last 7 days, most active first; ties by name. */
    ACTIVITY;

    fun sort(tags: List<FollowedTag>): List<FollowedTag> {
        val byName = compareBy<FollowedTag> { it.name.lowercase(Locale.ROOT) }
        return when (this) {
            ALPHABETICAL -> tags.sortedWith(byName)
            ACTIVITY -> tags.sortedWith(compareByDescending<FollowedTag> { it.totalUses }.then(byName))
        }
    }

    companion object {
        fun fromName(name: String?): FollowedTagSort = entries.firstOrNull { it.name == name } ?: ALPHABETICAL
    }
}
