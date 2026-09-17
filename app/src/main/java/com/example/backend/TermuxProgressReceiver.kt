package com.example.backend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.model.DownloadProgress
import com.example.model.DownloadProgressStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BroadcastReceiver that receives live progress events dispatched from the TubeP3 Termux backend.
 */
class TermuxProgressReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TermuxProgressReceiver"
        const val ACTION_PROGRESS = "com.example.tubep3.ACTION_PROGRESS"

        private val _progressFlow = MutableStateFlow<DownloadProgress?>(null)
        val progressFlow: StateFlow<DownloadProgress?> = _progressFlow.asStateFlow()

        fun updateProgress(progress: DownloadProgress?) {
            _progressFlow.value = progress
        }

        fun reset() {
            _progressFlow.value = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PROGRESS) return

        val stageStr = intent.getStringExtra("stage") ?: "DOWNLOADING"
        val stage = try {
            DownloadProgressStage.valueOf(stageStr.uppercase())
        } catch (e: Exception) {
            DownloadProgressStage.DOWNLOADING
        }

        val percent = extractFloat(intent, "percent")
        val downloaded = extractLong(intent, "downloaded", "downloaded_bytes")
        val total = extractLong(intent, "total", "total_bytes")
        val speed = extractLong(intent, "speed", "speed_bytes")
        val eta = extractInt(intent, "eta", "eta_seconds")
        val filename = intent.getStringExtra("filename")

        val progress = DownloadProgress(
            stage = stage,
            percent = percent,
            downloadedBytes = downloaded,
            totalBytes = total,
            speedBytesPerSecond = speed,
            etaSeconds = eta,
            filename = filename
        )

        Log.d(TAG, "Progress received: stage=$stage, percent=$percent, downloaded=$downloaded, total=$total, speed=$speed, eta=$eta, filename=$filename")
        _progressFlow.value = progress
    }

    private fun extractFloat(intent: Intent, key: String): Float? {
        if (!intent.hasExtra(key)) return null
        val value = intent.extras?.get(key)
        return when (value) {
            is Number -> value.toFloat().takeIf { it >= 0f }
            is String -> value.toFloatOrNull()?.takeIf { it >= 0f }
            else -> null
        }
    }

    private fun extractLong(intent: Intent, vararg keys: String): Long? {
        for (k in keys) {
            if (intent.hasExtra(k)) {
                val value = intent.extras?.get(k)
                val parsed = when (value) {
                    is Number -> value.toLong().takeIf { it >= 0L }
                    is String -> value.toLongOrNull()?.takeIf { it >= 0L }
                    else -> null
                }
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun extractInt(intent: Intent, vararg keys: String): Int? {
        for (k in keys) {
            if (intent.hasExtra(k)) {
                val value = intent.extras?.get(k)
                val parsed = when (value) {
                    is Number -> value.toInt().takeIf { it >= 0 }
                    is String -> value.toIntOrNull()?.takeIf { it >= 0 }
                    else -> null
                }
                if (parsed != null) return parsed
            }
        }
        return null
    }
}
