package com.example.reverseengineeringbuildphase.data.repository

import com.example.reverseengineeringbuildphase.BuildConfig
import com.example.reverseengineeringbuildphase.data.model.GenerateImageRequest
import com.example.reverseengineeringbuildphase.data.network.ApiService
import com.example.reverseengineeringbuildphase.domain.GenerateImageFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class ImageRepository(
    private val apiService: ApiService
) {
    suspend fun generateImageUrl(prompt: String): String {
        coroutineContext.ensureActive()

        val signature = try {
            val response = apiService.auth()
            val sig = response.signature.trim()
            if (sig.isEmpty()) {
                throw GenerateImageFailure.AuthFailed(
                    IllegalStateException("Empty signature from /auth response.")
                )
            }
            sig
        } catch (e: CancellationException) {
            throw e
        } catch (e: GenerateImageFailure.AuthFailed) {
            throw e
        } catch (e: Exception) {
            throw GenerateImageFailure.AuthFailed(e)
        }

        coroutineContext.ensureActive()

        return try {
            val raw = apiService.generateImage(
                GenerateImageRequest(
                    signature = signature,
                    prompt = prompt
                )
            ).trim()
            val url = normalizeImageUrl(raw)
            if (url.isEmpty()) {
                throw GenerateImageFailure.GenerateFailed(
                    IllegalStateException("Empty image path from /generate_image response.")
                )
            }
            url
        } catch (e: CancellationException) {
            throw e
        } catch (e: GenerateImageFailure.GenerateFailed) {
            throw e
        } catch (e: Exception) {
            throw GenerateImageFailure.GenerateFailed(e)
        }
    }

    /**
     * API may return a relative path (e.g. `images/…jpg`); Coil needs an absolute URL.
     */
    private fun normalizeImageUrl(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) {
            return t
        }
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val path = t.trimStart('/')
        return "$base/$path"
    }
}
