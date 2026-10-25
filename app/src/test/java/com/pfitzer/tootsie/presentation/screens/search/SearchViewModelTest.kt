package com.pfitzer.tootsie.presentation.screens.search

import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Hashtag
import com.pfitzer.tootsie.domain.model.SearchResults
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.search.SearchUseCase
import com.pfitzer.tootsie.domain.usecase.trending.GetTrendingHashtagsUseCase
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val searchUseCase = mockk<SearchUseCase>()
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val interactionStore = StatusInteractionStore()
    private val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()
    private val getTrendingHashtagsUseCase = mockk<GetTrendingHashtagsUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeActiveAccountUseCase() } returns flowOf(account("own"))
        coEvery { getTrendingHashtagsUseCase(any(), any()) } returns Result.Success(emptyList())
        coEvery { preferencesManager.getRecentSearches("own") } returns listOf("older")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SearchViewModel(
        statusInteractionStore = interactionStore,
        searchUseCase = searchUseCase,
        observeActiveAccountUseCase = observeActiveAccountUseCase,
        favoriteStatusUseCase = mockk(),
        unfavoriteStatusUseCase = mockk(),
        reblogStatusUseCase = mockk(),
        unreblogStatusUseCase = mockk(),
        bookmarkStatusUseCase = mockk(),
        unbookmarkStatusUseCase = mockk(),
        voteOnPollUseCase = mockk(),
        followTagUseCase = mockk(),
        unfollowTagUseCase = mockk(),
        getTrendingHashtagsUseCase = getTrendingHashtagsUseCase,
        preferencesManager = preferencesManager
    )

    private fun TestScope.search(viewModel: SearchViewModel, query: String) {
        viewModel.onAction(SearchAction.UpdateQuery(query))
        advanceUntilIdle()
    }

    @Test
    fun `handles and urls are resolved`() {
        assertTrue(SearchViewModel.shouldResolve("@alice@example.social"))
        assertTrue(SearchViewModel.shouldResolve("alice@example.social"))
        assertTrue(SearchViewModel.shouldResolve("https://example.social/@alice/123"))
        assertFalse(SearchViewModel.shouldResolve("alice"))
        assertFalse(SearchViewModel.shouldResolve("@alice"))
        assertFalse(SearchViewModel.shouldResolve("kotlin android"))
    }

    @Test
    fun `remote handle is searched with resolve`() = runTest(dispatcher) {
        coEvery { searchUseCase(any(), any(), any(), any(), any(), any()) } returns Result.Success(SearchResults())
        val viewModel = viewModel()

        search(viewModel, "@alice@example.social")

        coVerify { searchUseCase(query = "@alice@example.social", type = null, resolve = true, following = false, limit = 5, offset = 0) }
    }

    @Test
    fun `tab pages further with offset`() = runTest(dispatcher) {
        coEvery { searchUseCase(any(), "accounts", any(), any(), 20, 0) } returns
            Result.Success(SearchResults(accounts = (1..20).map { account("a$it") }))
        coEvery { searchUseCase(any(), "accounts", any(), any(), 20, 20) } returns
            Result.Success(SearchResults(accounts = (21..25).map { account("a$it") }))
        val viewModel = viewModel()
        viewModel.onAction(SearchAction.SelectFilter(SearchFilter.ACCOUNTS))

        search(viewModel, "alice")
        assertFalse(viewModel.uiState.value.endReached)

        viewModel.onAction(SearchAction.LoadMore)
        advanceUntilIdle()

        assertEquals(25, viewModel.uiState.value.accounts.size)
        assertTrue(viewModel.uiState.value.endReached)
    }

    @Test
    fun `submitted query becomes the newest recent search`() = runTest(dispatcher) {
        coEvery { searchUseCase(any(), any(), any(), any(), any(), any()) } returns Result.Success(SearchResults())
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(SearchAction.UpdateQuery("kotlin"))
        viewModel.onAction(SearchAction.Submit)
        viewModel.onAction(SearchAction.UpdateQuery("Older"))
        viewModel.onAction(SearchAction.Submit)
        advanceUntilIdle()

        // Case-insensitive duplicates are replaced, newest first
        assertEquals(listOf("Older", "kotlin"), viewModel.uiState.value.recentSearches)
        coVerify { preferencesManager.saveRecentSearches("own", listOf("Older", "kotlin")) }
    }

    @Test
    fun `hashtag follow state is updated in the results`() = runTest(dispatcher) {
        val followTag = mockk<com.pfitzer.tootsie.domain.usecase.followedtags.FollowTagUseCase>()
        coEvery { followTag("kotlin") } returns Result.Success(mockk())
        coEvery { searchUseCase(any(), any(), any(), any(), any(), any()) } returns
            Result.Success(SearchResults(hashtags = listOf(Hashtag(name = "kotlin", url = "", following = false))))
        val viewModel = SearchViewModel(
            statusInteractionStore = interactionStore,
            searchUseCase = searchUseCase,
            observeActiveAccountUseCase = observeActiveAccountUseCase,
            favoriteStatusUseCase = mockk(),
            unfavoriteStatusUseCase = mockk(),
            reblogStatusUseCase = mockk(),
            unreblogStatusUseCase = mockk(),
            bookmarkStatusUseCase = mockk(),
            unbookmarkStatusUseCase = mockk(),
            voteOnPollUseCase = mockk(),
            followTagUseCase = followTag,
            unfollowTagUseCase = mockk(),
            getTrendingHashtagsUseCase = getTrendingHashtagsUseCase,
            preferencesManager = preferencesManager
        )
        search(viewModel, "kotlin")

        viewModel.onAction(SearchAction.ToggleFollowHashtag(viewModel.uiState.value.hashtags.single()))
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.hashtags.single().following)
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
