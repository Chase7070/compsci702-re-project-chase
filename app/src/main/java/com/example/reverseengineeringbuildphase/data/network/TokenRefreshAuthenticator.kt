package com.example.reverseengineeringbuildphase.data.network

import com.example.reverseengineeringbuildphase.data.repository.SessionRepository
import com.example.reverseengineeringbuildphase.data.security.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * On HTTP 401 with an existing Authorization header: single refresh attempt + one replay.
 * Clears session when refresh is unavailable or fails.
 */
class TokenRefreshAuthenticator(
    private val tokenManager: TokenManager,
    private val sessionRepository: SessionRepository
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header(HEADER_AUTH).isNullOrBlank()) {
            return null
        }
        if (response.request.header(HEADER_AUTH_RETRY) != null) {
            return null
        }
        if (tokenManager.getRefreshToken().isNullOrBlank()) {
            return null
        }
        synchronized(lock) {
            val requestAuth = response.request.header(HEADER_AUTH).orEmpty()
            val current = tokenManager.getAuthorizationHeaderValue()
            if (current.isNotEmpty() && current != requestAuth) {
                return newRequestWithAuth(response.request, current)
            }
            val refreshed = sessionRepository.refreshAccessTokenBlocking()
            if (!refreshed) {
                sessionRepository.clearSessionAndSignalRelogin()
                return null
            }
            val newToken = tokenManager.getAuthorizationHeaderValue()
            if (newToken.isEmpty()) {
                sessionRepository.clearSessionAndSignalRelogin()
                return null
            }
            return newRequestWithAuth(response.request, newToken)
        }
    }

    private fun newRequestWithAuth(request: Request, token: String): Request {
        return request.newBuilder()
            .header(HEADER_AUTH, token)
            .header(HEADER_AUTH_RETRY, "1")
            .build()
    }

    private companion object {
        const val HEADER_AUTH = "Authorization"
        const val HEADER_AUTH_RETRY = "X-Auth-Retry"
    }
}
