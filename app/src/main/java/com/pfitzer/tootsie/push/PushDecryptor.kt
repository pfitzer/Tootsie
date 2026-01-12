package com.pfitzer.tootsie.push

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Push notification payload structure.
 * Based on Mastodon push notification payload spec.
 */
@Serializable
data class PushPayload(
    val notification_id: String? = null,
    val notification_type: String? = null,
    val icon: String? = null,
    val title: String? = null,
    val body: String? = null,
    val preferred_locale: String? = null,
    val access_token: String? = null
)

/**
 * Decryptor for push notification payloads.
 * Handles both encrypted and plaintext push messages.
 *
 * MVP: Simplified decryption - accepts plaintext JSON payloads.
 * Many Mastodon instances support unencrypted push for simplicity.
 * Full RFC 8291 Web Push encryption can be added later.
 */
@Singleton
class PushDecryptor @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Decrypt push message payload.
     *
     * MVP implementation:
     * - Tries parsing as plain JSON first
     * - Falls back to base64 decoding
     * - Full RFC 8291 encryption support can be added later
     *
     * @param encryptedData Raw message bytes
     * @param privateKey VAPID private key (optional, for future encryption support)
     * @param authSecret Auth secret (optional, for future encryption support)
     * @return Decoded PushPayload or null if decryption fails
     */
    fun decrypt(encryptedData: ByteArray, privateKey: String?, authSecret: String?): PushPayload? {
        return try {
            // Try parsing as plain JSON first
            val jsonString = String(encryptedData, Charsets.UTF_8)
            json.decodeFromString<PushPayload>(jsonString)
        } catch (e: Exception) {
            // If not plain JSON, try base64 decode
            try {
                val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
                val jsonString = String(decoded, Charsets.UTF_8)
                json.decodeFromString<PushPayload>(jsonString)
            } catch (e2: Exception) {
                // Both attempts failed
                null
            }
        }
    }

    // TODO: Future enhancement - full RFC 8291 Web Push encryption
    // Requires implementing:
    // - HKDF (HMAC-based Key Derivation Function)
    // - AES-128-GCM decryption
    // - Proper key derivation from VAPID private key and auth secret
    // Reference: https://datatracker.ietf.org/doc/html/rfc8291
    //
    // fun decryptWebPush(
    //     encryptedData: ByteArray,
    //     privateKey: String,
    //     authSecret: String
    // ): PushPayload {
    //     // Derive shared secret using ECDH
    //     // Derive encryption key using HKDF
    //     // Decrypt using AES-128-GCM
    //     // Return decoded payload
    // }
}
