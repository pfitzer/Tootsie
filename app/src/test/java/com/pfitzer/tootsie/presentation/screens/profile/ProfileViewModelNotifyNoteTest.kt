package com.pfitzer.tootsie.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.usecase.account.FollowAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetRelationshipUseCase
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.SetAccountNoteUseCase
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
class ProfileViewModelNotifyNoteTest {

    private val getAccountUseCase = mockk<GetAccountUseCase>()
    private val getRelationshipUseCase = mockk<GetRelationshipUseCase>()
    private val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()
    private val followAccountUseCase = mockk<FollowAccountUseCase>()
    private val setAccountNoteUseCase = mockk<SetAccountNoteUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { observeActiveAccountUseCase() } returns flowOf(account("own"))
        coEvery { getAccountUseCase("friend") } returns Result.Success(account("friend"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the bell follows again with notify, keeping boosts hidden`() = runTest {
        coEvery { getRelationshipUseCase("friend") } returns
            Result.Success(relationship(following = true, showingReblogs = false))
        coEvery { followAccountUseCase("friend", notify = true, showReblogs = false) } returns
            Result.Success(relationship(following = true, showingReblogs = false, notifying = true))
        val viewModel = viewModel()

        viewModel.onAction(ProfileAction.ToggleNotify)

        coVerify { followAccountUseCase("friend", notify = true, showReblogs = false) }
        assertTrue(viewModel.uiState.value.relationship!!.notifying)
        assertFalse(viewModel.uiState.value.isProcessingNotify)
    }

    @Test
    fun `the bell does nothing for accounts that aren't followed`() = runTest {
        coEvery { getRelationshipUseCase("friend") } returns Result.Success(relationship(following = false))
        val viewModel = viewModel()

        viewModel.onAction(ProfileAction.ToggleNotify)

        coVerify(exactly = 0) { followAccountUseCase(any(), any(), any()) }
    }

    @Test
    fun `the note is saved trimmed, a failure shows an error`() = runTest {
        coEvery { getRelationshipUseCase("friend") } returns Result.Success(relationship())
        coEvery { setAccountNoteUseCase("friend", "Met at the conference") } returns
            Result.Success(relationship(note = "Met at the conference"))
        val viewModel = viewModel()

        viewModel.onAction(ProfileAction.SaveNote("  Met at the conference \n"))
        assertEquals("Met at the conference", viewModel.uiState.value.relationship!!.note)

        coEvery { setAccountNoteUseCase("friend", "") } returns Result.Error("boom")
        viewModel.onAction(ProfileAction.SaveNote(""))
        assertEquals(UiText.of(R.string.error_save_note), viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSavingNote)
    }

    private fun viewModel() = ProfileViewModel(
        statusInteractionStore = StatusInteractionStore(),
        getAccountUseCase = getAccountUseCase,
        getRelationshipUseCase = getRelationshipUseCase,
        followAccountUseCase = followAccountUseCase,
        unfollowAccountUseCase = mockk(relaxed = true),
        muteAccountUseCase = mockk(relaxed = true),
        unmuteAccountUseCase = mockk(relaxed = true),
        blockAccountUseCase = mockk(relaxed = true),
        unblockAccountUseCase = mockk(relaxed = true),
        blockDomainUseCase = mockk(relaxed = true),
        unblockDomainUseCase = mockk(relaxed = true),
        setAccountNoteUseCase = setAccountNoteUseCase,
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
        savedStateHandle = SavedStateHandle(mapOf("accountId" to "friend"))
    )

    private fun relationship(
        following: Boolean = false,
        showingReblogs: Boolean = true,
        notifying: Boolean = false,
        note: String = ""
    ) = Relationship(
        id = "friend",
        following = following,
        followedBy = false,
        blocking = false,
        blockedBy = false,
        muting = false,
        mutingNotifications = false,
        requested = false,
        domainBlocking = false,
        showingReblogs = showingReblogs,
        endorsed = false,
        notifying = notifying,
        note = note
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
