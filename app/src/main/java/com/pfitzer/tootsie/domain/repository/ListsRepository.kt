package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.MastodonList
import com.pfitzer.tootsie.domain.model.RepliesPolicy
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Mastodon Lists.
 * Lists allow users to group accounts and view filtered timelines.
 */
interface ListsRepository {
    /**
     * Get all lists owned by the authenticated user.
     */
    suspend fun getLists(): Result<List<MastodonList>>

    /**
     * Get a specific list by ID.
     */
    suspend fun getList(id: String): Result<MastodonList>

    /**
     * Create a new list.
     *
     * @param title Title of the list
     * @param repliesPolicy Replies policy for the list
     */
    suspend fun createList(
        title: String,
        repliesPolicy: RepliesPolicy = RepliesPolicy.LIST
    ): Result<MastodonList>

    /**
     * Update an existing list.
     *
     * @param id List ID
     * @param title New title (null to keep current)
     * @param repliesPolicy New replies policy (null to keep current)
     */
    suspend fun updateList(
        id: String,
        title: String? = null,
        repliesPolicy: RepliesPolicy? = null
    ): Result<MastodonList>

    /**
     * Delete a list.
     *
     * @param id List ID
     */
    suspend fun deleteList(id: String): Result<Unit>

    /**
     * Get accounts in a list.
     *
     * @param id List ID
     */
    suspend fun getListAccounts(id: String): Result<List<Account>>

    /**
     * Add accounts to a list.
     *
     * @param id List ID
     * @param accountIds Account IDs to add
     */
    suspend fun addAccountsToList(
        id: String,
        accountIds: List<String>
    ): Result<Unit>

    /**
     * Remove accounts from a list.
     *
     * @param id List ID
     * @param accountIds Account IDs to remove
     */
    suspend fun removeAccountsFromList(
        id: String,
        accountIds: List<String>
    ): Result<Unit>
}
