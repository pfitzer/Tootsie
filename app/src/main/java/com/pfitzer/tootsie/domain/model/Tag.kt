package com.pfitzer.tootsie.domain.model

/**
 * Domain model for a followed hashtag.
 * Represents a tag that can be followed by the user.
 */
data class FollowedTag(
    val name: String,
    val url: String,
    val history: List<TagHistory>,
    val following: Boolean
) {
    /**
     * Total number of times this tag has been used across all history.
     */
    val totalUses: Int
        get() = history.sumOf { it.uses }

    /**
     * Total number of unique accounts that have used this tag across all history.
     */
    val totalAccounts: Int
        get() = history.sumOf { it.accounts }
}
