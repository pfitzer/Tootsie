package com.pfitzer.tootsie.push

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PushDecryptor"

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
 * Decrypts Web Push messages encrypted per RFC 8291 with the aes128gcm content coding
 * (RFC 8188).
 *
 * UnifiedPush only forwards the message body, not HTTP headers, so the legacy "aesgcm"
 * scheme (salt and server key in headers) can't be decrypted. Subscriptions are therefore
 * created with `standard = true`, which makes Mastodon (≥ 4.4) use aes128gcm.
 */
@Singleton
class PushDecryptor @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Decrypt and parse a push message.
     *
     * @param message Raw message body as forwarded by the UnifiedPush distributor
     * @param publicKey Subscription public key (base64url, uncompressed P-256 point)
     * @param privateKey Subscription private key (base64url, scalar)
     * @param authSecret Subscription auth secret (base64url, 16 bytes)
     * @return The payload, or null if the message can't be decrypted or parsed
     */
    fun decrypt(message: ByteArray, publicKey: String?, privateKey: String?, authSecret: String?): PushPayload? {
        if (publicKey == null || privateKey == null || authSecret == null) {
            Log.w(TAG, "Missing subscription keys, can't decrypt push message")
            return null
        }
        return try {
            val plaintext = decryptAes128gcm(
                body = message,
                receiverPublicKey = base64UrlDecode(publicKey),
                receiverPrivateKey = base64UrlDecode(privateKey),
                authSecret = base64UrlDecode(authSecret)
            )
            json.decodeFromString<PushPayload>(String(plaintext, Charsets.UTF_8))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decrypt push message (${message.size} bytes)", e)
            null
        }
    }

    companion object {
        private const val HEADER_FIXED_SIZE = 16 + 4 + 1 // salt + record size + key id length
        private const val TAG_SIZE = 16
        private const val UNCOMPRESSED_POINT_SIZE = 65

        /**
         * RFC 8291 section 3.4 + RFC 8188 aes128gcm decryption. Returns the plaintext with
         * padding removed.
         *
         * @throws IllegalArgumentException if the message is malformed
         * @throws java.security.GeneralSecurityException if decryption fails
         */
        internal fun decryptAes128gcm(
            body: ByteArray,
            receiverPublicKey: ByteArray,
            receiverPrivateKey: ByteArray,
            authSecret: ByteArray
        ): ByteArray {
            require(body.size > HEADER_FIXED_SIZE) { "Message too short" }
            val header = ByteBuffer.wrap(body)
            val salt = ByteArray(16).also { header.get(it) }
            val recordSize = header.int
            val keyIdLength = header.get().toInt() and 0xFF
            require(keyIdLength == UNCOMPRESSED_POINT_SIZE) {
                "Unexpected key id length $keyIdLength (not an aes128gcm Web Push message?)"
            }
            require(recordSize > TAG_SIZE + 1) { "Invalid record size $recordSize" }
            val senderPublicKey = ByteArray(keyIdLength).also { header.get(it) }
            val ciphertext = body.copyOfRange(HEADER_FIXED_SIZE + keyIdLength, body.size)
            require(ciphertext.isNotEmpty()) { "Empty ciphertext" }

            // Combine shared secret and auth secret (RFC 8291 section 3.3)
            val params = p256Parameters()
            val ecdhSecret = ecdh(receiverPrivateKey, senderPublicKey, params)
            val keyInfo = "WebPush: info".toByteArray() + 0 + receiverPublicKey + senderPublicKey
            val ikm = hkdf(salt = authSecret, ikm = ecdhSecret, info = keyInfo, length = 32)

            // Derive content encryption key and nonce (RFC 8188 section 2.2/2.3)
            val cek = hkdf(salt, ikm, "Content-Encoding: aes128gcm".toByteArray() + 0, 16)
            val baseNonce = hkdf(salt, ikm, "Content-Encoding: nonce".toByteArray() + 0, 12)

            val plaintext = java.io.ByteArrayOutputStream()
            val records = ciphertext.toList().chunked(recordSize).map { it.toByteArray() }
            records.forEachIndexed { index, record ->
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(cek, "AES"),
                    GCMParameterSpec(TAG_SIZE * 8, recordNonce(baseNonce, index.toLong()))
                )
                val padded = cipher.doFinal(record)
                plaintext.write(removePadding(padded, isLast = index == records.lastIndex))
            }
            return plaintext.toByteArray()
        }

        /** Strip trailing zero padding and the delimiter: 0x02 for the last record, else 0x01. */
        private fun removePadding(padded: ByteArray, isLast: Boolean): ByteArray {
            var end = padded.size - 1
            while (end >= 0 && padded[end] == 0.toByte()) end--
            require(end >= 0) { "Record without padding delimiter" }
            val expected: Byte = if (isLast) 2 else 1
            require(padded[end] == expected) { "Invalid padding delimiter ${padded[end]}" }
            return padded.copyOfRange(0, end)
        }

        /** Nonce for record [sequence]: base nonce XOR sequence number (RFC 8188 section 2.3). */
        private fun recordNonce(baseNonce: ByteArray, sequence: Long): ByteArray {
            val nonce = baseNonce.copyOf()
            for (i in 0 until 8) {
                val shift = 8 * i
                nonce[nonce.size - 1 - i] = (nonce[nonce.size - 1 - i].toInt() xor ((sequence shr shift).toInt() and 0xFF)).toByte()
            }
            return nonce
        }

        private fun p256Parameters(): ECParameterSpec =
            AlgorithmParameters.getInstance("EC").apply { init(ECGenParameterSpec("secp256r1")) }
                .getParameterSpec(ECParameterSpec::class.java)

        private fun ecdh(privateScalar: ByteArray, publicPoint: ByteArray, params: ECParameterSpec): ByteArray {
            require(publicPoint.size == UNCOMPRESSED_POINT_SIZE && publicPoint[0] == 4.toByte()) {
                "Sender key is not an uncompressed P-256 point"
            }
            val keyFactory = KeyFactory.getInstance("EC")
            // BigInteger(1, …) accepts the stored scalar with or without a leading zero byte
            val privateKey = keyFactory.generatePrivate(ECPrivateKeySpec(BigInteger(1, privateScalar), params))
            val point = ECPoint(
                BigInteger(1, publicPoint.copyOfRange(1, 33)),
                BigInteger(1, publicPoint.copyOfRange(33, 65))
            )
            val publicKey = keyFactory.generatePublic(ECPublicKeySpec(point, params)) as ECPublicKey
            return KeyAgreement.getInstance("ECDH").run {
                init(privateKey)
                doPhase(publicKey, true)
                generateSecret()
            }
        }

        /** HKDF-SHA256 (RFC 5869), extract and expand for outputs up to 32 bytes. */
        private fun hkdf(salt: ByteArray, ikm: ByteArray, info: ByteArray, length: Int): ByteArray {
            require(length <= 32)
            val prk = hmacSha256(salt, ikm)
            return hmacSha256(prk, info + 1).copyOf(length)
        }

        private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray =
            Mac.getInstance("HmacSHA256").run {
                init(SecretKeySpec(key, "HmacSHA256"))
                doFinal(data)
            }

        private fun base64UrlDecode(value: String): ByteArray =
            Base64.getUrlDecoder().decode(value.trimEnd('='))
    }
}
