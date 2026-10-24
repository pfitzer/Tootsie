package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.AsyncRefresh
import com.pfitzer.tootsie.domain.model.AsyncRefreshProgress
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AwaitRemoteRepliesUseCaseTest {

    private val repository = mockk<StatusRepository>()
    private val useCase = AwaitRemoteRepliesUseCase(repository)
    private val refresh = AsyncRefresh(id = "job", retrySeconds = 3)

    private fun progress(finished: Boolean, count: Int?) =
        Result.Success(AsyncRefreshProgress(finished = finished, resultCount = count))

    @Test
    fun `polls respecting retry until finished with new replies`() = runTest {
        coEvery { repository.getAsyncRefresh("job") } returnsMany listOf(
            progress(finished = false, count = 0),
            progress(finished = false, count = 1),
            progress(finished = true, count = 2)
        )

        assertTrue(useCase(refresh))
        coVerify(exactly = 3) { repository.getAsyncRefresh("job") }
        // Waited the retry interval before each poll
        assertEquals(9_000L, currentTime)
    }

    @Test
    fun `finished without new replies needs no reload`() = runTest {
        coEvery { repository.getAsyncRefresh("job") } returns progress(finished = true, count = 0)

        assertFalse(useCase(refresh))
    }

    @Test
    fun `finished without count reloads to be safe`() = runTest {
        coEvery { repository.getAsyncRefresh("job") } returns progress(finished = true, count = null)

        assertTrue(useCase(refresh))
    }

    @Test
    fun `errors stop polling`() = runTest {
        coEvery { repository.getAsyncRefresh("job") } returns Result.Error("Not Found")

        assertFalse(useCase(refresh))
        coVerify(exactly = 1) { repository.getAsyncRefresh("job") }
    }

    @Test
    fun `gives up after the maximum number of attempts`() = runTest {
        coEvery { repository.getAsyncRefresh("job") } returns progress(finished = false, count = 0)

        assertFalse(useCase(refresh))
        coVerify(exactly = AwaitRemoteRepliesUseCase.MAX_ATTEMPTS) { repository.getAsyncRefresh("job") }
    }
}
