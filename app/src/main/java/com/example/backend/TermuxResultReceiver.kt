package com.example.backend

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TermuxResultReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TermuxResultReceiver"
        private val _resultsFlow = MutableSharedFlow<TermuxExecutionResult>(extraBufferCapacity = 16)
        val resultsFlow = _resultsFlow.asSharedFlow()

        fun postResult(result: TermuxExecutionResult) {
            _resultsFlow.tryEmit(result)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast action: ${intent.action}")

        val resultBundle: Bundle? = intent.getBundleExtra(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE)
            ?: intent.extras

        if (resultBundle != null) {
            for (key in resultBundle.keySet()) {
                Log.d(TAG, "Result bundle key: '$key' = '${resultBundle.get(key)}'")
            }
        }

        // Extract stdout
        val stdout = resultBundle?.getString("stdout")
            ?: intent.getStringExtra("stdout")
            ?: resultBundle?.getString("out")
            ?: ""

        // Extract stderr
        val stderr = resultBundle?.getString("stderr")
            ?: intent.getStringExtra("stderr")
            ?: ""

        // Extract exit code across all known Termux keys
        var exitCode = extractInt(resultBundle, intent, "exitCode", "exit_code", "result", "code")

        // If the backend script explicitly printed the finished marker,
        // it definitively completed with exit status 0
        if (stdout.contains("TUBEP3_STAGE:FINISHED") || stderr.contains("TUBEP3_STAGE:FINISHED")) {
            exitCode = 0
        }

        // Extract internal Termux err code (Activity.RESULT_OK = -1 indicates no internal plugin error)
        val errCode = extractInt(resultBundle, intent, "err", "err_code", "errorCode", default = Activity.RESULT_OK)

        // Extract internal Termux error message
        val errmsg = resultBundle?.getString("errmsg")
            ?: intent.getStringExtra("errmsg")
            ?: resultBundle?.getString("err_msg")

        val executionResult = TermuxExecutionResult(
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            errCode = errCode,
            errmsg = errmsg
        )

        Log.d(
            TAG,
            "Parsed Termux execution result: exitCode=$exitCode, isSuccess=${executionResult.isSuccess}, " +
                "errCode=$errCode, stdoutLength=${stdout.length}, stderrLength=${stderr.length}"
        )

        CoroutineScope(Dispatchers.Default).launch {
            _resultsFlow.emit(executionResult)
        }
    }

    private fun extractInt(
        bundle: Bundle?,
        intent: Intent,
        vararg keys: String,
        default: Int = -1
    ): Int {
        if (bundle != null) {
            for (key in keys) {
                if (bundle.containsKey(key)) {
                    val value = bundle.get(key)
                    val parsed = when (value) {
                        is Number -> value.toInt()
                        is String -> value.toIntOrNull()
                        else -> null
                    }
                    if (parsed != null) return parsed
                }
            }
        }
        for (key in keys) {
            if (intent.hasExtra(key)) {
                val value = intent.extras?.get(key)
                val parsed = when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull()
                    else -> null
                }
                if (parsed != null) return parsed
            }
        }
        return default
    }
}
