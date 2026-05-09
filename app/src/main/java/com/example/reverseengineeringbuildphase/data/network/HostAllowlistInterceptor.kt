package com.example.reverseengineeringbuildphase.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Rejects requests whose URL host is not in the configured allowlist.
 */
class HostAllowlistInterceptor(
    private val allowedHosts: Set<String>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host.lowercase()
        if (host !in allowedHosts) {
            throw IOException("Blocked request to disallowed host: $host")
        }
        return chain.proceed(chain.request())
    }
}
