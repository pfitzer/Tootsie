package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for Mastodon Filter from API (v2).
 * Represents a user-created filter for hiding or warning about content.
 */
@Serializable
data class FilterDto(
    /**
     * The ID of the filter.
     */
    @SerialName("id")
    val id: String,

    /**
     * The user-defined title of the filter.
     */
    @SerialName("title")
    val title: String,

    /**
     * The contexts in which the filter applies.
     * Values: "home", "notifications", "public", "thread", "account"
     */
    @SerialName("context")
    val context: List<String>,

    /**
     * When the filter expires, or null if it doesn't expire.
     */
    @SerialName("expires_at")
    val expiresAt: String? = null,

    /**
     * The action to take when a status matches this filter.
     * Values: "warn", "hide"
     */
    @SerialName("filter_action")
    val filterAction: String = "warn",

    /**
     * The keywords attached to this filter.
     */
    @SerialName("keywords")
    val keywords: List<FilterKeywordDto> = emptyList()
)

/**
 * DTO for a filter keyword.
 */
@Serializable
data class FilterKeywordDto(
    /**
     * The ID of the keyword.
     */
    @SerialName("id")
    val id: String,

    /**
     * The text to match.
     */
    @SerialName("keyword")
    val keyword: String,

    /**
     * Whether to match whole words only.
     */
    @SerialName("whole_word")
    val wholeWord: Boolean = false
)
