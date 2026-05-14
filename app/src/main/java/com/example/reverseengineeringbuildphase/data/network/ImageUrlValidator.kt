package com.example.reverseengineeringbuildphase.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ImageUrlValidator {
    /**
     * @throws IllegalArgumentException if URL is not HTTPS or host is not allowlisted.
     */
    fun validateOrThrow(url: String) {
        val trimmed = url.trim()
        val httpUrl = trimmed.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Malformed image URL.")
        require(httpUrl.scheme.equals("https", ignoreCase = true)) {
            "Image URL must use HTTPS."
        }
        val host = httpUrl.host.lowercase()
        val allowed = AllowedHosts.asSet()
        require(host in allowed) {
            "Image host not allowed: $host"
        }
    }
}
