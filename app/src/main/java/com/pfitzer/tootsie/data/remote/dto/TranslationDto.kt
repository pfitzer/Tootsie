package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Translation of a status, from POST /api/v1/statuses/:id/translate.
 */
@Serializable
data class TranslationDto(
    @SerialName("content") val content: String = "",
    @SerialName("spoiler_text") val spoilerText: String = "",
    @SerialName("poll") val poll: TranslationPollDto? = null,
    @SerialName("media_attachments") val mediaAttachments: List<TranslationMediaDto> = emptyList(),
    @SerialName("detected_source_language") val detectedSourceLanguage: String? = null,
    @SerialName("provider") val provider: String? = null
)

@Serializable
data class TranslationPollDto(
    @SerialName("options") val options: List<TranslationPollOptionDto> = emptyList()
)

@Serializable
data class TranslationPollOptionDto(
    @SerialName("title") val title: String
)

@Serializable
data class TranslationMediaDto(
    @SerialName("id") val id: String,
    @SerialName("description") val description: String? = null
)

/**
 * The part of /api/v2/instance needed to know whether the server can translate.
 */
@Serializable
data class InstanceV2Dto(
    @SerialName("configuration") val configuration: InstanceV2ConfigurationDto? = null
)

@Serializable
data class InstanceV2ConfigurationDto(
    @SerialName("translation") val translation: TranslationConfigDto? = null
)

@Serializable
data class TranslationConfigDto(
    @SerialName("enabled") val enabled: Boolean = false
)
