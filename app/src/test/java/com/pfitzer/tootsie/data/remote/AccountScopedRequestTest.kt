package com.pfitzer.tootsie.data.remote

import android.util.Log
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.interceptors.AuthInterceptor
import com.pfitzer.tootsie.data.remote.interceptors.HEADER_INSTANCE
import com.pfitzer.tootsie.data.remote.interceptors.InstanceInterceptor
import com.pfitzer.tootsie.data.remote.interceptors.InstanceUrlProvider
import com.pfitzer.tootsie.data.remote.interceptors.TokenProvider
import com.pfitzer.tootsie.data.repository.PushRepositoryImpl
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountScopedRequestTest {

    private val tokenProvider = mockk<TokenProvider> { every { getTokenSync() } returns "active-token" }
    private val instanceUrlProvider = mockk<InstanceUrlProvider> {
        every { getInstanceUrlSync() } returns "active.social"
    }

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `interceptors use the active account by default`() {
        val sent = proceed(Request.Builder().url("https://placeholder/api/v1/notifications").build())

        assertEquals("active.social", sent.url.host)
        assertEquals("Bearer active-token", sent.header("Authorization"))
    }

    @Test
    fun `interceptors keep explicit account credentials and strip the instance header`() {
        val request = Request.Builder()
            .url("https://placeholder/api/v1/push/subscription")
            .header("Authorization", "Bearer other-token")
            .header(HEADER_INSTANCE, "other.social")
            .build()

        val sent = proceed(request)

        assertEquals("other.social", sent.url.host)
        assertEquals("Bearer other-token", sent.header("Authorization"))
        assertNull(sent.header(HEADER_INSTANCE))
    }

    @Test
    fun `push repository calls the API with the given account's credentials`() = runTest {
        val api = mockk<MastodonApiService>(relaxed = true)
        val credentialsProvider = mockk<AccountCredentialsProvider>()
        coEvery { credentialsProvider.forAccount("account-b") } returns
            AccountCredentials(authorization = "Bearer token-b", instance = "b.social")
        val repository = PushRepositoryImpl(api, mockk<PreferencesManager>(relaxed = true), credentialsProvider)

        repository.unsubscribeFromPush("account-b")

        coVerify { api.deletePushSubscription(authorization = "Bearer token-b", instance = "b.social") }
    }

    @Test
    fun `push repository refuses accounts that are not logged in`() = runTest {
        val api = mockk<MastodonApiService>(relaxed = true)
        val credentialsProvider = mockk<AccountCredentialsProvider>()
        coEvery { credentialsProvider.forAccount(any()) } returns null
        val repository = PushRepositoryImpl(api, mockk<PreferencesManager>(relaxed = true), credentialsProvider)

        val result = repository.unsubscribeFromPush("gone")

        assertTrue(result is Result.Error)
        // Must not fall back to the active account's token
        coVerify(exactly = 0) { api.deletePushSubscription(any(), any()) }
    }

    /** Runs a request through InstanceInterceptor and AuthInterceptor and returns what is sent. */
    private fun proceed(request: Request): Request {
        val instanceInterceptor = InstanceInterceptor(instanceUrlProvider)
        val authInterceptor = AuthInterceptor(tokenProvider)
        val sent = slot<Request>()

        val networkChain = mockk<Interceptor.Chain> {
            every { proceed(capture(sent)) } answers { response(sent.captured) }
        }
        val authChain = mockk<Interceptor.Chain> {
            val afterInstance = slot<Request>()
            every { proceed(capture(afterInstance)) } answers {
                every { networkChain.request() } returns afterInstance.captured
                authInterceptor.intercept(networkChain)
            }
        }
        every { authChain.request() } returns request
        instanceInterceptor.intercept(authChain)
        return sent.captured
    }

    private fun response(request: Request) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .build()
}
