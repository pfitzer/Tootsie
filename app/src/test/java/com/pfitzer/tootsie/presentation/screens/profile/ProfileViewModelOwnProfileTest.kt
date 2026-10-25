package com.pfitzer.tootsie.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.usecase.account.GetAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetRelationshipUseCase
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelOwnProfileTest {

    private val getAccountUseCase = mockk<GetAccountUseCase>()
    private val getRelationshipUseCase = mockk<GetRelationshipUseCase>(relaxed = true)
    private val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { observeActiveAccountUseCase() } returns flowOf(account("own"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `own profile opened by account id is recognized`() = runTest {
        coEvery { getAccountUseCase("own") } returns Result.Success(account("own"))

        val viewModel = viewModel(accountId = "own")

        assertTrue(viewModel.uiState.value.isOwnProfile)
        coVerify(exactly = 0) { getRelationshipUseCase(any()) }
    }

    @Test
    fun `other profile is not treated as own and loads the relationship`() = runTest {
        coEvery { getAccountUseCase("other") } returns Result.Success(account("other"))

        val viewModel = viewModel(accountId = "other")

        assertFalse(viewModel.uiState.value.isOwnProfile)
        coVerify { getRelationshipUseCase("other") }
    }

    @Test
    fun `own profile from the bottom navigation stays own`() = runTest {
        coEvery { getAccountUseCase(null) } returns Result.Success(account("own"))

        val viewModel = viewModel(accountId = null)

        assertTrue(viewModel.uiState.value.isOwnProfile)
    }

    private fun viewModel(accountId: String?) = ProfileViewModel(
        statusInteractionStore = StatusInteractionStore(),
        getAccountUseCase = getAccountUseCase,
        getRelationshipUseCase = getRelationshipUseCase,
        followAccountUseCase = mockk(relaxed = true),
        unfollowAccountUseCase = mockk(relaxed = true),
        muteAccountUseCase = mockk(relaxed = true),
        unmuteAccountUseCase = mockk(relaxed = true),
        blockAccountUseCase = mockk(relaxed = true),
        unblockAccountUseCase = mockk(relaxed = true),
        blockDomainUseCase = mockk(relaxed = true),
        unblockDomainUseCase = mockk(relaxed = true),
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
