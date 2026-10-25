package com.pfitzer.tootsie.domain.model

/**
 * Which accounts a follow list shows.
 */
enum class FollowListType {
    /** Accounts that follow the account. */
    FOLLOWERS,

    /** Accounts the account follows. */
    FOLLOWING
}

/**
 * Account in a followers or following list, with the logged-in user's relationship to it
 * (null if it couldn't be loaded).
 */
data class FollowListEntry(
    val account: Account,
    val relationship: Relationship?
)
