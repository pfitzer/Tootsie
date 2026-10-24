package com.pfitzer.tootsie.push

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

/**
 * Uses the example from RFC 8291 section 5 (keys, auth secret and encrypted body).
 */
class PushDecryptorTest {

    private val body = b64(
        "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27ml" +
            "mlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPT" +
            "pK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN"
    )
    private val authSecret = b64("BTBZMqHH6r4Tts7J_aSIgg")
    private val receiverPrivateKey = b64("q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94")
    private val receiverPublicKey = b64(
        "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4"
    )

    @Test
    fun `decrypts the RFC 8291 example message`() {
        val plaintext = PushDecryptor.decryptAes128gcm(body, receiverPublicKey, receiverPrivateKey, authSecret)

        assertEquals("When I grow up, I want to be a watermelon", String(plaintext, Charsets.UTF_8))
    }

    @Test
    fun `accepts a private key with a leading zero byte`() {
        // VapidKeyGenerator stores BigInteger.toByteArray(), which may have a sign byte
        val withSignByte = byteArrayOf(0) + receiverPrivateKey
        val plaintext = PushDecryptor.decryptAes128gcm(body, receiverPublicKey, withSignByte, authSecret)

        assertArrayEquals("When I grow up, I want to be a watermelon".toByteArray(), plaintext)
    }

    @Test
    fun `fails with a wrong auth secret`() {
        val wrongSecret = authSecret.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThrows(Exception::class.java) {
            PushDecryptor.decryptAes128gcm(body, receiverPublicKey, receiverPrivateKey, wrongSecret)
        }
    }

    @Test
    fun `fails with a tampered ciphertext`() {
        val tampered = body.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }

        assertThrows(Exception::class.java) {
            PushDecryptor.decryptAes128gcm(tampered, receiverPublicKey, receiverPrivateKey, authSecret)
        }
    }

    @Test
    fun `rejects plaintext JSON instead of accepting it`() {
        val plaintextJson = """{"notification_id":"1","title":"t","body":"b"}""".toByteArray()

        assertThrows(IllegalArgumentException::class.java) {
            PushDecryptor.decryptAes128gcm(plaintextJson, receiverPublicKey, receiverPrivateKey, authSecret)
        }
    }

    private fun b64(value: String): ByteArray = Base64.getUrlDecoder().decode(value)
}
