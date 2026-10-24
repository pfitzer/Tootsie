package com.pfitzer.tootsie.data.local.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreCredentialCipherTest {

    private val cipher = KeystoreCredentialCipher()

    @Test
    fun encryptAndDecrypt_roundTrips() {
        val encrypted = cipher.encrypt("secret-token")

        assertTrue(cipher.isEncrypted(encrypted))
        assertFalse(encrypted.contains("secret-token"))
        assertEquals("secret-token", cipher.decrypt(encrypted))
    }

    @Test
    fun encrypt_usesRandomIv() {
        assertNotEquals(cipher.encrypt("secret-token"), cipher.encrypt("secret-token"))
    }

    @Test
    fun decrypt_returnsLegacyPlaintextUnchanged() {
        assertFalse(cipher.isEncrypted("plain-token"))
        assertEquals("plain-token", cipher.decrypt("plain-token"))
    }

    @Test
    fun decrypt_returnsNullForTamperedValue() {
        val encrypted = cipher.encrypt("secret-token")
        val tampered = encrypted.dropLast(2) + if (encrypted.takeLast(2) == "AA") "BB" else "AA"

        assertNull(cipher.decrypt(tampered))
    }
}
