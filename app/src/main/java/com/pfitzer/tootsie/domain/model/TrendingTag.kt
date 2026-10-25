package com.pfitzer.tootsie.domain.model

/**
 * Trending hashtag domain model.
 * Represents a hashtag that is being used more frequently than usual.
 */
data class TrendingTag(
    override val name: String,  // Tag name without # prefix
    val url: String,  // URL to the tag page
    val history: List<TagHistory>  // Usage statistics over time
) : HashtagWithHistory {
    /**
     * Calculate total uses across all history.
     */
    override val totalUses: Int
        get() = history.sumOf { it.uses }

    /**
     * Calculate total accounts across all history.
     */
    val totalAccounts: Int
        get() = history.sumOf { it.accounts }
}

/**
 * Tag usage history for a specific day.
 */
data class TagHistory(
    val day: String,  // UNIX timestamp as string
    val uses: Int,  // Number of times the tag was used
    val accounts: Int  // Number of unique accounts that used the tag
)
