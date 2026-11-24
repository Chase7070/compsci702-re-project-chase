package com.example.reverseengineeringbuildphase.data.network

import com.example.reverseengineeringbuildphase.data.model.AuthResponse
import com.example.reverseengineeringbuildphase.data.model.GenerateImageRequest
import com.example.reverseengineeringbuildphase.data.model.SessionCreateRequest
import com.example.reverseengineeringbuildphase.data.model.SessionRefreshRequest
import com.example.reverseengineeringbuildphase.data.model.SessionResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    /** Relative path (no leading slash); resolved against Retrofit baseUrl, which must end with `/`. */
    @POST
    suspend fun auth(@Url path: String): AuthResponse

    /**
     * Backend returns a JSON string (e.g. `"images/uuid.jpg"`), not an object with `image_url`.
     * Relative path (no leading slash); resolved against baseUrl ending with `/`.
     */
    @POST
    suspend fun generateImage(
        @Url path: String,
        @Body request: GenerateImageRequest
    ): String

    @POST
    suspend fun createSession(
        @Url path: String,
        @Body request: SessionCreateRequest
    ): SessionResponse

    @POST
    suspend fun refreshSession(
        @Url path: String,
        @Body request: SessionRefreshRequest
    ): SessionResponse
}
