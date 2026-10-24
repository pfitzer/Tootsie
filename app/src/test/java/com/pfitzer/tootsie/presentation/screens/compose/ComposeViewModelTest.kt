package com.pfitzer.tootsie.presentation.screens.compose

import androidx.lifecycle.SavedStateHandle
import com.pfitzer.tootsie.domain.model.InstanceLimits
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.CustomEmoji
import com.pfitzer.tootsie.domain.model.SearchResults
import com.pfitzer.tootsie.domain.usecase.account.GetAllAccountsUseCase
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.emoji.GetCustomEmojisUseCase
import com.pfitzer.tootsie.domain.usecase.search.SearchUseCase
import com.pfitzer.tootsie.domain.util.AutocompleteToken
import com.pfitzer.tootsie.domain.usecase.instance.GetInstanceLimitsUseCase
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class ComposeViewModelTest {

    private val draftRepository = mockk<DraftRepository>()
    private val getCustomEmojisUseCase = mockk<GetCustomEmojisUseCase>()
    private val getInstanceLimitsUseCase = mockk<GetInstanceLimitsUseCase>()
    private val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()
    private val getAllAccountsUseCase = mockk<GetAllAccountsUseCase>()
    private val searchUseCase = mockk<SearchUseCase>()
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { getCustomEmojisUseCase() } returns Result.Success(emptyList())
        coEvery { getInstanceLimitsUseCase() } returns InstanceLimits(maxCharacters = 1000)
        coEvery { draftRepository.saveDraft(any()) } returns Result.Success(1L)
        coEvery { draftRepository.deleteDraft(any()) } returns Result.Success(Unit)
        every { observeActiveAccountUseCase() } returns flowOf(account("own"))
        coEvery { getAllAccountsUseCase() } returns listOf(account("own"))
        coEvery { preferencesManager.getPostLanguage() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ComposeViewModel(
        postStatusUseCase = mockk(),
        uploadMediaUseCase = mockk(),
        statusRepository = mockk(),
        draftRepository = draftRepository,
        getCustomEmojisUseCase = getCustomEmojisUseCase,
        getInstanceLimitsUseCase = getInstanceLimitsUseCase,
        observeActiveAccountUseCase = observeActiveAccountUseCase,
        getAllAccountsUseCase = getAllAccountsUseCase,
        switchAccountUseCase = mockk(relaxed = true),
        searchUseCase = searchUseCase,
        preferencesManager = preferencesManager,
        savedStateHandle = SavedStateHandle()
    )

    @Test
    fun `leaving right after typing saves the draft before exiting`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateContent("Not saved by the 5 s auto-save yet"))
        viewModel.onAction(ComposeAction.Exit)

        coVerify(exactly = 1) { draftRepository.saveDraft(match { it.content == "Not saved by the 5 s auto-save yet" }) }
        assertTrue(viewModel.uiState.value.isExited)
    }

    @Test
    fun `leaving without content saves nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.Exit)

        coVerify(exactly = 0) { draftRepository.saveDraft(any()) }
        assertTrue(viewModel.uiState.value.isExited)
    }

    @Test
    fun `character limit comes from the instance`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateContent("x".repeat(800)))

        val state = viewModel.uiState.value
        assertEquals(1000, state.maxCharacters)
        assertFalse(state.isOverCharacterLimit)
        assertTrue(state.canPost)

        viewModel.onAction(ComposeAction.UpdateContent("x".repeat(1001)))
        assertTrue(viewModel.uiState.value.isOverCharacterLimit)
        assertFalse(viewModel.uiState.value.canPost)
    }

    @Test
    fun `long links don't block posting`() = runTest {
        coEvery { getInstanceLimitsUseCase() } returns InstanceLimits()
        val viewModel = viewModel()
        val url = "https://example.social/" + "a".repeat(600)

        viewModel.onAction(ComposeAction.UpdateContent("Read this: $url"))

        assertEquals("Read this: ".length + 23, viewModel.uiState.value.characterCount)
        assertTrue(viewModel.uiState.value.canPost)
    }

    @Test
    fun `mention suggestions are looked up after typing pauses`() = runTest {
        coEvery { searchUseCase(query = "ali", type = "accounts", limit = 5) } returns
            Result.Success(SearchResults(accounts = listOf(account("alice"))))
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateAutocomplete(AutocompleteToken(AutocompleteToken.Type.MENTION, 0, 4, "ali")))
        advanceTimeBy(300)

        val suggestion = viewModel.uiState.value.suggestions.single()
        assertEquals("@alice ", suggestion.replacement)

        viewModel.onAction(ComposeAction.UpdateAutocomplete(null))
        assertTrue(viewModel.uiState.value.suggestions.isEmpty())
    }

    @Test
    fun `emoji suggestions come from the instance emojis`() = runTest {
        coEvery { getCustomEmojisUseCase() } returns Result.Success(
            listOf(emoji("blobcat"), emoji("catblob"), emoji("hidden", visible = false), emoji("dog"))
        )
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateAutocomplete(AutocompleteToken(AutocompleteToken.Type.EMOJI, 0, 4, "cat")))

        // Prefix matches first, hidden emojis excluded
        assertEquals(
            listOf(":catblob: ", ":blobcat: "),
            viewModel.uiState.value.suggestions.map { it.replacement }
        )
    }

    @Test
    fun `posting account and switching only for new posts`() = runTest {
        coEvery { getAllAccountsUseCase() } returns listOf(account("own"), account("other"))
        val viewModel = viewModel()

        assertEquals("own", viewModel.uiState.value.account?.id)
        assertTrue(viewModel.uiState.value.canSwitchAccount)
    }

    private fun emoji(shortcode: String, visible: Boolean = true) =
        CustomEmoji(shortcode = shortcode, url = "", staticUrl = "", visibleInPicker = visible)

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
