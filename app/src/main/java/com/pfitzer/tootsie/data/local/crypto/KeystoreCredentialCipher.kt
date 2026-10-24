package com.pfitzer.tootsie.data.local.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "KeystoreCredentialCipher"

/**
 * [CredentialCipher] using AES-256-GCM with a non-exportable key held in the Android Keystore.
 *
 * Output format: `enc1:` + Base64(12-byte IV || ciphertext || 16-byte GCM tag).
 * Keystore operations are blocking; call from Dispatchers.IO.
 */
@Singleton
class KeystoreCredentialCipher @Inject constructor() : CredentialCipher {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "tootsie_credentials_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFIX = "enc1:"
        private const val IV_SIZE = 12
        private const val TAG_SIZE_BITS = 128
    }

    private val key: SecretKey by lazy { getOrCreateKey() }

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    override fun decrypt(value: String): String? {
        if (!isEncrypted(value)) return value

        return try {
            val bytes = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(TAG_SIZE_BITS, bytes, 0, IV_SIZE)
            )
            String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Failed to decrypt credential", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Malformed encrypted credential", e)
            null
        }
    }

    override fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
