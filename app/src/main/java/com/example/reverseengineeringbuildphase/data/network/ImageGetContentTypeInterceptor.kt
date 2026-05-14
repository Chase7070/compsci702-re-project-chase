package com.example.reverseengineeringbuildphase.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * For successful GET responses, require a body Content-Type whose primary type is `image`
 * (e.g. image/jpeg). POST JSON responses are unchanged.
 */
class ImageGetContentTypeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.method != "GET") return response
        if (!response.isSuccessful) return response
        val raw = response.header("Content-Type") ?: ""
        val mime = raw.substringBefore(';').trim().lowercase()
        if (!mime.startsWith("image/")) {
            response.close()
            throw IOException("Blocked non-image GET (Content-Type: $raw)")
        }
        return response
    }
}
