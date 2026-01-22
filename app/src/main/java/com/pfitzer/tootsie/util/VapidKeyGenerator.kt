package com.pfitzer.tootsie.util

import android.util.Base64
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.spec.ECGenParameterSpec

/**
 * VAPID key pair for Web Push encryption.
 */
data class VapidKeyPair(
    val publicKey: String,   // base64url encoded
    val privateKey: String   // base64url encoded
)

/**
 * Utility for generating VAPID (Voluntary Application Server Identification) keys
 * used for Web Push notifications following RFC 8292.
 *
 * Uses BouncyCastle cryptography provider for P-256 elliptic curve key generation.
 */
object VapidKeyGenerator {

    @Volatile
    private var providerRegistered = false

    /**
     * Ensures the BouncyCastle provider is registered before use.
     * Uses double-checked locking for thread safety and performance.
     */
    private fun ensureProviderRegistered() {
        if (!providerRegistered) {
            synchronized(this) {
                if (!providerRegistered) {
                    // Remove any existing BC provider to avoid conflicts with Android's bundled providers
                    Security.removeProvider("BC")
                    // Add BouncyCastle provider
                    Security.addProvider(BouncyCastleProvider())
                    providerRegistered = true
                }
            }
        }
    }

    /**
     * Generates a new VAPID key pair for Web Push encryption.
     * Uses P-256 (secp256r1) elliptic curve as specified by RFC 8292.
     *
     * @return VapidKeyPair with base64url-encoded public and private keys
     */
    fun generateKeyPair(): VapidKeyPair {
        ensureProviderRegistered()

        val keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC")
        val ecSpec = ECGenParameterSpec("secp256r1") // P-256 curve for Web Push
        keyPairGenerator.initialize(ecSpec, SecureRandom())

        val keyPair = keyPairGenerator.generateKeyPair()

        // Extract raw bytes
        val publicKeyBytes = (keyPair.public as ECPublicKey)
            .q.getEncoded(false) // Uncompressed format
        val privateKeyBytes = (keyPair.private as ECPrivateKey)
            .d.toByteArray()

        return VapidKeyPair(
            publicKey = base64UrlEncode(publicKeyBytes),
            privateKey = base64UrlEncode(privateKeyBytes)
        )
    }

    /**
     * Generates a random auth secret for Web Push encryption.
     * The auth secret is a 16-byte random value.
     *
     * @return Base64url-encoded auth secret
     */
    fun generateAuthSecret(): String {
        val secret = ByteArray(16)
        SecureRandom().nextBytes(secret)
        return base64UrlEncode(secret)
    }

    /**
     * Encodes bytes to base64url format (URL-safe base64 without padding).
     */
    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
