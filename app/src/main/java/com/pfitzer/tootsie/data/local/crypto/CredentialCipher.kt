package com.pfitzer.tootsie.data.local.crypto

/**
 * Encrypts and decrypts sensitive credentials (OAuth access tokens, instance URL)
 * before they are written to disk.
 */
interface CredentialCipher {

    /**
     * Encrypt [plaintext]. The result carries a version prefix (see [isEncrypted]).
     */
    fun encrypt(plaintext: String): String

    /**
     * Decrypt a value produced by [encrypt].
     * Values without the encryption prefix are legacy plaintext and returned unchanged.
     * Returns null if the value cannot be decrypted (e.g. the Keystore key was lost).
     */
    fun decrypt(value: String): String?

    /**
     * Whether [value] was produced by [encrypt].
     */
    fun isEncrypted(value: String): Boolean
}
