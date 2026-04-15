package com.example.reverseengineeringbuildphase.domain

import com.example.reverseengineeringbuildphase.data.repository.ImageRepository

class GenerateImageUseCase(
    private val repository: ImageRepository
) {
    suspend operator fun invoke(prompt: String): String {
        return repository.generateImageUrl(prompt)
    }
}
