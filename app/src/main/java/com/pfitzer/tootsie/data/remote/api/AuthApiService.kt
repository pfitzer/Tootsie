package com.pfitzer.tootsie.data.remote.api

import com.pfitzer.tootsie.data.remote.dto.AppCredentialsDto
import com.pfitzer.tootsie.data.remote.dto.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Retrofit API service for OAuth authentication endpoints.
 */
interface AuthApiService {
    /**
     * Register a new application with the Mastodon instance.
     * This is required before initiating the OAuth flow.
     *
     * @param clientName Application name
     * @param redirectUris OAuth redirect URI (use "urn:ietf:wg:oauth:2.0:oob" for manual entry)
     * @param scopes Space-separated list of scopes (e.g., "read write follow")
     * @param website Application website URL
     * @return Application credentials containing client_id and client_secret
     */
    @FormUrlEncoded
    @POST("api/v1/apps")
    suspend fun registerApp(
        @Field("client_name") clientName: String,
        @Field("redirect_uris") redirectUris: String = "urn:ietf:wg:oauth:2.0:oob",
        @Field("scopes") scopes: String = "read write follow push",
        @Field("website") website: String? = null
    ): AppCredentialsDto

    /**
     * Exchange an authorization code for an access token.
     * Called after user authorizes the app.
     *
     * @param clientId Application client ID
     * @param clientSecret Application client secret
     * @param redirectUri Must match the redirect_uri used during registration
     * @param code Authorization code obtained from the OAuth flow
     * @param grantType OAuth grant type (should be "authorization_code")
     * @param scope Space-separated list of scopes
     * @return Access token and metadata
     */
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun getAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String = "urn:ietf:wg:oauth:2.0:oob",
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("scope") scope: String = "read write follow push"
    ): TokenResponse

    /**
     * Revoke an access token.
     * Logs out the user and invalidates the token.
     *
     * @param clientId Application client ID
     * @param clientSecret Application client secret
     * @param token The access token to revoke
     */
    @FormUrlEncoded
    @POST("oauth/revoke")
    suspend fun revokeToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("token") token: String
    )
}
