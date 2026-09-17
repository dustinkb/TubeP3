package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.backend.TermuxExecutionResult
import com.example.backend.TermuxProgressReceiver
import com.example.error.TermuxErrorClassifier
import com.example.model.DownloadError
import com.example.model.DownloadProgress
import com.example.model.DownloadProgressStage
import com.example.util.TermuxScriptProvider
import com.example.util.UrlSanitizer
import com.example.util.UrlValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `One-step setup command contains all Version 0_2 stage markers and verification steps`() {
        val command = TermuxScriptProvider.ONE_STEP_SETUP_COMMAND
        assertTrue(command.contains("TUBEP3_SETUP:STARTING"))
        assertTrue(command.contains("TUBEP3_SETUP:UPDATING"))
        assertTrue(command.contains("TUBEP3_SETUP:STORAGE"))
        assertTrue(command.contains("TUBEP3_SETUP:FFMPEG"))
        assertTrue(command.contains("TUBEP3_SETUP:YTDLP"))
        assertTrue(command.contains("TUBEP3_SETUP:BACKEND"))
        assertTrue(command.contains("TUBEP3_SETUP:EXTERNAL"))
        assertTrue(command.contains("TUBEP3_SETUP:VERIFYING"))
        assertTrue(command.contains("TUBEP3_SETUP:READY"))
        assertTrue(command.contains("TUBEP3_SETUP:FAILED"))

        // Executable verification
        assertTrue(command.contains("ffmpeg -version"))
        assertTrue(command.contains("--version"))
        assertTrue(command.contains("dpkg --configure -a"))

        // Final ready summary checkmarks
        assertTrue(command.contains("=== TubeP3 backend ready! ==="))
        assertTrue(command.contains("✓ Termux packages"))
        assertTrue(command.contains("✓ FFmpeg"))
        assertTrue(command.contains("✓ yt-dlp"))
        assertTrue(command.contains("✓ Android storage"))
        assertTrue(command.contains("✓ TubeP3 backend"))
        assertTrue(command.contains("✓ External command execution"))
    }

    @Test
    fun `Backend script contains non-destructive self-test mode and preserves standard download flags`() {
        val script = TermuxScriptProvider.BACKEND_SCRIPT_CONTENT
        assertTrue(script.contains("--self-test"))
        assertTrue(script.contains("TUBEP3_STAGE:SELF_TEST_OK"))

        // Baseline yt-dlp download flags preserved
        assertTrue(script.contains("-f bestaudio"))
        assertTrue(script.contains("-x"))
        assertTrue(script.contains("--audio-format mp3"))
        assertTrue(script.contains("--audio-quality 0"))
        assertTrue(script.contains("--embed-thumbnail"))
        assertTrue(script.contains("--embed-metadata"))
        assertTrue(script.contains("TUBEP3_STAGE:FINISHED"))
        assertTrue(script.contains("TUBEP3_STAGE:DOWNLOADING"))
    }

    @Test
    fun `DownloadProgress formatting utilities format sizes speeds and ETAs accurately`() {
        val progress = DownloadProgress(
            stage = DownloadProgressStage.DOWNLOADING,
            percent = 45.5f,
            downloadedBytes = 5_242_880L, // 5.0 MB
            totalBytes = 10_485_760L,     // 10.0 MB
            speedBytesPerSecond = 1_572_864L, // 1.5 MB/s
            etaSeconds = 4,
            filename = "Track.mp3"
        )

        assertTrue(progress.hasDeterminatePercent)
        assertEquals("5.0 MB / 10.0 MB", progress.formattedSizeProgress)
        assertEquals("1.5 MB/s • ETA 4s", progress.formattedSpeedAndEta)

        val indeterminateProgress = DownloadProgress(
            stage = DownloadProgressStage.DOWNLOADING,
            percent = null,
            downloadedBytes = 2_097_152L,
            totalBytes = null,
            speedBytesPerSecond = 524_288L,
            etaSeconds = null
        )

        assertFalse(indeterminateProgress.hasDeterminatePercent)
        assertEquals("2.0 MB", indeterminateProgress.formattedSizeProgress)
        assertEquals("512.0 KB/s", indeterminateProgress.formattedSpeedAndEta)

        val convertingProgress = DownloadProgress(
            stage = DownloadProgressStage.CONVERTING,
            filename = "Song.mp3"
        )
        assertEquals("Converting to MP3", convertingProgress.stage.displayText)
        assertFalse(convertingProgress.hasDeterminatePercent)
    }

    @Test
    fun `TermuxProgressReceiver parses broadcast intent correctly and emits to progressFlow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = TermuxProgressReceiver()

        val intent = Intent(TermuxProgressReceiver.ACTION_PROGRESS).apply {
            putExtra("stage", "DOWNLOADING")
            putExtra("percent", 68.2f)
            putExtra("downloaded", 7_340_032L)
            putExtra("total", 10_485_760L)
            putExtra("speed", 1_048_576L)
            putExtra("eta", 3)
            putExtra("filename", "CoolSong.mp3")
        }

        receiver.onReceive(context, intent)

        val emitted = TermuxProgressReceiver.progressFlow.value
        assertNotNull(emitted)
        assertEquals(DownloadProgressStage.DOWNLOADING, emitted!!.stage)
        assertEquals(68.2f, emitted.percent ?: 0f, 0.01f)
        assertEquals(7_340_032L, emitted.downloadedBytes)
        assertEquals(10_485_760L, emitted.totalBytes)
        assertEquals(1_048_576L, emitted.speedBytesPerSecond)
        assertEquals(3, emitted.etaSeconds)
        assertEquals("CoolSong.mp3", emitted.filename)

        TermuxProgressReceiver.reset()
        assertNull(TermuxProgressReceiver.progressFlow.value)
    }

    @Test
    fun `tubep3-audio script has broadcast progress integration`() {
        val script = TermuxScriptProvider.BACKEND_SCRIPT_CONTENT
        assertTrue(script.contains("com.example.tubep3.ACTION_PROGRESS"))
        assertTrue(script.contains("PROGRESS_PARSER"))
        assertTrue(script.contains("--newline"))
        assertTrue(script.contains("TUBEP3_STAGE:DOWNLOADING"))
        assertTrue(script.contains("TUBEP3_STAGE:FINISHED"))
        assertTrue(script.contains("TUBEP3_STAGE:FAILED"))
    }
}
