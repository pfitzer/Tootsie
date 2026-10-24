package com.pfitzer.tootsie.data.local.crypto

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage used for credentials before [CredentialCipher] was introduced.
 */
interface LegacySecureStorage {

    /**
     * Delete the legacy storage and its key material. Safe to call if nothing exists.
     */
    fun delete()
}

/**
 * Removes the former androidx.security EncryptedSharedPreferences file and its master key.
 * Its contents are not read: the active account's credentials are restored from Room instead
 * (see [CredentialMigration]), so the deprecated security-crypto library is no longer needed.
 */
@Singleton
class EncryptedSharedPreferencesCleanup @Inject constructor(
    @ApplicationContext private val context: Context
) : LegacySecureStorage {

    companion object {
        private const val PREFS_FILE = "tootsie_secure_prefs"
        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    override fun delete() {
        context.deleteSharedPreferences(PREFS_FILE)

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            keyStore.deleteEntry(MASTER_KEY_ALIAS)
        }
    }
}
