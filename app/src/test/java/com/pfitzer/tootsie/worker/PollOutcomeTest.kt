package com.pfitzer.tootsie.worker

import com.pfitzer.tootsie.util.Result
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class PollOutcomeTest {

    @Test
    fun `401 and 403 stop polling`() {
        assertEquals(PollOutcome.AUTH_FAILED, pollOutcomeOf(httpError(401)))
        assertEquals(PollOutcome.AUTH_FAILED, pollOutcomeOf(httpError(403)))
    }

    @Test
    fun `rate limit, server errors and network errors are retried`() {
        assertEquals(PollOutcome.RETRY, pollOutcomeOf(httpError(429)))
        assertEquals(PollOutcome.RETRY, pollOutcomeOf(httpError(503)))
        assertEquals(PollOutcome.RETRY, pollOutcomeOf(Result.Error("offline", IOException("offline"))))
        assertEquals(PollOutcome.RETRY, pollOutcomeOf(Result.Error("unknown")))
    }

    private fun httpError(code: Int) = Result.Error(
        message = "HTTP $code",
        exception = HttpException(Response.error<Any>(code, "".toResponseBody()))
    )
}
