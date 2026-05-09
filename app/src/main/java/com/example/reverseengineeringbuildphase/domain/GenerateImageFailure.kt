package com.example.reverseengineeringbuildphase.domain

/**
 * Typed failures for the auth → generate_image pipeline.
 * Used so the UI can distinguish /auth vs /generate_image errors.
 */
sealed class GenerateImageFailure(
    message: String,
    cause: Throwable
) : Exception(message, cause) {
    class AuthFailed(cause: Throwable) : GenerateImageFailure("Auth request failed", cause)
    class GenerateFailed(cause: Throwable) : GenerateImageFailure("Generate image request failed", cause)
    class InvalidImageUrl(cause: Throwable) : GenerateImageFailure("Invalid or disallowed image URL", cause)
}
