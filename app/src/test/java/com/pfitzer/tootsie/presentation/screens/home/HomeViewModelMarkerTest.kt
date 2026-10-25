package com.pfitzer.tootsie.presentation.screens.home

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.MarkerRepository
import com.pfitzer.tootsie.domain.repository.MarkerTimeline
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelMarkerTest {

    private val markerRepository = mockk<MarkerRepository>()
    private val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()
    private val activeAccount = MutableStateFlow<Account?>(account("one"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { observeActiveAccountUseCase() } returns activeAccount
        coEvery { markerRepository.getMarker(MarkerTimeline.HOME) } returns Result.Success("100")
        coEvery { markerRepository.saveMarker(any(), any()) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(
        statusInteractionStore = StatusInteractionStore(),
        getTimelineUseCase = mockk(relaxed = true),
        updateTimelineFilterUseCase = mockk(relaxed = true),
        observeActiveAccountUseCase = observeActiveAccountUseCase,
        getFiltersForContextUseCase = mockk(relaxed = true),
        preferencesManager = mockk(relaxed = true),
        favoriteStatusUseCase = mockk(relaxed = true),
        unfavoriteStatusUseCase = mockk(relaxed = true),
        reblogStatusUseCase = mockk(relaxed = true),
        unreblogStatusUseCase = mockk(relaxed = true),
        bookmarkStatusUseCase = mockk(relaxed = true),
        unbookmarkStatusUseCase = mockk(relaxed = true),
        voteOnPollUseCase = mockk(relaxed = true),
        reportUseCase = mockk(relaxed = true),
        markerRepository = markerRepository
    )

    @Test
    fun `the server's reading position is restored once`() = runTest {
        val viewModel = viewModel()
        assertEquals("100", viewModel.uiState.value.homeMarker)

        viewModel.onAction(HomeAction.MarkerRestored)

        assertNull(viewModel.uiState.value.homeMarker)
    }

    @Test
    fun `an unchanged reading position isn't sent again`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(HomeAction.SaveReadingPosition("100"))
        viewModel.onAction(HomeAction.SaveReadingPosition("120"))
        viewModel.onAction(HomeAction.SaveReadingPosition("120"))

        coVerify(exactly = 0) { markerRepository.saveMarker(MarkerTimeline.HOME, "100") }
        coVerify(exactly = 1) { markerRepository.saveMarker(MarkerTimeline.HOME, "120") }
    }

    @Test
    fun `another account gets its own reading position`() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(HomeAction.MarkerRestored)

        coEvery { markerRepository.getMarker(MarkerTimeline.HOME) } returns Result.Success("555")
        activeAccount.value = account("two")

        assertEquals("555", viewModel.uiState.value.homeMarker)
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
