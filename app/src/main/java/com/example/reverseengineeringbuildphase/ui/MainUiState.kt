package com.example.reverseengineeringbuildphase.ui

data class MainUiState(
    val prompt: String = "",
    val imageUrl: String? = null,
    val isLoading: Boolean = false,
    val statusMessage: String = "Input prompt and generate image.",
    /** Shown on [com.google.android.material.textfield.TextInputLayout] (e.g. empty prompt). */
    val promptInputError: String? = null,
    /** /auth or /generate_image failure message for the status area. */
    val pipelineError: String? = null,
    /** Coil / network error when loading the image URL into the preview. */
    val imageLoadError: String? = null
)
