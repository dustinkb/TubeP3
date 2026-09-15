package com.example

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.backend.TermuxExecutionResult
import com.example.error.TermuxErrorClassifier
import com.example.model.DownloadError
import com.example.util.TermuxScriptProvider
import com.example.util.UrlSanitizer
import com.example.util.UrlValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name is TubeP3`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TubeP3", appName)
    }

    @Test
    fun `UrlSanitizer validates valid youtube URLs`() {
        val standardUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val result = UrlSanitizer.sanitize(standardUrl)
        assertTrue(result is UrlValidationResult.Valid)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", (result as UrlValidationResult.Valid).sanitizedUrl)

        val shortUrl = "https://youtu.be/dQw4w9WgXcQ"
        val shortResult = UrlSanitizer.sanitize(shortUrl)
        assertTrue(shortResult is UrlValidationResult.Valid)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", (shortResult as UrlValidationResult.Valid).sanitizedUrl)
    }

    @Test
    fun `UrlSanitizer rejects dangerous injection attempts`() {
        val injection = "https://www.youtube.com/watch?v=dQw4w9WgXcQ; rm -rf /"
        val result = UrlSanitizer.sanitize(injection)
        assertTrue(result is UrlValidationResult.Invalid)

        val maliciousSubshell = "https://youtu.be/$(whoami)"
        val result2 = UrlSanitizer.sanitize(maliciousSubshell)
        assertTrue(result2 is UrlValidationResult.Invalid)
    }

    @Test
    fun `TermuxExecutionResult recognizes exit code 0 and RESULT_OK as success`() {
        val successResult = TermuxExecutionResult(
            exitCode = 0,
            stdout = "TUBEP3_STAGE:DOWNLOADING\n[youtube] Extracting URL\n[youtube] Downloading webpage\nTUBEP3_STAGE:FINISHED",
            stderr = "",
            errCode = Activity.RESULT_OK,
            errmsg = null
        )
        assertTrue("Expected isSuccess to be true", successResult.isSuccess)
    }

    @Test
    fun `TermuxExecutionResult recognizes success even when stderr contains yt-dlp warnings`() {
        val successWithWarnings = TermuxExecutionResult(
            exitCode = 0,
            stdout = "TUBEP3_STAGE:DOWNLOADING\n[youtube] Downloading webpage\nTUBEP3_STAGE:FINISHED",
            stderr = "WARNING: [youtube] Some non-fatal warning occurred",
            errCode = Activity.RESULT_OK,
            errmsg = null
        )
        assertTrue("Expected isSuccess to be true with exitCode 0", successWithWarnings.isSuccess)
    }

    @Test
    fun `TermuxExecutionResult with stage finished marker is treated as success`() {
        val stageFinishedResult = TermuxExecutionResult(
            exitCode = -1,
            stdout = "TUBEP3_STAGE:FINISHED\nTubeP3: Successfully downloaded and converted to MP3 in Downloads.",
            stderr = "",
            errCode = Activity.RESULT_OK
        )
        assertTrue("Expected isSuccess to be true when stage is finished", stageFinishedResult.isSuccess)
    }

    @Test
    fun `TermuxErrorClassifier identifies missing dependencies correctly`() {
        val ytdlpMissing = TermuxExecutionResult(
            exitCode = 11,
            stderr = "ERROR: yt-dlp executable not found in PATH"
        )
        val classified = TermuxErrorClassifier.classify(ytdlpMissing)
        assertTrue(classified is DownloadError.YtDlpUnavailable)

        val ffmpegMissing = TermuxExecutionResult(
            exitCode = 12,
            stderr = "ERROR: ffmpeg executable not found in PATH"
        )
        val ffmpegClassified = TermuxErrorClassifier.classify(ffmpegMissing)
        assertTrue(ffmpegClassified is DownloadError.FfmpegUnavailable)
    }

    @Test
    fun `TermuxErrorClassifier identifies external apps disabled correctly`() {
        val externalAppsDisabled = TermuxExecutionResult(
            exitCode = -1,
            stderr = "",
            errCode = 1,
            errmsg = "Execution of external commands disabled in ~/.termux/termux.properties"
        )
        assertFalse(externalAppsDisabled.isSuccess)
        val classified = TermuxErrorClassifier.classify(externalAppsDisabled)
        assertTrue(classified is DownloadError.BackendNotConfigured)
    }

    @Test
    fun `One-step setup command configures allow-external-apps and reloads settings`() {
        val command = TermuxScriptProvider.ONE_STEP_SETUP_COMMAND
        assertTrue(command.contains("allow-external-apps=true"))
        assertTrue(command.contains("termux-reload-settings"))
        assertTrue(command.contains("TubeP3: External app execution enabled."))
        assertTrue(command.contains("grep -q '^allow-external-apps=true$'"))
    }
}
