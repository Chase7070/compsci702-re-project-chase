package com.example.reverseengineeringbuildphase.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthorizationInterceptor(
    private val tokenProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider().trim()
        val requestBuilder = chain.request().newBuilder()

        if (token.isNotEmpty()) {
            requestBuilder.header("Authorization", token)
        }

        return chain.proceed(requestBuilder.build())
    }
}
