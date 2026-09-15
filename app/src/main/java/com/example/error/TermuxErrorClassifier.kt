package com.example.error

import com.example.backend.TermuxExecutionResult
import com.example.model.DownloadError

object TermuxErrorClassifier {

    /**
     * Inspects the output and exit codes of the Termux execution to produce
     * an accurate, human-readable DownloadError.
     * Note: Only called when result.isSuccess == false.
     */
    fun classify(result: TermuxExecutionResult): DownloadError {
        // Guard against misclassification if already successful
        if (result.isSuccess) {
            return DownloadError.UnknownFailure("Download succeeded.")
        }

        val combined = "${result.stderr}\n${result.stdout}\n${result.errmsg.orEmpty()}".lowercase()
        val exitCode = result.exitCode

        // 1. Backend configuration / permission / script issues
        if (result.hasTermuxInternalError ||
            combined.contains("allow-external-apps") ||
            combined.contains("the executable regular file not found") ||
            (combined.contains("no such file or directory") && combined.contains("tubep3-audio.sh")) ||
            exitCode == 10 ||
            combined.contains("android downloads folder is not accessible")
        ) {
            return DownloadError.BackendNotConfigured
        }

        // 2. yt-dlp unavailable
        if (exitCode == 11 ||
            combined.contains("yt-dlp executable not found") ||
            combined.contains("yt-dlp: command not found") ||
            combined.contains("yt-dlp: not found")
        ) {
            return DownloadError.YtDlpUnavailable
        }

        // 3. FFmpeg unavailable
        if (exitCode == 12 ||
            combined.contains("ffmpeg executable not found") ||
            combined.contains("ffmpeg: command not found") ||
            combined.contains("ffmpeg: not found") ||
            combined.contains("ffprobe: not found")
        ) {
            return DownloadError.FfmpegUnavailable
        }

        // 4. Invalid URL rejected by script
        if (exitCode == 13 || combined.contains("missing url parameter")) {
            return DownloadError.InvalidUrl("The YouTube URL parameter was missing or rejected.")
        }

        // 5. Network / Connectivity failures
        if (combined.contains("unable to download webpage") ||
            combined.contains("network is unreachable") ||
            combined.contains("name or service not known") ||
            combined.contains("temporary failure in name resolution") ||
            combined.contains("connection timed out") ||
            combined.contains("connection refused") ||
            combined.contains("timed out")
        ) {
            val detail = extractKeyErrorLine(result.stderr).ifBlank {
                "Network connection timed out or unreachable while contacting YouTube."
            }
            return DownloadError.NetworkFailure(detail)
        }

        // 6. Extraction failure (private, removed, geo-blocked, bot check)
        if (combined.contains("video unavailable") ||
            combined.contains("private video") ||
            combined.contains("this video has been removed") ||
            combined.contains("sign in to confirm") ||
            combined.contains("members-only") ||
            combined.contains("incomplete youtube id") ||
            combined.contains("extractorerror")
        ) {
            val detail = extractKeyErrorLine(result.stderr).ifBlank {
                "The requested video cannot be extracted by yt-dlp (it may be private, removed, or geo-blocked)."
            }
            return DownloadError.ExtractionFailure(detail)
        }

        // 7. Conversion failure
        if (exitCode == 21 ||
            combined.contains("conversion failed") ||
            combined.contains("ffmpeg failed") ||
            combined.contains("postprocessing: error")
        ) {
            val detail = extractKeyErrorLine(result.stderr).ifBlank {
                "FFmpeg encountered an error while converting the audio stream to MP3."
            }
            return DownloadError.ConversionFailure(detail)
        }

        // 8. Unknown failure fallback
        val snippet = extractKeyErrorLine(result.stderr).ifBlank {
            extractKeyErrorLine(result.stdout).ifBlank {
                result.errmsg ?: "Process finished with error exit code $exitCode."
            }
        }
        return DownloadError.UnknownFailure(snippet)
    }

    private fun extractKeyErrorLine(text: String): String {
        return text.lines()
            .map { it.trim() }
            .firstOrNull { line ->
                line.startsWith("ERROR:", ignoreCase = true) ||
                line.contains("error:", ignoreCase = true) ||
                line.startsWith("TubeP3:", ignoreCase = true)
            }
            ?: text.lines().lastOrNull { it.isNotBlank() }
            ?: ""
    }
}
