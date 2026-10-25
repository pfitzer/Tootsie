package com.pfitzer.tootsie.presentation.screens.compose

import androidx.lifecycle.SavedStateHandle
import com.pfitzer.tootsie.domain.model.InstanceLimits
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.domain.repository.ScheduledStatusRepository
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.ScheduledStatus
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusSource
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import java.time.Duration
import java.time.Instant
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
    private val scheduledStatusRepository = mockk<ScheduledStatusRepository>()
    private val statusRepository = mockk<StatusRepository>()
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

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = ComposeViewModel(
        postStatusUseCase = mockk(),
        uploadMediaUseCase = mockk(),
        statusRepository = statusRepository,
        draftRepository = draftRepository,
        scheduledStatusRepository = scheduledStatusRepository,
        getCustomEmojisUseCase = getCustomEmojisUseCase,
        getInstanceLimitsUseCase = getInstanceLimitsUseCase,
        observeActiveAccountUseCase = observeActiveAccountUseCase,
        getAllAccountsUseCase = getAllAccountsUseCase,
        switchAccountUseCase = mockk(relaxed = true),
        searchUseCase = searchUseCase,
        preferencesManager = preferencesManager,
        savedStateHandle = savedStateHandle
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

    @Test
    fun `a scheduled post is sent with its time instead of being posted`() = runTest {
        val at = Instant.now().plus(Duration.ofHours(2))
        coEvery { scheduledStatusRepository.scheduleStatus(any(), any()) } returns Result.Success(scheduled("1", at))
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateContent("Later"))
        viewModel.onAction(ComposeAction.SetSchedule(at))
        viewModel.onAction(ComposeAction.Post)

        coVerify { scheduledStatusRepository.scheduleStatus(match { it.content == "Later" }, at) }
        assertTrue(viewModel.uiState.value.isPosted)
    }

    @Test
    fun `a time less than 5 minutes ahead is rejected`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateContent("Too soon"))
        viewModel.onAction(ComposeAction.SetSchedule(Instant.now().plus(Duration.ofMinutes(2))))
        viewModel.onAction(ComposeAction.Post)

        assertEquals(UiText.of(R.string.error_schedule_too_soon), viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { scheduledStatusRepository.scheduleStatus(any(), any()) }
        assertFalse(viewModel.uiState.value.isPosted)
    }

    @Test
    fun `an edited scheduled post replaces the old one and is not kept as draft`() = runTest {
        val at = Instant.now().plus(Duration.ofDays(1))
        coEvery { scheduledStatusRepository.getScheduledStatus("old") } returns Result.Success(scheduled("old", at, "Before"))
        coEvery { scheduledStatusRepository.scheduleStatus(any(), any()) } returns Result.Success(scheduled("new", at))
        coEvery { scheduledStatusRepository.deleteScheduledStatus("old") } returns Result.Success(Unit)
        val viewModel = viewModel(SavedStateHandle(mapOf("scheduledId" to "old")))

        assertEquals("Before", viewModel.uiState.value.content)
        assertEquals(at, viewModel.uiState.value.scheduledAt)

        viewModel.onAction(ComposeAction.UpdateContent("After"))
        viewModel.onAction(ComposeAction.Post)

        coVerify { scheduledStatusRepository.scheduleStatus(match { it.content == "After" }, at) }
        coVerify { scheduledStatusRepository.deleteScheduledStatus("old") }
        coVerify(exactly = 0) { draftRepository.saveDraft(any()) }
    }

    @Test
    fun `the edited scheduled post is kept if scheduling the new one fails`() = runTest {
        val at = Instant.now().plus(Duration.ofDays(1))
        coEvery { scheduledStatusRepository.getScheduledStatus("old") } returns Result.Success(scheduled("old", at, "Before"))
        coEvery { scheduledStatusRepository.scheduleStatus(any(), any()) } returns Result.Error("boom")
        val viewModel = viewModel(SavedStateHandle(mapOf("scheduledId" to "old")))

        viewModel.onAction(ComposeAction.Post)

        coVerify(exactly = 0) { scheduledStatusRepository.deleteScheduledStatus(any()) }
        assertFalse(viewModel.uiState.value.isPosted)
    }

    @Test
    fun `a resumed draft gets its media, poll and language back`() = runTest {
        coEvery { draftRepository.getDraft(7L) } returns Result.Success(
            DraftEntity(
                id = 7L,
                content = "With pictures",
                mediaIds = "[\"m1\",\"gone\"]",
                createdAt = 0L,
                updatedAt = 0L,
                pollJson = "{\"options\":[\"A\",\"B\"],\"expiresIn\":3600}",
                language = "fr"
            )
        )
        coEvery { statusRepository.getMedia("m1") } returns Result.Success(media("m1", "A cat"))
        coEvery { statusRepository.getMedia("gone") } returns Result.Success(null)

        val viewModel = viewModel(SavedStateHandle(mapOf("draftId" to "7")))

        val state = viewModel.uiState.value
        assertEquals("With pictures", state.content)
        assertEquals(listOf("m1"), state.mediaAttachments.map { it.id })
        assertEquals("A cat", state.mediaAttachments.single().description)
        assertEquals(listOf("A", "B"), state.pollConfig?.options)
        assertEquals("fr", state.language)
        assertEquals(UiText.of(R.string.error_draft_media_missing), state.errorMessage)
        assertFalse(state.isUploadingMedia)
    }

    @Test
    fun `media is sensitive by toggle or content warning, and kept in drafts`() = runTest {
        val at = Instant.now().plus(Duration.ofHours(2))
        coEvery { scheduledStatusRepository.scheduleStatus(any(), any()) } returns Result.Success(scheduled("1", at))
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateContent("Spoiler"))
        assertFalse(viewModel.uiState.value.isMediaSensitive)
        viewModel.onAction(ComposeAction.ToggleContentWarning)
        viewModel.onAction(ComposeAction.UpdateContentWarning("Food"))
        assertTrue(viewModel.uiState.value.isMediaSensitive)

        viewModel.onAction(ComposeAction.UpdateContentWarning(""))
        viewModel.onAction(ComposeAction.ToggleSensitive)
        viewModel.onAction(ComposeAction.SaveDraftNow)
        coVerify { draftRepository.saveDraft(match { it.sensitive }) }

        viewModel.onAction(ComposeAction.SetSchedule(at))
        viewModel.onAction(ComposeAction.Post)
        coVerify { scheduledStatusRepository.scheduleStatus(match { it.sensitive }, at) }
    }

    @Test
    fun `alt text is saved on the server`() = runTest {
        coEvery { draftRepository.getDraft(7L) } returns Result.Success(
            DraftEntity(id = 7L, content = "", mediaIds = "[\"m1\"]", createdAt = 0L, updatedAt = 0L)
        )
        coEvery { statusRepository.getMedia("m1") } returns Result.Success(media("m1", null))
        coEvery { statusRepository.updateMediaDescription("m1", "A dog") } returns Result.Success(media("m1", "A dog"))
        val viewModel = viewModel(SavedStateHandle(mapOf("draftId" to "7")))

        viewModel.onAction(ComposeAction.UpdateMediaDescription("m1", "A dog"))

        coVerify { statusRepository.updateMediaDescription("m1", "A dog") }
        assertEquals("A dog", viewModel.uiState.value.mediaAttachments.single().description)
    }

    @Test
    fun `an own post is edited with its source text, alt texts and without drafts`() = runTest {
        val original = ownStatus(media = listOf(media("m1", "Old alt")))
        coEvery { statusRepository.getStatus("42") } returns Result.Success(original)
        coEvery { statusRepository.getStatusSource("42") } returns
            Result.Success(StatusSource(id = "42", text = "Plain @text", spoilerText = "CW"))
        coEvery { statusRepository.editStatus(any(), any(), any()) } returns Result.Success(original)
        val viewModel = viewModel(SavedStateHandle(mapOf("editId" to "42")))

        val loaded = viewModel.uiState.value
        assertEquals("Plain @text", loaded.content)
        assertEquals("CW", loaded.contentWarning)
        assertEquals(StatusVisibility.UNLISTED, loaded.visibility)
        assertFalse(loaded.canChangeVisibility)
        assertFalse(loaded.canSchedule)

        viewModel.onAction(ComposeAction.UpdateMediaDescription("m1", "New alt"))
        viewModel.onAction(ComposeAction.UpdateContent("Edited"))
        viewModel.onAction(ComposeAction.Post)

        // Attached media can't be updated on its own; the alt text goes with the edit
        coVerify(exactly = 0) { statusRepository.updateMediaDescription(any(), any()) }
        coVerify {
            statusRepository.editStatus(
                "42",
                match { it.content == "Edited" && it.spoilerText == "CW" && it.mediaIds == listOf("m1") },
                mapOf("m1" to "New alt")
            )
        }
        assertTrue(viewModel.uiState.value.isPosted)

        viewModel.onAction(ComposeAction.Exit)
        coVerify(exactly = 0) { draftRepository.saveDraft(any()) }
    }

    @Test
    fun `editing keeps the end time of a poll, at least 5 minutes`() {
        val now = Instant.parse("2026-10-25T10:00:00Z")

        assertEquals(7200, remainingPollSeconds("2026-10-25T12:00:00Z", now))
        assertEquals(300, remainingPollSeconds("2026-10-25T09:00:00Z", now))
        assertEquals(86400, remainingPollSeconds(null, now))
    }

    private fun ownStatus(media: List<MediaAttachment> = emptyList()) = Status(
        id = "42",
        uri = "https://example.social/statuses/42",
        url = null,
        account = account("own"),
        content = "<p>Plain <a>@text</a></p>",
        createdAt = "2026-10-25T09:00:00Z",
        visibility = com.pfitzer.tootsie.domain.model.StatusVisibility.UNLISTED,
        sensitive = false,
        spoilerText = "CW",
        mediaAttachments = media,
        reblogsCount = 0,
        favouritesCount = 0,
        repliesCount = 0,
        language = "en"
    )

    private fun media(id: String, description: String?) = MediaAttachment(
        id = id,
        type = MediaType.IMAGE,
        url = "https://example.social/$id.png",
        previewUrl = null,
        description = description,
        blurhash = null
    )

    private fun scheduled(id: String, at: Instant, text: String = "") = ScheduledStatus(
        id = id,
        scheduledAt = at,
        text = text,
        spoilerText = null,
        visibility = "unlisted",
        sensitive = false,
        language = null,
        inReplyToId = null,
        poll = null,
        mediaAttachments = emptyList()
    )

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
