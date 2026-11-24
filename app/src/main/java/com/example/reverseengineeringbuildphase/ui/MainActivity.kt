package com.example.reverseengineeringbuildphase.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.dispose
import coil.load
import com.example.reverseengineeringbuildphase.R
import com.example.reverseengineeringbuildphase.data.session.AppSessionEvents
import com.example.reverseengineeringbuildphase.databinding.ActivityMainBinding
import com.example.reverseengineeringbuildphase.util.ImageSaver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory {
            initializer {
                MainViewModel(application)
            }
        }
    }

    private var lastLoadedImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActions()
        observeUiState()
        observeSessionEvents()
    }

    private fun setupActions() {
        binding.promptEditText.doAfterTextChangedCompat {
            viewModel.onPromptChanged(it)
        }

        binding.generateButton.setOnClickListener {
            viewModel.generateImage(getString(R.string.error_prompt_empty))
        }

        binding.cancelButton.setOnClickListener {
            viewModel.cancelGenerate()
        }

        binding.saveButton.setOnClickListener {
            val imageUrl = viewModel.uiState.value.imageUrl
            if (imageUrl.isNullOrBlank()) {
                Toast.makeText(this, "No image to save.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                binding.saveButton.isEnabled = false
                try {
                    val result = ImageSaver.saveFromUrl(this@MainActivity, imageUrl)
                    result.onSuccess {
                        Toast.makeText(this@MainActivity, "Saved to gallery.", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(
                            this@MainActivity,
                            "Save failed: ${err.message ?: "unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Save failed: ${e.message ?: "unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    val s = viewModel.uiState.value
                    binding.saveButton.isEnabled = !s.imageUrl.isNullOrBlank() && !s.isLoading
                }
            }
        }
    }

    private fun observeSessionEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppSessionEvents.reloginSuggested.collect {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.session_relogin_hint),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: MainUiState) {
        if (binding.promptEditText.text?.toString() != state.prompt) {
            binding.promptEditText.setText(state.prompt)
            binding.promptEditText.setSelection(state.prompt.length)
        }

        binding.promptInputLayout.error = state.promptInputError
        if (state.promptInputError != null) {
            binding.promptEditText.requestFocus()
        }

        binding.progressBar.isVisible = state.isLoading
        binding.cancelButton.isVisible = state.isLoading
        binding.generateButton.isEnabled = !state.isLoading
        binding.promptInputLayout.isEnabled = !state.isLoading

        binding.statusText.text = buildStatusText(state)
        binding.statusText.setTextColor(resolveStatusColor(state))

        bindImagePreview(state)

        binding.saveButton.isEnabled = !state.imageUrl.isNullOrBlank() && !state.isLoading
    }

    private fun resolveStatusColor(state: MainUiState): Int {
        val colorRes = when {
            !state.pipelineError.isNullOrBlank() || !state.imageLoadError.isNullOrBlank() -> R.color.cp_error
            state.isLoading -> R.color.cp_neon_cyan
            !state.imageUrl.isNullOrBlank() -> R.color.cp_text_primary
            else -> R.color.cp_text_secondary
        }
        return ContextCompat.getColor(this, colorRes)
    }

    private fun buildStatusText(state: MainUiState): String = buildString {
        append(state.statusMessage.trim())
        state.pipelineError?.trim()?.let { err ->
            if (isNotEmpty()) append("\n\n")
            append(err)
        }
        state.imageLoadError?.trim()?.let { err ->
            if (isNotEmpty()) append("\n\n")
            append(err)
        }
    }

    private fun bindImagePreview(state: MainUiState) {
        val url = state.imageUrl?.trim().orEmpty()
        if (url.isEmpty()) {
            if (lastLoadedImageUrl != null) {
                binding.imageView.dispose()
                binding.imageView.setImageDrawable(null)
                lastLoadedImageUrl = null
            }
            return
        }

        if (url == lastLoadedImageUrl) {
            return
        }

        lastLoadedImageUrl = url
        binding.imageView.load(url) {
            listener(
                onStart = {
                    viewModel.onImageLoadStarted(url)
                },
                onSuccess = { _, _ ->
                    viewModel.onImageLoadSuccess(url)
                },
                onError = { _, result ->
                    val err = result.throwable
                    if (err !is CancellationException) {
                        viewModel.onImageLoadFailed(url, err)
                    }
                },
                onCancel = {
                    // Coil cancels in-flight work; do not treat as a hard error.
                }
            )
        }
    }
}
