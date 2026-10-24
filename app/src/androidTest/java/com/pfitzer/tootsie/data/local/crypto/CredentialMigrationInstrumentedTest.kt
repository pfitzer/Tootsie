package com.pfitzer.tootsie.data.local.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pfitzer.tootsie.data.local.TootsieDatabase
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.local.entities.AccountEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator

/**
 * End-to-end upgrade path from EncryptedSharedPreferences + plaintext Room tokens,
 * using the real Keystore cipher, DataStore and an in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class CredentialMigrationInstrumentedTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val cipher = KeystoreCredentialCipher()
    private val preferencesManager = PreferencesManager(context, cipher)
    private lateinit var database: TootsieDatabase

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, TootsieDatabase::class.java).build()
        preferencesManager.clearAll()
    }

    @After
    fun tearDown() = runBlocking {
        preferencesManager.clearAll()
        database.close()
    }

    @Test
    fun upgrade_keepsAllAccountsLoggedInAndRemovesPlaintext() = runBlocking {
        // State of an installation before the migration
        database.accountDao().insertAccounts(
            listOf(
                account("1", token = "token-active", isActive = true),
                account("2", token = "token-other", isActive = false)
            )
        )
        createLegacyEncryptedPrefs()

        CredentialMigration(
            database,
            database.accountDao(),
            preferencesManager,
            cipher,
            EncryptedSharedPreferencesCleanup(context)
        ).migrate()

        // Active credentials restored, readable through PreferencesManager
        assertEquals("token-active", preferencesManager.getAccessToken())
        assertEquals("https://example.social", preferencesManager.getInstanceUrl())

        // All Room tokens encrypted and still decryptable (account switching keeps working)
        val accounts = database.accountDao().getAllLoggedInAccounts().associateBy { it.id }
        accounts.values.forEach { assertTrue(cipher.isEncrypted(requireNotNull(it.accessToken))) }
        assertEquals("token-active", cipher.decrypt(requireNotNull(accounts.getValue("1").accessToken)))
        assertEquals("token-other", cipher.decrypt(requireNotNull(accounts.getValue("2").accessToken)))

        // Legacy storage removed
        assertFalse(legacyPrefsFile().exists())
        assertFalse(androidKeyStore().containsAlias(LEGACY_MASTER_KEY_ALIAS))

        // DataStore file contains no plaintext credentials
        val dataStoreFile = File(context.filesDir, "datastore/tootsie_preferences.preferences_pb")
        val raw = dataStoreFile.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(raw.contains("token-active"))
        assertFalse(raw.contains("example.social"))
    }

    @Test
    fun migrate_isIdempotent() = runBlocking {
        database.accountDao().insertAccount(account("1", token = "token-active", isActive = true))
        val migration = CredentialMigration(
            database,
            database.accountDao(),
            preferencesManager,
            cipher,
            EncryptedSharedPreferencesCleanup(context)
        )

        migration.migrate()
        val afterFirstRun = database.accountDao().getAccountById("1")?.accessToken
        migration.migrate()

        assertEquals(afterFirstRun, database.accountDao().getAccountById("1")?.accessToken)
        assertEquals("token-active", preferencesManager.getAccessToken())
    }

    private fun createLegacyEncryptedPrefs() {
        context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)
            .edit().putString("opaque", "ciphertext").commit()
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    LEGACY_MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
        assertTrue(legacyPrefsFile().exists())
    }

    private fun legacyPrefsFile() =
        File(context.dataDir, "shared_prefs/$LEGACY_PREFS_FILE.xml")

    private fun androidKeyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun account(id: String, token: String, isActive: Boolean) = AccountEntity(
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
        isActive = isActive
    )

    private companion object {
        const val LEGACY_PREFS_FILE = "tootsie_secure_prefs"
        const val LEGACY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
    }
}
