package com.example.model

sealed class DownloadError(
    val title: String,
    val description: String,
    val actionableHelp: String
) {
    data object TermuxUnavailable : DownloadError(
        title = "Termux Not Found",
        description = "Termux application is not installed on this device.",
        actionableHelp = "Install Termux from F-Droid or GitHub to enable yt-dlp backend execution."
    )

    data object PermissionDenied : DownloadError(
        title = "Termux Permission Required",
        description = "The Termux RUN_COMMAND permission is not granted to TubeP3.",
        actionableHelp = "Grant the 'Execute Termux commands' permission in app settings or setup dialog."
    )

    data object BackendNotConfigured : DownloadError(
        title = "Backend Not Configured",
        description = "Termux backend script or storage access is not set up.",
        actionableHelp = "Open the Setup Guide and run the 1-step backend configuration command in Termux."
    )

    data object YtDlpUnavailable : DownloadError(
        title = "yt-dlp Not Found",
        description = "The yt-dlp downloader binary could not be found in Termux.",
        actionableHelp = "Install yt-dlp inside Termux using: 'pkg install python && pip install yt-dlp'."
    )

    data object FfmpegUnavailable : DownloadError(
        title = "FFmpeg Not Found",
        description = "FFmpeg is required for MP3 audio conversion but was not found in Termux.",
        actionableHelp = "Install FFmpeg inside Termux using: 'pkg install ffmpeg'."
    )

    data class InvalidUrl(val details: String = "The provided URL is not a valid YouTube link.") : DownloadError(
        title = "Invalid URL",
        description = details,
        actionableHelp = "Please paste a valid YouTube video URL (e.g., https://youtu.be/... or https://youtube.com/watch?v=...)."
    )

    data class NetworkFailure(val details: String = "Network connection failed during audio download.") : DownloadError(
        title = "Network Failure",
        description = details,
        actionableHelp = "Check your device's internet connection or Wi-Fi and try again."
    )

    data class ExtractionFailure(val details: String = "yt-dlp could not extract stream from this video.") : DownloadError(
        title = "Extraction Failure",
        description = details,
        actionableHelp = "The video may be private, age-restricted, region-locked, or removed from YouTube."
    )

    data class ConversionFailure(val details: String = "FFmpeg failed to convert audio stream to MP3.") : DownloadError(
        title = "Audio Conversion Failure",
        description = details,
        actionableHelp = "Ensure FFmpeg is up to date in Termux: 'pkg upgrade ffmpeg'."
    )

    data class UnknownFailure(val details: String) : DownloadError(
        title = "Download Failed",
        description = details.ifBlank { "An unexpected error occurred during execution." },
        actionableHelp = "Review the execution logs or re-run the backend setup."
    )
}
