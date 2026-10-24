package com.pfitzer.tootsie.push

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Base64

/**
 * RFC 8291 section 5 example on the device's crypto providers (Conscrypt), which differ
 * from the JVM used by unit tests.
 */
@RunWith(AndroidJUnit4::class)
class PushDecryptorInstrumentedTest {

    @Test
    fun decryptsRfc8291ExampleOnDevice() {
        val decoder = Base64.getUrlDecoder()
        val plaintext = PushDecryptor.decryptAes128gcm(
            body = decoder.decode(
                "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27ml" +
                    "mlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPT" +
                    "pK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN"
            ),
            receiverPublicKey = decoder.decode(
                "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4"
            ),
            receiverPrivateKey = decoder.decode("q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94"),
            authSecret = decoder.decode("BTBZMqHH6r4Tts7J_aSIgg")
        )

        assertEquals("When I grow up, I want to be a watermelon", String(plaintext, Charsets.UTF_8))
    }
}
