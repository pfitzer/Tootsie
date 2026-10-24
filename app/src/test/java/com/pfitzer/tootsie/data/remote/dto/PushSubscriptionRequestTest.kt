package com.pfitzer.tootsie.data.remote.dto

import com.pfitzer.tootsie.data.mapper.toDto
import com.pfitzer.tootsie.di.NetworkModule
import com.pfitzer.tootsie.domain.model.PushAlerts
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class PushSubscriptionRequestTest {

    // Same Json configuration as the app's Retrofit converter (does not encode defaults)
    private val json = NetworkModule.provideJson()

    @Test
    fun `request contains data_alerts with every alert, including defaults`() {
        val request = PushSubscriptionRequestDto(
            subscription = SubscriptionKeysDto(
                endpoint = "https://ntfy.example/up",
                keys = KeysDto(p256dh = "key", auth = "auth"),
                standard = true
            ),
            data = PushDataDto(alerts = PushAlerts(status = true).toDto())
        )

        val body = json.parseToJsonElement(json.encodeToString(PushSubscriptionRequestDto.serializer(), request))
        val alerts = body.jsonObject.getValue("data").jsonObject.getValue("alerts").jsonObject

        val expected = mapOf(
            "mention" to true, "follow" to true, "favourite" to true, "reblog" to true,
            "poll" to false, "status" to true, "update" to false, "follow_request" to false
        )
        assertEquals(expected, alerts.mapValues { it.value.jsonPrimitive.boolean })
    }
}
