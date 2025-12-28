package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.MastodonList
import com.pfitzer.tootsie.domain.model.RepliesPolicy
import com.pfitzer.tootsie.domain.repository.ListsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ListsRepository.
 * Manages Mastodon Lists via the API.
 */
@Singleton
class ListsRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : ListsRepository {

    override suspend fun getLists(): Result<List<MastodonList>> {
        return try {
            val listsDto = mastodonApi.getLists()
            Result.Success(listsDto.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch lists")
        }
    }

    override suspend fun getList(id: String): Result<MastodonList> {
        return try {
            val listDto = mastodonApi.getList(id)
            Result.Success(listDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch list")
        }
    }

    override suspend fun createList(
        title: String,
        repliesPolicy: RepliesPolicy
    ): Result<MastodonList> {
        return try {
            val listDto = mastodonApi.createList(
                title = title,
                repliesPolicy = repliesPolicy.toApiString()
            )
            Result.Success(listDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create list")
        }
    }

    override suspend fun updateList(
        id: String,
        title: String?,
        repliesPolicy: RepliesPolicy?
    ): Result<MastodonList> {
        return try {
            val listDto = mastodonApi.updateList(
                id = id,
                title = title,
                repliesPolicy = repliesPolicy?.toApiString()
            )
            Result.Success(listDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update list")
        }
    }

    override suspend fun deleteList(id: String): Result<Unit> {
        return try {
            mastodonApi.deleteList(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete list")
        }
    }

    override suspend fun getListAccounts(id: String): Result<List<Account>> {
        return try {
            val accountsDto = mastodonApi.getListAccounts(id)
            Result.Success(accountsDto.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch list accounts")
        }
    }

    override suspend fun addAccountsToList(
        id: String,
        accountIds: List<String>
    ): Result<Unit> {
        return try {
            mastodonApi.addAccountsToList(id, accountIds)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to add accounts to list")
        }
    }

    override suspend fun removeAccountsFromList(
        id: String,
        accountIds: List<String>
    ): Result<Unit> {
        return try {
            mastodonApi.removeAccountsFromList(id, accountIds)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to remove accounts from list")
        }
    }
}
