package com.example.backend

import com.example.model.DownloadError
import com.example.model.TermuxInstallationStatus
import kotlinx.coroutines.flow.SharedFlow

sealed class DownloadDispatchResult {
    data object Success : DownloadDispatchResult()
    data class Failure(val error: DownloadError) : DownloadDispatchResult()
}

/**
 * Clean abstraction over the Termux execution layer.
 * Decouples the UI and ViewModel from Android intent/IPC details.
 */
interface TermuxBackend {
    /**
     * Checks if Termux is installed and has required permissions.
     */
    fun checkInstallation(): TermuxInstallationStatus

    /**
     * Sends the controlled audio download command to Termux.
     */
    fun startAudioDownload(sanitizedUrl: String): DownloadDispatchResult

    /**
     * Hot stream of execution results coming back from Termux.
     */
    val executionEvents: SharedFlow<TermuxExecutionResult>
}
