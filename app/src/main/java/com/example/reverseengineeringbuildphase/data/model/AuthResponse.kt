package com.example.reverseengineeringbuildphase.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("signature")
    val signature: String
)
