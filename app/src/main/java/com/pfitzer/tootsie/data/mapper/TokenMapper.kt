package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.AppCredentialsDto
import com.pfitzer.tootsie.data.remote.dto.TokenResponse
import com.pfitzer.tootsie.domain.model.AccessToken
import com.pfitzer.tootsie.domain.model.AppCredentials

/**
 * Maps TokenResponse (network) to AccessToken (domain).
 */
fun TokenResponse.toDomain(): AccessToken {
    return AccessToken(
        accessToken = accessToken,
        tokenType = tokenType,
        scope = scope,
        createdAt = createdAt
    )
}

/**
 * Maps AppCredentialsDto (network) to AppCredentials (domain).
 */
fun AppCredentialsDto.toDomain(): AppCredentials {
    return AppCredentials(
        clientId = clientId,
        clientSecret = clientSecret,
        redirectUri = redirectUri,
        name = name
    )
}
