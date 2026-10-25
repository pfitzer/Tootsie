package com.pfitzer.tootsie.presentation.screens.scheduled

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.ScheduledStatus
import com.pfitzer.tootsie.domain.repository.ScheduledStatusRepository
import com.pfitzer.tootsie.presentation.util.UiText
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduledPostsViewModelTest {

    private val repository = mockk<ScheduledStatusRepository>()
    private val now = Instant.now()
    private val a = scheduled("a", now.plus(Duration.ofHours(1)))
    private val b = scheduled("b", now.plus(Duration.ofHours(2)))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { repository.getScheduledStatuses() } returns Result.Success(listOf(a, b))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loadedViewModel() = ScheduledPostsViewModel(repository).apply {
        onAction(ScheduledPostsAction.Load)
    }

    @Test
    fun `rescheduled post moves to its new place in the list`() = runTest {
        val later = now.plus(Duration.ofHours(3))
        coEvery { repository.reschedule("a", later) } returns Result.Success(a.copy(scheduledAt = later))
        val viewModel = loadedViewModel()

        viewModel.onAction(ScheduledPostsAction.Reschedule("a", later))

        assertEquals(listOf("b", "a"), viewModel.uiState.value.scheduledPosts.map { it.id })
        assertTrue(viewModel.uiState.value.processingIds.isEmpty())
    }

    @Test
    fun `a time less than 5 minutes ahead is rejected`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onAction(ScheduledPostsAction.Reschedule("a", Instant.now().plus(Duration.ofMinutes(1))))

        assertEquals(UiText.of(R.string.error_schedule_too_soon), viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { repository.reschedule(any(), any()) }
    }

    @Test
    fun `deleted post is removed, a failed delete keeps it`() = runTest {
        coEvery { repository.deleteScheduledStatus("a") } returns Result.Success(Unit)
        coEvery { repository.deleteScheduledStatus("b") } returns Result.Error("boom")
        val viewModel = loadedViewModel()

        viewModel.onAction(ScheduledPostsAction.Delete("a"))
        viewModel.onAction(ScheduledPostsAction.Delete("b"))

        assertEquals(listOf("b"), viewModel.uiState.value.scheduledPosts.map { it.id })
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.processingIds.isEmpty())
    }

    private fun scheduled(id: String, at: Instant) = ScheduledStatus(
        id = id,
        scheduledAt = at,
        text = id,
        spoilerText = null,
        visibility = "public",
        sensitive = false,
        language = null,
        inReplyToId = null,
        poll = null,
        mediaAttachments = emptyList()
    )
}
