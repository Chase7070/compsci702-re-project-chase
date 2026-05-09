package com.example.reverseengineeringbuildphase.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reverseengineeringbuildphase.data.network.NetworkModule
import com.example.reverseengineeringbuildphase.data.repository.ImageRepository
import com.example.reverseengineeringbuildphase.domain.GenerateImageFailure
import com.example.reverseengineeringbuildphase.domain.GenerateImageUseCase
import com.example.reverseengineeringbuildphase.util.NetworkErrorMapper
import kotlinx.coroutines.CancellationException as CoroutineCancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val useCase = GenerateImageUseCase(
        ImageRepository(NetworkModule.apiService)
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

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
                    statusMessage = "请先输入 prompt。"
                )
            }
            return
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
                    statusMessage = "正在请求 /auth …"
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
                        statusMessage = "已获取图片地址，正在加载预览…"
                    )
                }
            } catch (e: CoroutineCancellationException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = null,
                        statusMessage = "已取消。"
                    )
                }
                throw e
            } catch (e: GenerateImageFailure.AuthFailed) {
                val msg = NetworkErrorMapper.messageWithPrefix(
                    cause = e.cause ?: e,
                    prefix = "/auth 失败："
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = msg,
                        statusMessage = "生成失败。"
                    )
                }
            } catch (e: GenerateImageFailure.GenerateFailed) {
                val msg = NetworkErrorMapper.messageWithPrefix(
                    cause = e.cause ?: e,
                    prefix = "/generate_image 失败："
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = msg,
                        statusMessage = "生成失败。"
                    )
                }
            } catch (e: GenerateImageFailure.InvalidImageUrl) {
                val msg = NetworkErrorMapper.messageWithPrefix(
                    cause = e.cause ?: e,
                    prefix = "图片地址无效："
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = msg,
                        statusMessage = "生成失败。"
                    )
                }
            } catch (e: Exception) {
                val msg = NetworkErrorMapper.messageForThrowable(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineError = msg,
                        statusMessage = "生成失败。"
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
                statusMessage = "图片已显示。"
            )
        }
    }

    fun onImageLoadFailed(expectedUrl: String, throwable: Throwable) {
        if (throwable is CoroutineCancellationException) return
        _uiState.update { state ->
            if (state.imageUrl != expectedUrl) return@update state
            state.copy(
                imageLoadError = "图片加载失败：${NetworkErrorMapper.messageForThrowable(throwable)}",
                statusMessage = "图片地址有效，但预览加载失败。"
            )
        }
    }
}
