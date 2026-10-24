package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTO for OAuth token responses.
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("scope") val scope: String,
    @SerialName("created_at") val createdAt: Long
)

/**
 * Network DTO for app registration responses.
 */
@Serializable
data class AppCredentialsDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("name") val name: String,
    @SerialName("redirect_uri") val redirectUri: String = "urn:ietf:wg:oauth:2.0:oob",
    @SerialName("vapid_key") val vapidKey: String? = null
)

/**
 * Network DTO for instance information.
 */
@Serializable
data class InstanceDto(
    @SerialName("uri") val uri: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("version") val version: String,
    @SerialName("email") val email: String? = null,
    @SerialName("languages") val languages: List<String> = emptyList(),
    @SerialName("registrations") val registrations: Boolean = false,
    @SerialName("approval_required") val approvalRequired: Boolean = false,
    @SerialName("max_toot_chars") val maxTootChars: Int = 500,
    @SerialName("configuration") val configuration: InstanceConfigurationDto? = null,
    @SerialName("stats") val stats: InstanceStatsDto? = null
)

@Serializable
data class InstanceConfigurationDto(
    @SerialName("statuses") val statuses: StatusesConfigDto? = null,
    @SerialName("polls") val polls: PollsConfigDto? = null
)

@Serializable
data class StatusesConfigDto(
    @SerialName("max_characters") val maxCharacters: Int = 500,
    @SerialName("max_media_attachments") val maxMediaAttachments: Int = 4,
    @SerialName("characters_reserved_per_url") val charactersReservedPerUrl: Int = 23
)

@Serializable
data class PollsConfigDto(
    @SerialName("max_options") val maxOptions: Int = 4,
    @SerialName("max_characters_per_option") val maxCharactersPerOption: Int = 50,
    @SerialName("min_expiration") val minExpiration: Int = 300,
    @SerialName("max_expiration") val maxExpiration: Int = 2629746
)

@Serializable
data class InstanceStatsDto(
    @SerialName("user_count") val userCount: Long,
    @SerialName("status_count") val statusCount: Long,
    @SerialName("domain_count") val domainCount: Long
)
