package com.example.backend

import android.app.Activity

data class TermuxExecutionResult(
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = "",
    val errCode: Int = Activity.RESULT_OK,
    val errmsg: String? = null
) {
    /**
     * An internal Termux error outside of the shell command execution
     * (e.g. Termux service crash, external commands disabled in termux.properties,
     * or missing RUN_COMMAND permission).
     *
     * In Android/Termux:
     * Activity.RESULT_OK (-1) means SUCCESS (no internal Termux error).
     * If errCode is Activity.RESULT_OK or 0, or errmsg is blank, there is NO internal error.
     */
    val hasTermuxInternalError: Boolean
        get() = !errmsg.isNullOrBlank() && errCode != Activity.RESULT_OK && errCode != 0

    /**
     * The command execution is considered successful if:
     * 1. The script explicitly emitted the finished stage marker, OR
     * 2. The exitCode is 0 and no internal Termux plugin error occurred.
     *
     * Note: yt-dlp/ffmpeg printing informational or progress messages to stderr
     * when exitCode == 0 is completely normal and NOT a failure.
     */
    val isSuccess: Boolean
        get() {
            if (stdout.contains("TUBEP3_STAGE:FINISHED") || stderr.contains("TUBEP3_STAGE:FINISHED")) {
                return true
            }
            if (hasTermuxInternalError) {
                return false
            }
            return exitCode == 0
        }

    val fullOutput: String
        get() = buildString {
            if (stdout.isNotBlank()) {
                append(stdout.trim())
            }
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(stderr.trim())
            }
            if (!errmsg.isNullOrBlank() && hasTermuxInternalError) {
                if (isNotEmpty()) append("\n")
                append("Termux Internal Error: ").append(errmsg.trim())
            }
        }
}
