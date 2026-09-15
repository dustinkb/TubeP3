package com.example.model

data class TermuxInstallationStatus(
    val isInstalled: Boolean = false,
    val hasRunCommandPermission: Boolean = false,
    val versionName: String? = null
) {
    val isReady: Boolean
        get() = isInstalled && hasRunCommandPermission
}
