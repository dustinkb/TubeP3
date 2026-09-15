package com.example.backend

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.DownloadError
import com.example.model.TermuxInstallationStatus
import com.example.util.TermuxScriptProvider
import kotlinx.coroutines.flow.SharedFlow

class TermuxRunCommandBackend(
    private val context: Context
) : TermuxBackend {

    companion object {
        private const val TAG = "TermuxBackend"
    }

    override val executionEvents: SharedFlow<TermuxExecutionResult> =
        TermuxResultReceiver.resultsFlow

    override fun checkInstallation(): TermuxInstallationStatus {
        val pm = context.packageManager
        return try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(TermuxConstants.TERMUX_PACKAGE_NAME, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(TermuxConstants.TERMUX_PACKAGE_NAME, 0)
            }

            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                TermuxConstants.PERMISSION_RUN_COMMAND
            ) == PackageManager.PERMISSION_GRANTED

            TermuxInstallationStatus(
                isInstalled = true,
                hasRunCommandPermission = hasPermission,
                versionName = pkgInfo.versionName
            )
        } catch (e: PackageManager.NameNotFoundException) {
            TermuxInstallationStatus(
                isInstalled = false,
                hasRunCommandPermission = false,
                versionName = null
            )
        }
    }

    override fun startAudioDownload(sanitizedUrl: String): DownloadDispatchResult {
        val installStatus = checkInstallation()
        if (!installStatus.isInstalled) {
            return DownloadDispatchResult.Failure(DownloadError.TermuxUnavailable)
        }

        if (!installStatus.hasRunCommandPermission) {
            return DownloadDispatchResult.Failure(DownloadError.PermissionDenied)
        }

        try {
            // Explicit callback Intent to receive command result from Termux
            val callbackIntent = Intent(context, TermuxResultReceiver::class.java).apply {
                action = TermuxConstants.ACTION_TERMUX_RESULT
                setPackage(context.packageName)
            }

            val requestCode = (System.currentTimeMillis() and 0xFFFF).toInt()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                callbackIntent,
                flags
            )

            // Intent for Termux RUN_COMMAND service
            val termuxIntent = Intent().apply {
                setClassName(
                    TermuxConstants.TERMUX_PACKAGE_NAME,
                    TermuxConstants.TERMUX_RUN_COMMAND_SERVICE
                )
                action = TermuxConstants.ACTION_RUN_COMMAND

                // Use bash to invoke the dedicated TubeP3 backend script
                // Passing sanitized URL strictly as an argv element
                putExtra(TermuxConstants.EXTRA_COMMAND_PATH, TermuxScriptProvider.BASH_ABSOLUTE_PATH)
                putExtra(
                    TermuxConstants.EXTRA_ARGUMENTS,
                    arrayOf(TermuxScriptProvider.SCRIPT_ABSOLUTE_PATH, sanitizedUrl)
                )
                putExtra(TermuxConstants.EXTRA_WORKDIR, "/data/data/com.termux/files/home")
                putExtra(TermuxConstants.EXTRA_BACKGROUND, true)
                putExtra(TermuxConstants.EXTRA_SESSION_ACTION, "0")
                putExtra(TermuxConstants.EXTRA_PENDING_INTENT, pendingIntent)
            }

            Log.d(TAG, "Dispatching RUN_COMMAND to Termux with URL: $sanitizedUrl")
            context.startService(termuxIntent)
            return DownloadDispatchResult.Success

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while launching Termux RUN_COMMAND", e)
            return DownloadDispatchResult.Failure(
                DownloadError.BackendNotConfigured
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error dispatching to Termux", e)
            return DownloadDispatchResult.Failure(
                DownloadError.UnknownFailure("Failed to send command to Termux: ${e.localizedMessage ?: "Unknown error"}")
            )
        }
    }
}
