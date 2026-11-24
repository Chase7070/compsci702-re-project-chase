package com.example.reverseengineeringbuildphase.data.model

import com.google.gson.annotations.SerializedName

/** Request body for [ApiService.createSession]. Backend may ignore unknown fields. */
data class SessionCreateRequest(
    @SerializedName("bootstrap_token")
    val bootstrapToken: String? = null
) {
    override fun toString(): String = "SessionCreateRequest(bootstrapToken=<redacted>)"
}

data class SessionRefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
) {
    override fun toString(): String = "SessionRefreshRequest(refreshToken=<redacted>)"
}

/**
 * Session endpoint response. [accessToken] is the raw value to send in the `Authorization` header
 * (not prefixed with `Bearer ` unless the server returns it that way).
 */
data class SessionResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String? = null
) {
    override fun toString(): String =
        "SessionResponse(accessToken=<redacted>, refreshToken=<redacted>)"
}
