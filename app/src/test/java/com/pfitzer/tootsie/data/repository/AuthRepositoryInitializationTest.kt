package com.pfitzer.tootsie.data.repository

import android.util.Log
import com.pfitzer.tootsie.data.local.crypto.CredentialMigration
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.remote.interceptors.InstanceUrlProvider
import com.pfitzer.tootsie.data.remote.interceptors.TokenProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthRepositoryInitializationTest {

    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val tokenProvider = mockk<TokenProvider>(relaxed = true)
    private val instanceUrlProvider = mockk<InstanceUrlProvider>(relaxed = true)
    private val credentialMigration = mockk<CredentialMigration>(relaxed = true)

    private val repository = AuthRepositoryImpl(
        authApiService = mockk(relaxed = true),
        mastodonApiService = mockk(relaxed = true),
        accountDao = mockk(relaxed = true),
        preferencesManager = preferencesManager,
        instanceUrlProvider = instanceUrlProvider,
        tokenProvider = tokenProvider,
        credentialCipher = mockk(relaxed = true),
        credentialMigration = credentialMigration
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `ensureInitialized loads stored credentials into the providers`() = runTest {
        coEvery { preferencesManager.getAccessToken() } returns "token"
        coEvery { preferencesManager.getInstanceUrl() } returns "example.social"

        repository.ensureInitialized()

        coVerify { credentialMigration.migrate() }
        verify { tokenProvider.setToken("token") }
        verify { instanceUrlProvider.setInstanceUrl("example.social") }
    }

    @Test
    fun `ensureInitialized runs only once, also for concurrent callers`() = runTest {
        coEvery { preferencesManager.getAccessToken() } coAnswers {
            delay(100)
            "token"
        }

        List(5) { async { repository.ensureInitialized() } }.awaitAll()
        repository.ensureInitialized()

        coVerify(exactly = 1) { credentialMigration.migrate() }
        verify(exactly = 1) { tokenProvider.setToken("token") }
    }
}
