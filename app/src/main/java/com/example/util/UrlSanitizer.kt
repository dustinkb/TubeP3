package com.example.util

import android.net.Uri
import java.util.regex.Pattern

sealed class UrlValidationResult {
    data class Valid(val sanitizedUrl: String) : UrlValidationResult()
    data class Invalid(val reason: String) : UrlValidationResult()
}

object UrlSanitizer {

    // YouTube Video ID regex: 11 characters of [a-zA-Z0-9_-]
    private val VIDEO_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$")

    // Dangerous shell metacharacters that must never be present in the URL parameter
    private val DANGEROUS_SHELL_CHARS = Pattern.compile("[`$\\;&|<>\"'\\{\\}\\(\\)\\\\\\n\\r\\t\\s]")

    private val ALLOWED_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be"
    )

    /**
     * Strictly validates and sanitizes a user-provided YouTube URL.
     * Prevents shell command injection and ensures only safe, valid YouTube URLs are passed.
     */
    fun sanitize(rawInput: String?): UrlValidationResult {
        if (rawInput.isNullOrBlank()) {
            return UrlValidationResult.Invalid("URL cannot be empty.")
        }

        val trimmed = rawInput.trim()

        // Fast reject any input with dangerous shell characters
        if (DANGEROUS_SHELL_CHARS.matcher(trimmed).find()) {
            return UrlValidationResult.Invalid("URL contains invalid or dangerous characters.")
        }

        // Add https prefix if user omitted it
        val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val uri = try {
            Uri.parse(withScheme)
        } catch (e: Exception) {
            return UrlValidationResult.Invalid("Malformed URL format.")
        }

        val host = uri.host?.lowercase() ?: return UrlValidationResult.Invalid("Missing URL host.")

        if (ALLOWED_HOSTS.none { host == it || host.endsWith(".$it") }) {
            return UrlValidationResult.Invalid("Only YouTube URLs are supported (e.g. youtube.com or youtu.be).")
        }

        // Handle youtu.be/<id>
        if (host == "youtu.be") {
            val videoId = uri.pathSegments.firstOrNull() ?: ""
            if (!VIDEO_ID_PATTERN.matcher(videoId).matches()) {
                return UrlValidationResult.Invalid("Invalid YouTube video ID format.")
            }
            return UrlValidationResult.Valid("https://youtu.be/$videoId")
        }

        // Handle youtube.com/watch?v=<id>
        val videoIdParam = uri.getQueryParameter("v")
        if (!videoIdParam.isNullOrBlank()) {
            if (!VIDEO_ID_PATTERN.matcher(videoIdParam).matches()) {
                return UrlValidationResult.Invalid("Invalid YouTube video ID in URL.")
            }
            return UrlValidationResult.Valid("https://www.youtube.com/watch?v=$videoIdParam")
        }

        // Handle youtube.com/shorts/<id>
        val pathSegments = uri.pathSegments
        if (pathSegments.size >= 2 && pathSegments[0] == "shorts") {
            val shortId = pathSegments[1]
            if (!VIDEO_ID_PATTERN.matcher(shortId).matches()) {
                return UrlValidationResult.Invalid("Invalid YouTube Shorts ID.")
            }
            return UrlValidationResult.Valid("https://www.youtube.com/shorts/$shortId")
        }

        // Handle youtube.com/embed/<id> or /v/<id>
        if (pathSegments.size >= 2 && (pathSegments[0] == "embed" || pathSegments[0] == "v")) {
            val embedId = pathSegments[1]
            if (VIDEO_ID_PATTERN.matcher(embedId).matches()) {
                return UrlValidationResult.Valid("https://www.youtube.com/watch?v=$embedId")
            }
        }

        return UrlValidationResult.Invalid("Could not locate a valid YouTube video ID in the URL.")
    }
}
