package com.pfitzer.tootsie.domain.usecase.auth

import com.pfitzer.tootsie.domain.model.AppCredentials
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Use case for building the OAuth2 authorization URL.
 * This URL is used to redirect the user to the Mastodon instance for login.
 */
class GetAuthorizationUrlUseCase @Inject constructor() {

    /**
     * Builds the OAuth2 authorization URL for the Mastodon instance.
     *
     * @param instanceUrl The Mastodon instance URL (e.g., "mastodon.social")
     * @param appCredentials The app credentials from registration
     * @return The full authorization URL to open in browser
     */
    operator fun invoke(
        instanceUrl: String,
        appCredentials: AppCredentials
    ): String {
        val redirectUri = URLEncoder.encode(appCredentials.redirectUri, "UTF-8")
        val scopes = URLEncoder.encode("read write follow push", "UTF-8")

        return buildString {
            append("https://")
            append(instanceUrl)
            append("/oauth/authorize")
            append("?client_id=").append(appCredentials.clientId)
            append("&redirect_uri=").append(redirectUri)
            append("&response_type=code")
            append("&scope=").append(scopes)
        }
    }
}
