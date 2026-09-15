package com.example.backend

object TermuxConstants {
    const val TERMUX_PACKAGE_NAME = "com.termux"
    const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    // Termux RUN_COMMAND extras
    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

    // Termux RUN_COMMAND permissions
    const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"

    // Result Bundle extras passed back by Termux
    const val EXTRA_PLUGIN_RESULT_BUNDLE = "result"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"

    // Action for TubeP3 callback broadcast
    const val ACTION_TERMUX_RESULT = "com.example.tubep3.ACTION_TERMUX_RESULT"
}
