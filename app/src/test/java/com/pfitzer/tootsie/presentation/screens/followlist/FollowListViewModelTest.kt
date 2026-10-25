package com.pfitzer.tootsie.presentation.screens.followlist

import androidx.lifecycle.SavedStateHandle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.FollowListEntry
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.usecase.account.FollowAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.UnfollowAccountUseCase
import com.pfitzer.tootsie.presentation.util.UiText
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FollowListViewModelTest {

    private val accountRepository = mockk<AccountRepository>()
    private val getAccountUseCase = mockk<GetAccountUseCase>()
    private val followAccountUseCase = mockk<FollowAccountUseCase>()
    private val unfollowAccountUseCase = mockk<UnfollowAccountUseCase>()
    private val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()

    private val stranger = FollowListEntry(account("stranger"), relationship("stranger"))
    private val friend = FollowListEntry(account("friend"), relationship("friend", following = true))
    private val pending = FollowListEntry(account("pending"), relationship("pending", requested = true))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { observeActiveAccountUseCase() } returns flowOf(account("own"))
        coEvery { getAccountUseCase("remote") } returns Result.Success(account("remote", acct = "remote@example.social"))
        every { accountRepository.getFollowers("remote") } returns flowOf(
            PagingData.from(
                listOf(stranger, friend, pending),
                // All loaded, otherwise asSnapshot() waits for further pages forever
                sourceLoadStates = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true)
                )
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = FollowListViewModel(
        accountRepository = accountRepository,
        getAccountUseCase = getAccountUseCase,
        followAccountUseCase = followAccountUseCase,
        unfollowAccountUseCase = unfollowAccountUseCase,
        observeActiveAccountUseCase = observeActiveAccountUseCase,
        savedStateHandle = SavedStateHandle(mapOf("id" to "remote", "type" to "FOLLOWERS"))
    )

    @Test
    fun `following an account updates its entry`() = runTest {
        coEvery { followAccountUseCase("stranger") } returns Result.Success(relationship("stranger", following = true))
        val viewModel = viewModel()

        viewModel.onAction(FollowListAction.ToggleFollow(stranger))

        val entry = viewModel.entries.asSnapshot().first { it.account.id == "stranger" }
        assertTrue(entry.relationship!!.following)
        assertTrue(viewModel.uiState.value.processingIds.isEmpty())
    }

    @Test
    fun `followed and requested accounts are unfollowed`() = runTest {
        coEvery { unfollowAccountUseCase(any()) } answers { Result.Success(relationship(firstArg())) }
        val viewModel = viewModel()

        viewModel.onAction(FollowListAction.ToggleFollow(friend))
        viewModel.onAction(FollowListAction.ToggleFollow(pending))

        coVerify { unfollowAccountUseCase("friend") }
        coVerify { unfollowAccountUseCase("pending") }
        coVerify(exactly = 0) { followAccountUseCase(any()) }
        val entries = viewModel.entries.asSnapshot()
        assertFalse(entries.any { it.relationship!!.following || it.relationship!!.requested })
    }

    @Test
    fun `failed follow shows an error and keeps the entry`() = runTest {
        coEvery { followAccountUseCase("stranger") } returns Result.Error("boom")
        val viewModel = viewModel()

        viewModel.onAction(FollowListAction.ToggleFollow(stranger))

        assertEquals(UiText.of(R.string.error_follow_account), viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.entries.asSnapshot().first { it.account.id == "stranger" }.relationship!!.following)
    }

    @Test
    fun `remote accounts and the own account are recognized`() = runTest {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isRemoteAccount)
        assertEquals("own", viewModel.uiState.value.ownAccountId)
    }

    @Test
    fun `lists of a status load their accounts without an account title`() = runTest {
        every { accountRepository.getFavouritedBy("status-1") } returns flowOf(
            PagingData.from(
                listOf(friend),
                sourceLoadStates = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true)
                )
            )
        )
        val viewModel = FollowListViewModel(
            accountRepository = accountRepository,
            getAccountUseCase = getAccountUseCase,
            followAccountUseCase = followAccountUseCase,
            unfollowAccountUseCase = unfollowAccountUseCase,
            observeActiveAccountUseCase = observeActiveAccountUseCase,
            savedStateHandle = SavedStateHandle(mapOf("id" to "status-1", "type" to "FAVOURITED_BY"))
        )

        assertEquals(listOf("friend"), viewModel.entries.asSnapshot().map { it.account.id })
        assertEquals(null, viewModel.uiState.value.account)
        coVerify(exactly = 0) { getAccountUseCase(any()) }
    }

    private fun account(id: String, acct: String = id) = Account(
        id = id,
        username = id,
        acct = acct,
        displayName = id,
        note = "",
        url = "",
        avatar = "",
        header = "",
        locked = false,
        followersCount = 0,
        followingCount = 0,
        statusesCount = 0,
        createdAt = "2025-01-01T00:00:00Z",
        bot = false
    )

    private fun relationship(id: String, following: Boolean = false, requested: Boolean = false) = Relationship(
        id = id,
        following = following,
        followedBy = false,
        blocking = false,
        blockedBy = false,
        muting = false,
        mutingNotifications = false,
        requested = requested,
        domainBlocking = false,
        showingReblogs = true,
        endorsed = false,
        notifying = false,
        note = ""
    )
}
