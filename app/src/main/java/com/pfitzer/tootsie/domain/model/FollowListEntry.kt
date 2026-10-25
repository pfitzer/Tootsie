package com.pfitzer.tootsie.domain.model

/**
 * Which accounts an account list shows.
 *
 * @param ofStatus The list belongs to a status (its ID is given), otherwise to an account
 */
enum class AccountListType(val ofStatus: Boolean) {
    /** Accounts that follow the account. */
    FOLLOWERS(ofStatus = false),

    /** Accounts the account follows. */
    FOLLOWING(ofStatus = false),

    /** Accounts that favourited the status. */
    FAVOURITED_BY(ofStatus = true),

    /** Accounts that boosted the status. */
    REBLOGGED_BY(ofStatus = true)
}

/**
 * Account in an account list (followers, following, favourited/boosted by), with the
 * logged-in user's relationship to it (null if it couldn't be loaded).
 */
data class FollowListEntry(
    val account: Account,
    val relationship: Relationship?
)
