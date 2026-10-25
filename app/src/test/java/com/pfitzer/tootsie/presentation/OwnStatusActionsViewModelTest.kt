package com.pfitzer.tootsie.presentation

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.data.local.entities.DraftPoll
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.DeletedStatus
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.Poll
import com.pfitzer.tootsie.domain.model.PollOption
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusVisibility
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.presentation.util.UiText
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OwnStatusActionsViewModelTest {

    private val statusRepository = mockk<StatusRepository>()
    private val draftRepository = mockk<DraftRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = OwnStatusActionsViewModel(statusRepository, draftRepository)

    @Test
    fun `delete removes the media too`() = runTest {
        val status = status()
        coEvery { statusRepository.deleteStatus("1", deleteMedia = true) } returns Result.Success(DeletedStatus(status, "Hi"))

        viewModel().delete(status)

        coVerify { statusRepository.deleteStatus("1", deleteMedia = true) }
    }

    @Test
    fun `redraft keeps the media and opens a draft with the source text`() = runTest {
        val status = status()
        coEvery { statusRepository.deleteStatus("1", deleteMedia = false) } returns
            Result.Success(DeletedStatus(status, "Hi @alice #tag"))
        val draft = slot<DraftEntity>()
        coEvery { draftRepository.saveDraft(capture(draft)) } returns Result.Success(9L)
        val viewModel = viewModel()

        viewModel.deleteAndRedraft(status)

        assertEquals(OwnStatusEvent.Redraft(9L), viewModel.events.first())
        with(draft.captured) {
            assertEquals("Hi @alice #tag", content)
            assertEquals("[\"m1\"]", mediaIds)
            assertEquals("unlisted", visibility)
            assertEquals("CW", spoilerText)
            assertEquals("r1", inReplyToId)
            assertEquals("de", language)
            // The poll has ended, so the redraft gets the default duration
            assertEquals(
                DraftPoll(options = listOf("A", "B"), expiresIn = 86400, multiple = true),
                Json.decodeFromString<DraftPoll>(pollJson!!)
            )
        }
    }

    @Test
    fun `a failed delete is reported and creates no draft`() = runTest {
        coEvery { statusRepository.deleteStatus(any(), any()) } returns Result.Error("boom")
        val viewModel = viewModel()

        viewModel.deleteAndRedraft(status())

        assertEquals(OwnStatusEvent.Error(UiText.of(R.string.error_delete_post)), viewModel.events.first())
        coVerify(exactly = 0) { draftRepository.saveDraft(any()) }
    }

    private fun status() = Status(
        id = "1",
        uri = "https://example.social/statuses/1",
        url = null,
        account = Account(
            id = "own",
            username = "own",
            acct = "own",
            displayName = "Own",
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
        ),
        content = "<p>Hi <a>@alice</a> <a>#tag</a></p>",
        createdAt = "2026-10-25T09:00:00Z",
        visibility = StatusVisibility.UNLISTED,
        sensitive = true,
        spoilerText = "CW",
        mediaAttachments = listOf(MediaAttachment(id = "m1", type = MediaType.IMAGE, url = "", previewUrl = null)),
        reblogsCount = 0,
        favouritesCount = 0,
        repliesCount = 0,
        poll = Poll(
            id = "p",
            expiresAt = "2026-10-20T00:00:00Z",
            expired = true,
            multiple = true,
            votesCount = 0,
            votersCount = null,
            voted = false,
            options = listOf(PollOption("A"), PollOption("B"))
        ),
        inReplyToId = "r1",
        language = "de"
    )
}
