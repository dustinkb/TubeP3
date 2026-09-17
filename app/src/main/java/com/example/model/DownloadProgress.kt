package com.example.model

import java.util.Locale

/**
 * Stages of the download and audio processing lifecycle.
 */
enum class DownloadProgressStage(val displayText: String) {
    CHECKING("Checking URL..."),
    DOWNLOADING("Downloading"),
    CONVERTING("Converting to MP3"),
    FINISHING("Finishing..."),
    FINISHED("Download Complete"),
    FAILED("Download Failed")
}

/**
 * Real progress information emitted by yt-dlp and the TubeP3 Termux backend.
 * Uses nullable fields for values that may not be available for all streams.
 */
data class DownloadProgress(
    val stage: DownloadProgressStage = DownloadProgressStage.CHECKING,
    val percent: Float? = null,
    val downloadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Long? = null,
    val etaSeconds: Int? = null,
    val filename: String? = null
) {
    /**
     * Whether reliable percentage information is available.
     * When false or during conversion/finishing, an indeterminate animated indicator is used.
     */
    val hasDeterminatePercent: Boolean
        get() = stage == DownloadProgressStage.DOWNLOADING && percent != null && percent in 0f..100f

    /**
     * Formatted string showing downloaded size and total size when available (e.g. "3.8 MB / 8.1 MB" or "3.8 MB").
     */
    val formattedSizeProgress: String?
        get() {
            if (downloadedBytes == null) return null
            val downloadedStr = formatBytes(downloadedBytes)
            return if (totalBytes != null && totalBytes > 0L) {
                val totalStr = formatBytes(totalBytes)
                "$downloadedStr / $totalStr"
            } else {
                downloadedStr
            }
        }

    /**
     * Formatted speed and ETA string (e.g. "1.2 MB/s • ETA 4s").
     */
    val formattedSpeedAndEta: String?
        get() {
            val speedStr = speedBytesPerSecond?.let { if (it > 0L) "${formatBytes(it)}/s" else null }
            val etaStr = etaSeconds?.let { if (it >= 0) "ETA ${formatEta(it)}" else null }

            return when {
                speedStr != null && etaStr != null -> "$speedStr • $etaStr"
                speedStr != null -> speedStr
                etaStr != null -> etaStr
                else -> null
            }
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return if (digitGroups == 0) {
                "$bytes B"
            } else {
                String.format(Locale.US, "%.1f %s", value, units[digitGroups])
            }
        }

        fun formatEta(seconds: Int): String {
            return when {
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> {
                    val m = seconds / 60
                    val s = seconds % 60
                    "${m}m ${s}s"
                }
                else -> {
                    val h = seconds / 3600
                    val m = (seconds % 3600) / 60
                    "${h}h ${m}m"
                }
            }
        }
    }
}
