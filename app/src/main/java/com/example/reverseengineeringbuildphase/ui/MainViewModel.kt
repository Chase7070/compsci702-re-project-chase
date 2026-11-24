package com.example.reverseengineeringbuildphase.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reverseengineeringbuildphase.R
import com.example.reverseengineeringbuildphase.data.network.NetworkModule
import com.example.reverseengineeringbuildphase.data.repository.ImageRepository
import com.example.reverseengineeringbuildphase.domain.GenerateImageFailure
import com.example.reverseengineeringbuildphase.domain.GenerateImageUseCase
import com.example.reverseengineeringbuildphase.security.DeviceRisk
import com.example.reverseengineeringbuildphase.security.IntegrityChecker
import com.example.reverseengineeringbuildphase.util.NetworkErrorMapper
import kotlinx.coroutines.CancellationException as CoroutineCancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val useCase = GenerateImageUseCase(
        ImageRepository(NetworkModule.apiService)
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    private val res get() = getApplication<Application>().resources

    fun onPromptChanged(newPrompt: String) {
        _uiState.update {
            it.copy(
                prompt = newPrompt,
                promptInputError = null,
                pipelineError = null
            )
        }
    }

    fun generateImage(emptyPromptMessage: String) {
        val promptValue = uiState.value.prompt.trim()
        if (promptValue.isBlank()) {
            _uiState.update {
                it.copy(
                    promptInputError = emptyPromptMessage,
                    pipelineError = null,
                    imageLoadError = null,
                    statusMessage = res.getString(R.string.status_enter_prompt_first)
                )
            }
            return
        }

        val app = getApplication<Application>()
        val risk = IntegrityChecker.assess(app)
        if (risk == DeviceRisk.High) {
            _uiState.update {
                it.copy(
                    pipelineError = res.getString(R.string.error_high_risk_environment),
                    statusMessage = res.getString(R.string.status_generation_blocked)
                )
            }
            return
        }
        if (!IntegrityChecker.signingCertMatchesExpected(app)) {
            _uiState.update {
                it.copy(
                    pipelineError = res.getString(R.string.error_signing_mismatch),
                    statusMessage = res.getString(R.string.status_generation_blocked)
                )
            }
            return
        }
        if (risk == DeviceRisk.Medium) {
            _uiState.update {
                it.copy(
                    statusMessage = res.getString(R.string.status_medium_risk_warning)
                )
            }
        }

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    imageUrl = null,
                    promptInputError = null,
                    pipelineError = null,
                    imageLoadError = null,
                    statusMessage = res.getString(R.string.status_requesting_auth)
                )
            }

            try {
                val imageUrl = useCase(promptValue)
                _uiState.update {
                    it.copy(
                        imageUrl = imageUrl,
                        isLoading = false,
                        pipelineError = null,
                        imageLoadError = null,
                        statusMessage = res.getString(R.string.status_loading_preview)
                    )
                }
            } catch (e: CoroutineCancellationException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = null,
                        statusMessage = res.getString(R.string.status_cancelled)
                    )
                }
                throw e
            } catch (e: GenerateImageFailure.AuthFailed) {
                val msg = NetworkErrorMapper.messageWithPrefix(
                    resources = res,
                    cause = e.cause ?: e,
                    prefix = res.getString(R.string.error_auth_failed_prefix)
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = msg,
                        statusMessage = res.getString(R.string.status_generation_failed)
                    )
                }
            } catch (e: GenerateImageFailure.GenerateFailed) {
                val msg = NetworkErrorMapper.messageWithPrefix(
                    resources = res,
                    cause = e.cause ?: e,
                    prefix = res.getString(R.string.error_generate_failed_prefix)
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = msg,
                        statusMessage = res.getString(R.string.status_generation_failed)
                    )
                }
            } catch (e: GenerateImageFailure.InvalidImageUrl) {
                val msg = NetworkErrorMapper.messageWithPrefix(
                    resources = res,
                    cause = e.cause ?: e,
                    prefix = res.getString(R.string.error_invalid_image_url_prefix)
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = msg,
                        statusMessage = res.getString(R.string.status_generation_failed)
                    )
                }
            } catch (e: Exception) {
                val msg = NetworkErrorMapper.messageForThrowable(res, e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = msg,
                        statusMessage = res.getString(R.string.status_generation_failed)
                    )
                }
            }
        }
    }

    fun cancelGenerate() {
        generateJob?.cancel()
    }

    fun onImageLoadStarted(expectedUrl: String) {
        _uiState.update { state ->
            if (state.imageUrl != expectedUrl) return@update state
            state.copy(imageLoadError = null)
        }
    }

    fun onImageLoadSuccess(expectedUrl: String) {
        _uiState.update { state ->
            if (state.imageUrl != expectedUrl) return@update state
            state.copy(
                imageLoadError = null,
                statusMessage = res.getString(R.string.status_image_displayed)
            )
        }
    }

    fun onImageLoadFailed(expectedUrl: String, throwable: Throwable) {
        if (throwable is CoroutineCancellationException) return
        _uiState.update { state ->
            if (state.imageUrl != expectedUrl) return@update state
            val detail = NetworkErrorMapper.messageForThrowable(res, throwable)
            state.copy(
                imageLoadError = res.getString(R.string.error_image_load_failed_prefix) + " " + detail,
                statusMessage = res.getString(R.string.status_preview_load_failed)
            )
        }
    }
}
