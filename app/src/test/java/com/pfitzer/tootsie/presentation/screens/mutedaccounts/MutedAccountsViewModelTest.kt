package com.pfitzer.tootsie.presentation.screens.mutedaccounts

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.usecase.account.UnmuteAccountUseCase
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MutedAccountsViewModelTest {

    private val accountRepository = mockk<AccountRepository>()
    private val unmuteAccountUseCase = mockk<UnmuteAccountUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { accountRepository.getMutedAccounts() } returns
            flowOf(
                PagingData.from(
                    listOf(account("a"), account("b"), account("c")),
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

    private fun viewModel() = MutedAccountsViewModel(accountRepository, unmuteAccountUseCase)

    @Test
    fun `every unmuted account is removed from the list`() = runTest {
        coEvery { unmuteAccountUseCase(any()) } returns Result.Success(mockk())
        val viewModel = viewModel()

        viewModel.onAction(MutedAccountsAction.UnmuteAccount(account("a")))
        viewModel.onAction(MutedAccountsAction.UnmuteAccount(account("c")))

        assertEquals(listOf("b"), viewModel.mutedAccountsPagingData.asSnapshot().map { it.id })
        assertTrue(viewModel.uiState.value.processingAccountIds.isEmpty())
    }

    @Test
    fun `list is empty after unmuting all accounts`() = runTest {
        coEvery { unmuteAccountUseCase(any()) } returns Result.Success(mockk())
        val viewModel = viewModel()

        listOf("a", "b", "c").forEach {
            viewModel.onAction(MutedAccountsAction.UnmuteAccount(account(it)))
        }

        assertTrue(viewModel.mutedAccountsPagingData.asSnapshot().isEmpty())
    }

    @Test
    fun `failed unmute keeps the account and shows an error`() = runTest {
        coEvery { unmuteAccountUseCase(any()) } returns Result.Error("boom")
        val viewModel = viewModel()

        viewModel.onAction(MutedAccountsAction.UnmuteAccount(account("b")))

        assertEquals(listOf("a", "b", "c"), viewModel.mutedAccountsPagingData.asSnapshot().map { it.id })
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.processingAccountIds.isEmpty())
    }

    private fun account(id: String) = Account(
        id = id,
        username = id,
        acct = id,
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
}
