package com.example.reverseengineeringbuildphase.data.network

import android.content.Context
import com.example.reverseengineeringbuildphase.security.DeviceRisk
import com.example.reverseengineeringbuildphase.security.IntegrityChecker
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Blocks sensitive outbound requests in obviously risky environments.
 *
 * We intentionally gate at the network layer so bypassing UI controls is not enough to
 * reach `/auth`, `/generate_image`, `/session`, or `/session/refresh`.
 */
class SensitiveRequestGuardInterceptor(
    private val appContext: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isSensitive = request.method.equals("POST", ignoreCase = true) ||
            !request.header("Authorization").isNullOrBlank()
        if (!isSensitive) {
            return chain.proceed(request)
        }

        if (!IntegrityChecker.signingCertMatchesExpected(appContext)) {
            throw SensitiveRequestBlockedException("应用签名校验失败，已阻止敏感请求。")
        }

        when (IntegrityChecker.assess(appContext)) {
            DeviceRisk.High ->
                throw SensitiveRequestBlockedException("检测到高风险运行环境，已阻止敏感请求。")

            else -> return chain.proceed(request)
        }
    }
}

class SensitiveRequestBlockedException(
    message: String
) : IOException(message)
