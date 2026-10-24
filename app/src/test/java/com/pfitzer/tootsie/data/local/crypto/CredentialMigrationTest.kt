package com.pfitzer.tootsie.data.local.crypto

import android.util.Log
import com.pfitzer.tootsie.data.local.TootsieDatabase
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.local.entities.AccountEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CredentialMigrationTest {

    /** Reversible test cipher: "enc1:" + reversed plaintext. */
    private class FakeCipher : CredentialCipher {
        override fun encrypt(plaintext: String) = "enc1:" + plaintext.reversed()
        override fun decrypt(value: String) =
            if (isEncrypted(value)) value.removePrefix("enc1:").reversed() else value
        override fun isEncrypted(value: String) = value.startsWith("enc1:")
    }

    private val cipher = FakeCipher()
    private val database = mockk<TootsieDatabase>(relaxed = true)
    private val accountDao = mockk<AccountDao>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val legacySecureStorage = mockk<LegacySecureStorage>(relaxed = true)

    private lateinit var migration: CredentialMigration

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        migration = CredentialMigration(
            database, accountDao, preferencesManager, cipher, legacySecureStorage
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `encrypts plaintext tokens of all accounts`() = runTest {
        val first = account("1", token = "token-a", isActive = true)
        val second = account("2", token = "token-b")
        coEvery { accountDao.getAllLoggedInAccounts() } returns listOf(first, second)
        coEvery { preferencesManager.getAccessToken() } returns "token-a"
        coEvery { preferencesManager.getInstanceUrl() } returns "https://example.social"

        migration.migrate()

        coVerify { accountDao.updateAccount(first.copy(accessToken = cipher.encrypt("token-a"))) }
        coVerify { accountDao.updateAccount(second.copy(accessToken = cipher.encrypt("token-b"))) }
        verify { database.openHelper.writableDatabase.execSQL("VACUUM") }
    }

    @Test
    fun `leaves already encrypted tokens untouched`() = runTest {
        val encrypted = account("1", token = cipher.encrypt("token-a"), isActive = true)
        coEvery { accountDao.getAllLoggedInAccounts() } returns listOf(encrypted)
        coEvery { preferencesManager.getAccessToken() } returns "token-a"
        coEvery { preferencesManager.getInstanceUrl() } returns "https://example.social"

        migration.migrate()

        coVerify(exactly = 0) { accountDao.updateAccount(any()) }
        verify(exactly = 0) { database.openHelper }
    }

    @Test
    fun `restores active credentials from Room when missing in preferences`() = runTest {
        // Upgrade from EncryptedSharedPreferences: the old store is not read anymore
        val active = account("1", token = "token-a", isActive = true)
        coEvery { accountDao.getAllLoggedInAccounts() } returns listOf(active)
        coEvery { accountDao.getActiveAccount() } returns
            active.copy(accessToken = cipher.encrypt("token-a"))
        coEvery { preferencesManager.getAccessToken() } returns null
        coEvery { preferencesManager.getInstanceUrl() } returns null

        migration.migrate()

        coVerify { preferencesManager.saveAccessToken("token-a") }
        coVerify { preferencesManager.saveInstanceUrl("https://example.social") }
    }

    @Test
    fun `keeps existing preferences credentials`() = runTest {
        coEvery { accountDao.getAllLoggedInAccounts() } returns emptyList()
        coEvery { preferencesManager.getAccessToken() } returns "token-a"
        coEvery { preferencesManager.getInstanceUrl() } returns "https://example.social"

        migration.migrate()

        coVerify(exactly = 0) { accountDao.getActiveAccount() }
        coVerify(exactly = 0) { preferencesManager.saveAccessToken(any()) }
        coVerify(exactly = 0) { preferencesManager.saveInstanceUrl(any()) }
    }

    @Test
    fun `restores nothing when no account is active`() = runTest {
        coEvery { accountDao.getAllLoggedInAccounts() } returns emptyList()
        coEvery { accountDao.getActiveAccount() } returns null
        coEvery { preferencesManager.getAccessToken() } returns null
        coEvery { preferencesManager.getInstanceUrl() } returns null

        migration.migrate()

        coVerify(exactly = 0) { preferencesManager.saveAccessToken(any()) }
        coVerify(exactly = 0) { preferencesManager.saveInstanceUrl(any()) }
    }

    @Test
    fun `deletes legacy storage and continues if that fails`() = runTest {
        val active = account("1", token = "token-a", isActive = true)
        coEvery { accountDao.getAllLoggedInAccounts() } returns listOf(active)
        coEvery { preferencesManager.getAccessToken() } returns "token-a"
        coEvery { preferencesManager.getInstanceUrl() } returns "https://example.social"
        every { legacySecureStorage.delete() } throws IllegalStateException("keystore unavailable")

        migration.migrate()

        verify { legacySecureStorage.delete() }
        val updated = slot<AccountEntity>()
        coVerify { accountDao.updateAccount(capture(updated)) }
        assertEquals(cipher.encrypt("token-a"), updated.captured.accessToken)
    }

    private fun account(id: String, token: String?, isActive: Boolean = false) = AccountEntity(
        id = id,
        username = "user$id",
        acct = "user$id@example.social",
        displayName = "User $id",
        note = "",
        url = "https://example.social/@user$id",
        avatar = "",
        header = "",
        locked = false,
        followersCount = 0,
        followingCount = 0,
        statusesCount = 0,
        createdAt = "2025-01-01T00:00:00Z",
        bot = false,
        accessToken = token,
        instanceUrl = "https://example.social",
        isActive = isActive,
        lastSyncedAt = 0
    )
}
