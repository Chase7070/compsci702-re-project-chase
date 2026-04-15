package com.example.reverseengineeringbuildphase.data.network

import com.example.reverseengineeringbuildphase.data.model.AuthResponse
import com.example.reverseengineeringbuildphase.data.model.GenerateImageRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth")
    suspend fun auth(): AuthResponse

    /**
     * Backend returns a JSON string (e.g. `"images/uuid.jpg"`), not an object with `image_url`.
     */
    @POST("generate_image")
    suspend fun generateImage(
        @Body request: GenerateImageRequest
    ): String
}
