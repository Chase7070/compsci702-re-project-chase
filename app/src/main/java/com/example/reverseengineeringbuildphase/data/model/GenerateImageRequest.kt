package com.example.reverseengineeringbuildphase.data.model

import com.google.gson.annotations.SerializedName

data class GenerateImageRequest(
    @SerializedName("signature")
    val signature: String,
    @SerializedName("prompt")
    val prompt: String
) {
    override fun toString(): String =
        "GenerateImageRequest(signature=<redacted>, prompt=<redacted>)"
}
