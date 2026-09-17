package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.DownloadDispatchResult
import com.example.backend.TermuxBackend
import com.example.backend.TermuxExecutionResult
import com.example.backend.TermuxProgressReceiver
import com.example.error.TermuxErrorClassifier
import com.example.model.DownloadError
import com.example.model.DownloadProgress
import com.example.model.DownloadProgressStage
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
        observeProgress()
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

    private fun observeProgress() {
        viewModelScope.launch {
            TermuxProgressReceiver.progressFlow.collect { progress ->
                if (progress != null &&
                    _uiState.value.status != DownloadStatus.FINISHED &&
                    _uiState.value.status != DownloadStatus.FAILED
                ) {
                    _uiState.update { current ->
                        val mappedStatus = when (progress.stage) {
                            DownloadProgressStage.CHECKING -> DownloadStatus.PREPARING
                            DownloadProgressStage.DOWNLOADING -> DownloadStatus.DOWNLOADING
                            DownloadProgressStage.CONVERTING -> DownloadStatus.CONVERTING
                            DownloadProgressStage.FINISHING -> DownloadStatus.CONVERTING
                            DownloadProgressStage.FINISHED -> DownloadStatus.FINISHED
                            DownloadProgressStage.FAILED -> DownloadStatus.FAILED
                        }
                        current.copy(
                            status = if (current.status == DownloadStatus.FAILED) current.status else mappedStatus,
                            progress = progress,
                            lastFinishedFile = progress.filename ?: current.lastFinishedFile
                        )
                    }
                }
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { current ->
            val isResettingFromFailed = current.status == DownloadStatus.FAILED
            if (isResettingFromFailed) {
                TermuxProgressReceiver.reset()
            }
            current.copy(
                urlInput = newUrl,
                error = if (isResettingFromFailed) null else current.error,
                status = if (isResettingFromFailed) DownloadStatus.READY else current.status,
                progress = if (isResettingFromFailed) null else current.progress
            )
        }
    }

    fun pasteUrl(pastedUrl: String) {
        onUrlChanged(pastedUrl)
    }

    fun clearUrl() {
        TermuxProgressReceiver.reset()
        _uiState.update { it.copy(urlInput = "", error = null, status = DownloadStatus.READY, progress = null) }
    }

    fun startDownload() {
        val rawInput = _uiState.value.urlInput
        val validation = UrlSanitizer.sanitize(rawInput)

        if (validation is UrlValidationResult.Invalid) {
            TermuxProgressReceiver.reset()
            _uiState.update {
                it.copy(
                    status = DownloadStatus.FAILED,
                    statusDetail = "Invalid YouTube URL",
                    error = DownloadError.InvalidUrl(validation.reason),
                    progress = null
                )
            }
            return
        }

        val sanitizedUrl = (validation as UrlValidationResult.Valid).sanitizedUrl

        // Reset any stale progress from previous downloads
        TermuxProgressReceiver.reset()
        val initialProgress = DownloadProgress(stage = DownloadProgressStage.CHECKING)

        // Update state to PREPARING
        _uiState.update {
            it.copy(
                status = DownloadStatus.PREPARING,
                activeUrl = sanitizedUrl,
                statusDetail = "Preparing download...",
                error = null,
                logOutput = null,
                progress = initialProgress
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
                TermuxProgressReceiver.reset()
                _uiState.update {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        statusDetail = dispatchResult.error.title,
                        error = dispatchResult.error,
                        progress = null
                    )
                }
            }
        }
    }

    private fun handleExecutionResult(result: TermuxExecutionResult) {
        if (result.isSuccess) {
            val currentProgress = _uiState.value.progress
            val finalFilename = currentProgress?.filename ?: _uiState.value.lastFinishedFile
            val finishedProgress = DownloadProgress(
                stage = DownloadProgressStage.FINISHED,
                percent = 100f,
                filename = finalFilename
            )
            TermuxProgressReceiver.updateProgress(finishedProgress)

            _uiState.update {
                it.copy(
                    status = DownloadStatus.FINISHED,
                    statusDetail = "Finished. High-quality MP3 saved to Downloads.",
                    error = null,
                    logOutput = result.fullOutput,
                    lastFinishedFile = finalFilename,
                    progress = finishedProgress
                )
            }
        } else {
            TermuxProgressReceiver.reset()
            val classifiedError = TermuxErrorClassifier.classify(result)
            _uiState.update {
                it.copy(
                    status = DownloadStatus.FAILED,
                    statusDetail = classifiedError.title,
                    error = classifiedError,
                    logOutput = result.fullOutput,
                    progress = null
                )
            }
        }
    }

    fun resetStatus() {
        TermuxProgressReceiver.reset()
        _uiState.update {
            it.copy(
                status = DownloadStatus.READY,
                statusDetail = "",
                error = null,
                progress = null
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
