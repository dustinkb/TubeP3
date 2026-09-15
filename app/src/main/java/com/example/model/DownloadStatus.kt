package com.example.model

enum class DownloadStatus(val displayText: String) {
    READY("Ready"),
    PREPARING("Preparing download..."),
    DOWNLOADING("Downloading..."),
    CONVERTING("Converting to MP3..."),
    FINISHED("Finished"),
    FAILED("Download failed")
}
