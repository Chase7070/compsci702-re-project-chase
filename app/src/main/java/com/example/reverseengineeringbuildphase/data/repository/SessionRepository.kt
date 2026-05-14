package com.example.reverseengineeringbuildphase.data.repository

import com.example.reverseengineeringbuildphase.data.model.SessionCreateRequest
import com.example.reverseengineeringbuildphase.data.model.SessionRefreshRequest
import com.example.reverseengineeringbuildphase.data.model.SessionResponse
import com.example.reverseengineeringbuildphase.data.network.ApiService
import com.example.reverseengineeringbuildphase.data.network.EndpointProvider
import com.example.reverseengineeringbuildphase.data.security.TokenManager
import com.example.reverseengineeringbuildphase.data.session.AppSessionEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Session bootstrap and refresh use an **unauthenticated** OkHttp stack (no Authorization interceptor, no authenticator).
 */
class SessionRepository(
    private val publicApiService: ApiService,
    private val tokenManager: TokenManager,
    private val assetBootstrapReader: () -> String?,
    private val localBootstrapReader: () -> String?
) {

    suspend fun bootstrapIfNeeded() = withContext(Dispatchers.IO) {
        if (tokenManager.hasAccessToken()) return@withContext
        val bootstrap = assetBootstrapReader()?.trim().takeIf { !it.isNullOrEmpty() }
            ?: localBootstrapReader()?.trim().takeIf { !it.isNullOrEmpty() }
            ?: ""

        val remote = tryCreateSession(bootstrap)

        if (remote != null) {
            applySessionResponse(remote)
            return@withContext
        }

        if (bootstrap.isNotEmpty()) {
            tokenManager.setAccessTokenOnly(bootstrap)
        }
    }

    fun applySessionResponse(response: SessionResponse) {
        tokenManager.setTokens(
            accessTokenHeaderValue = response.accessToken.trim(),
            refreshToken = response.refreshToken?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    fun refreshAccessTokenBlocking(): Boolean {
        val refresh = tokenManager.getRefreshToken() ?: return false
        return try {
            val resp = runBlocking(Dispatchers.IO) {
                publicApiService.refreshSession(
                    EndpointProvider.sessionRefreshPath(),
                    SessionRefreshRequest(refreshToken = refresh)
                )
            }
            applySessionResponse(resp)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearSessionAndSignalRelogin() {
        tokenManager.clearSession()
        AppSessionEvents.notifyReloginSuggested()
    }

    /**
     * `/session` is an opportunistic upgrade path.
     *
     * If the backend has not fully switched to the session contract yet, local builds should still
     * be able to fall back to the bootstrap Authorization value instead of failing startup and then
     * hitting `/auth` with an empty header.
     */
    private suspend fun tryCreateSession(bootstrap: String): SessionResponse? {
        return runCatching {
            publicApiService.createSession(
                EndpointProvider.sessionPath(),
                SessionCreateRequest(bootstrapToken = bootstrap.takeIf { it.isNotEmpty() })
            )
        }.getOrElse { e ->
            when (e) {
                is HttpException -> null
                is IOException -> null
                else -> null
            }
        }
    }
}
