package com.example.model

data class DownloadState(
    val status: DownloadStatus = DownloadStatus.READY,
    val urlInput: String = "",
    val activeUrl: String? = null,
    val statusDetail: String = "",
    val error: DownloadError? = null,
    val logOutput: String? = null,
    val lastFinishedFile: String? = null
)
