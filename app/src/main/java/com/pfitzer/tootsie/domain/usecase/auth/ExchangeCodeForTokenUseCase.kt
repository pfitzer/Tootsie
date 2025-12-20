package com.pfitzer.tootsie.domain.usecase.auth

import com.pfitzer.tootsie.domain.model.AccessToken
import com.pfitzer.tootsie.domain.model.AppCredentials
import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for exchanging an authorization code for an access token.
 * This is step 3 of the OAuth2 flow, after the user has authorized the app.
 */
class ExchangeCodeForTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Exchanges the authorization code for an access token.
     *
     * @param instanceUrl The Mastodon instance URL
     * @param appCredentials The app credentials from registration
     * @param authorizationCode The code received from OAuth callback
     * @return Result containing AccessToken on success
     */
    suspend operator fun invoke(
        instanceUrl: String,
        appCredentials: AppCredentials,
        authorizationCode: String
    ): Result<AccessToken> {
        return authRepository.getAccessToken(
            instanceUrl = instanceUrl,
            clientId = appCredentials.clientId,
            clientSecret = appCredentials.clientSecret,
            code = authorizationCode
        )
    }
}
