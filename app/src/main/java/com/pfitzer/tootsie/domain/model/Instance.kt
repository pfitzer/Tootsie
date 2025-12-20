package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing a Mastodon instance.
 */
data class Instance(
    val uri: String, // Instance domain (e.g., "mastodon.social")
    val title: String,
    val description: String,
    val version: String, // Mastodon version
    val email: String? = null, // Contact email
    val languages: List<String> = emptyList(), // ISO 639 language codes
    val registrations: Boolean, // Whether new registrations are open
    val approvalRequired: Boolean, // Whether registration requires approval
    val maxTootChars: Int = 500, // Maximum characters per status
    val configuration: InstanceConfiguration? = null,
    val stats: InstanceStats? = null
)

/**
 * Instance configuration details.
 */
data class InstanceConfiguration(
    val statusesMaxCharacters: Int = 500,
    val statusesMaxMediaAttachments: Int = 4,
    val pollMaxOptions: Int = 4,
    val pollMaxCharactersPerOption: Int = 50,
    val pollMinExpiration: Int = 300, // Seconds
    val pollMaxExpiration: Int = 2629746 // Seconds (1 month)
)

/**
 * Instance statistics.
 */
data class InstanceStats(
    val userCount: Long,
    val statusCount: Long,
    val domainCount: Long // Number of known instances
)
