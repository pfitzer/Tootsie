package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTO for creating/updating push subscriptions.
 */
@Serializable
data class PushSubscriptionRequestDto(
    val subscription: SubscriptionKeysDto,
    val data: PushDataDto
)

/**
 * `data` part of the subscription request: Mastodon expects `data[alerts][…]`.
 */
@Serializable
data class PushDataDto(
    val alerts: PushAlertsDto
)

/**
 * Subscription keys container.
 */
@Serializable
data class SubscriptionKeysDto(
    val endpoint: String,
    val keys: KeysDto,
    // true: standard RFC 8291 aes128gcm (Mastodon >= 4.4). The legacy scheme puts salt and
    // server key in HTTP headers, which UnifiedPush doesn't forward. No default value:
    // the Json config doesn't encode defaults.
    val standard: Boolean
)

/**
 * VAPID keys for Web Push encryption.
 */
@Serializable
data class KeysDto(
    val p256dh: String,  // VAPID public key (base64url)
    val auth: String      // Auth secret (base64url)
)

/**
 * Response DTO for push subscription.
 */
@Serializable
data class PushSubscriptionDto(
    val id: String,
    val endpoint: String,
    @SerialName("server_key") val serverKey: String,
    val alerts: PushAlertsDto
)

/**
 * Push alerts configuration DTO.
 * All values are always encoded: the Json config omits defaults, and Mastodon treats a
 * missing alert as disabled.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PushAlertsDto(
    @EncodeDefault val mention: Boolean = true,
    @EncodeDefault val follow: Boolean = true,
    @EncodeDefault val favourite: Boolean = true,
    @EncodeDefault val reblog: Boolean = true,
    @EncodeDefault val poll: Boolean = false,
    @EncodeDefault val status: Boolean = false,
    @EncodeDefault val update: Boolean = false,
    @EncodeDefault @SerialName("follow_request") val followRequest: Boolean = false
)
