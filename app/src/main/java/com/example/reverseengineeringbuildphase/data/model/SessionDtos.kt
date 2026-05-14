package com.example.reverseengineeringbuildphase.data.model

import com.google.gson.annotations.SerializedName

/** Request body for [ApiService.createSession]. Backend may ignore unknown fields. */
data class SessionCreateRequest(
    @SerializedName("bootstrap_token")
    val bootstrapToken: String? = null
)

data class SessionRefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * Session endpoint response. [accessToken] is the raw value to send in the `Authorization` header
 * (not prefixed with `Bearer ` unless the server returns it that way).
 */
data class SessionResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String? = null
)
