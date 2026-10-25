package com.pfitzer.tootsie.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.usecase.account.GetAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetRelationshipUseCase
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.domainblock.BlockDomainUseCase
import com.pfitzer.tootsie.domain.usecase.domainblock.UnblockDomainUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelDomainBlockTest {

    private val getAccountUseCase = mockk<GetAccountUseCase>()
    private val getRelationshipUseCase = mockk<GetRelationshipUseCase>(relaxed = true)
    private val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()
    private val blockDomainUseCase = mockk<BlockDomainUseCase>()
    private val unblockDomainUseCase = mockk<UnblockDomainUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { observeActiveAccountUseCase() } returns flowOf(account("own", "own"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blocking the domain of a remote account reloads the relationship`() = runTest {
        coEvery { getAccountUseCase("remote") } returns Result.Success(account("remote", "alice@example.social"))
        coEvery { blockDomainUseCase("example.social") } returns Result.Success(Unit)
        val viewModel = viewModel("remote")

        viewModel.onAction(ProfileAction.BlockDomain)

        coVerify { blockDomainUseCase("example.social") }
        // Once when opening the profile, once after blocking
        coVerify(exactly = 2) { getRelationshipUseCase("remote") }
        assertEquals(
            UiText.of(R.string.success_block_domain, "example.social"),
            viewModel.uiState.value.successMessage
        )
        assertFalse(viewModel.uiState.value.isProcessingDomainBlock)
    }

    @Test
    fun `failed unblock shows an error`() = runTest {
        coEvery { getAccountUseCase("remote") } returns Result.Success(account("remote", "alice@example.social"))
        coEvery { unblockDomainUseCase("example.social") } returns Result.Error("boom")
        val viewModel = viewModel("remote")

        viewModel.onAction(ProfileAction.UnblockDomain)

        assertEquals(UiText.of(R.string.error_unblock_domain), viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isProcessingDomainBlock)
    }

    @Test
    fun `accounts of the own server have no domain to block`() = runTest {
        coEvery { getAccountUseCase("local") } returns Result.Success(account("local", "bob"))
        val viewModel = viewModel("local")

        viewModel.onAction(ProfileAction.BlockDomain)

        coVerify(exactly = 0) { blockDomainUseCase(any()) }
    }

    private fun viewModel(accountId: String) = ProfileViewModel(
        statusInteractionStore = StatusInteractionStore(),
        getAccountUseCase = getAccountUseCase,
        getRelationshipUseCase = getRelationshipUseCase,
        followAccountUseCase = mockk(relaxed = true),
        unfollowAccountUseCase = mockk(relaxed = true),
        muteAccountUseCase = mockk(relaxed = true),
        unmuteAccountUseCase = mockk(relaxed = true),
        blockAccountUseCase = mockk(relaxed = true),
        unblockAccountUseCase = mockk(relaxed = true),
        blockDomainUseCase = blockDomainUseCase,
        unblockDomainUseCase = unblockDomainUseCase,
        setAccountNoteUseCase = mockk(relaxed = true),
        getAccountStatusesUseCase = mockk(relaxed = true),
        observeActiveAccountUseCase = observeActiveAccountUseCase,
        favoriteStatusUseCase = mockk(relaxed = true),
        unfavoriteStatusUseCase = mockk(relaxed = true),
        reblogStatusUseCase = mockk(relaxed = true),
        unreblogStatusUseCase = mockk(relaxed = true),
        bookmarkStatusUseCase = mockk(relaxed = true),
        unbookmarkStatusUseCase = mockk(relaxed = true),
        pinStatusUseCase = mockk(relaxed = true),
        unpinStatusUseCase = mockk(relaxed = true),
        voteOnPollUseCase = mockk(relaxed = true),
        reportUseCase = mockk(relaxed = true),
        savedStateHandle = SavedStateHandle(mapOf("accountId" to accountId))
    )

    private fun account(id: String, acct: String) = Account(
        id = id,
        username = acct.substringBefore('@'),
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
}
