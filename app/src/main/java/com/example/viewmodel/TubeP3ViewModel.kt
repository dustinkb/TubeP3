package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.DownloadDispatchResult
import com.example.backend.TermuxBackend
import com.example.backend.TermuxExecutionResult
import com.example.error.TermuxErrorClassifier
import com.example.model.DownloadError
import com.example.model.DownloadState
import com.example.model.DownloadStatus
import com.example.model.TermuxInstallationStatus
import com.example.util.UrlSanitizer
import com.example.util.UrlValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TubeP3ViewModel(
    private val termuxBackend: TermuxBackend
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadState())
    val uiState: StateFlow<DownloadState> = _uiState.asStateFlow()

    private val _termuxStatus = MutableStateFlow(TermuxInstallationStatus())
    val termuxStatus: StateFlow<TermuxInstallationStatus> = _termuxStatus.asStateFlow()

    init {
        refreshTermuxStatus()
        observeExecutionResults()
    }

    fun refreshTermuxStatus() {
        val status = termuxBackend.checkInstallation()
        _termuxStatus.value = status
    }

    private fun observeExecutionResults() {
        viewModelScope.launch {
            termuxBackend.executionEvents.collect { result ->
                handleExecutionResult(result)
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { current ->
            current.copy(
                urlInput = newUrl,
                error = if (current.status == DownloadStatus.FAILED) null else current.error,
                status = if (current.status == DownloadStatus.FAILED) DownloadStatus.READY else current.status
            )
        }
    }

    fun pasteUrl(pastedUrl: String) {
        onUrlChanged(pastedUrl)
    }

    fun clearUrl() {
        _uiState.update { it.copy(urlInput = "", error = null, status = DownloadStatus.READY) }
    }

    fun startDownload() {
        val rawInput = _uiState.value.urlInput
        val validation = UrlSanitizer.sanitize(rawInput)

        if (validation is UrlValidationResult.Invalid) {
            _uiState.update {
                it.copy(
                    status = DownloadStatus.FAILED,
                    statusDetail = "Invalid YouTube URL",
                    error = DownloadError.InvalidUrl(validation.reason)
                )
            }
            return
        }

        val sanitizedUrl = (validation as UrlValidationResult.Valid).sanitizedUrl

        // Update state to PREPARING
        _uiState.update {
            it.copy(
                status = DownloadStatus.PREPARING,
                activeUrl = sanitizedUrl,
                statusDetail = "Preparing download...",
                error = null,
                logOutput = null
            )
        }

        // Dispatch to Termux backend
        when (val dispatchResult = termuxBackend.startAudioDownload(sanitizedUrl)) {
            is DownloadDispatchResult.Success -> {
                _uiState.update {
                    it.copy(
                        status = DownloadStatus.DOWNLOADING,
                        statusDetail = "Downloading audio stream..."
                    )
                }
            }
            is DownloadDispatchResult.Failure -> {
                _uiState.update {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        statusDetail = dispatchResult.error.title,
                        error = dispatchResult.error
                    )
                }
            }
        }
    }

    private fun handleExecutionResult(result: TermuxExecutionResult) {
        if (result.isSuccess) {
            _uiState.update {
                it.copy(
                    status = DownloadStatus.FINISHED,
                    statusDetail = "Finished. High-quality MP3 saved to Downloads.",
                    error = null,
                    logOutput = result.fullOutput
                )
            }
        } else {
            val classifiedError = TermuxErrorClassifier.classify(result)
            _uiState.update {
                it.copy(
                    status = DownloadStatus.FAILED,
                    statusDetail = classifiedError.title,
                    error = classifiedError,
                    logOutput = result.fullOutput
                )
            }
        }
    }

    fun resetStatus() {
        _uiState.update {
            it.copy(
                status = DownloadStatus.READY,
                statusDetail = "",
                error = null
            )
        }
    }

    class Factory(private val termuxBackend: TermuxBackend) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TubeP3ViewModel(termuxBackend) as T
        }
    }
}
