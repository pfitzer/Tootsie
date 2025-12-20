package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing OAuth access token.
 */
data class AccessToken(
    val accessToken: String,
    val tokenType: String, // Usually "Bearer"
    val scope: String, // Space-separated scopes
    val createdAt: Long // Unix timestamp
)

/**
 * Domain model representing application credentials for OAuth.
 */
data class AppCredentials(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String = "tootsie://oauth", // Custom deep link for OAuth callback
    val name: String = "Tootsie"
)
