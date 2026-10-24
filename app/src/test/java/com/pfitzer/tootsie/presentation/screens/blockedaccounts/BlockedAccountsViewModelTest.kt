package com.pfitzer.tootsie.presentation.screens.blockedaccounts

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.usecase.account.UnblockAccountUseCase
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
class BlockedAccountsViewModelTest {

    private val accountRepository = mockk<AccountRepository>()
    private val unblockAccountUseCase = mockk<UnblockAccountUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { accountRepository.getBlockedAccounts() } returns
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

    private fun viewModel() = BlockedAccountsViewModel(accountRepository, unblockAccountUseCase)

    @Test
    fun `every unblocked account is removed from the list`() = runTest {
        coEvery { unblockAccountUseCase(any()) } returns Result.Success(mockk())
        val viewModel = viewModel()

        viewModel.onAction(BlockedAccountsAction.UnblockAccount(account("a")))
        viewModel.onAction(BlockedAccountsAction.UnblockAccount(account("c")))

        assertEquals(listOf("b"), viewModel.blockedAccountsPagingData.asSnapshot().map { it.id })
        assertTrue(viewModel.uiState.value.processingAccountIds.isEmpty())
    }

    @Test
    fun `list is empty after unblocking all accounts`() = runTest {
        coEvery { unblockAccountUseCase(any()) } returns Result.Success(mockk())
        val viewModel = viewModel()

        listOf("a", "b", "c").forEach {
            viewModel.onAction(BlockedAccountsAction.UnblockAccount(account(it)))
        }

        assertTrue(viewModel.blockedAccountsPagingData.asSnapshot().isEmpty())
    }

    @Test
    fun `failed unblock keeps the account and shows an error`() = runTest {
        coEvery { unblockAccountUseCase(any()) } returns Result.Error("boom")
        val viewModel = viewModel()

        viewModel.onAction(BlockedAccountsAction.UnblockAccount(account("b")))

        assertEquals(listOf("a", "b", "c"), viewModel.blockedAccountsPagingData.asSnapshot().map { it.id })
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
