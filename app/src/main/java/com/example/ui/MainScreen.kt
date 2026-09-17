package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadError
import com.example.model.DownloadState
import com.example.model.DownloadStatus
import com.example.model.TermuxInstallationStatus
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkGraySurface
import com.example.ui.theme.DarkerGrayDisabled
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightGraySecondary
import com.example.ui.theme.SubtleGrayBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TubeP3Red
import com.example.ui.theme.WhiteText

@Composable
fun MainScreen(
    state: DownloadState,
    termuxStatus: TermuxInstallationStatus,
    onUrlChange: (String) -> Unit,
    onPasteUrl: (String) -> Unit,
    onClearUrl: () -> Unit,
    onDownloadClick: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSetupDialog by remember { mutableStateOf(false) }
    var showLogDetails by remember { mutableStateOf(false) }
    val mainScrollState = rememberScrollState()

    val isBusy = state.status == DownloadStatus.PREPARING ||
            state.status == DownloadStatus.DOWNLOADING ||
            state.status == DownloadStatus.CONVERTING

    Scaffold(
        containerColor = AmoledBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(mainScrollState)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar with Setup Icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showSetupDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_setup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Termux Setup & Status",
                            tint = if (termuxStatus.isReady) LightGraySecondary else TubeP3Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Brand Header with responsive display sizing
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TubeP3",
                        color = WhiteText,
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("app_title")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "YouTube Audio Downloader",
                        color = LightGraySecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.25.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // URL Input Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "YouTube URL",
                        color = LightGraySecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = state.urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_text_field"),
                        placeholder = {
                            Text(
                                text = "https://www.youtube.com/watch?v=...",
                                color = DarkerGrayDisabled,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WhiteText,
                            unfocusedTextColor = WhiteText,
                            disabledTextColor = DarkerGrayDisabled,
                            focusedContainerColor = DarkGraySurface,
                            unfocusedContainerColor = DarkGraySurface,
                            disabledContainerColor = DarkGraySurface,
                            focusedBorderColor = TubeP3Red,
                            unfocusedBorderColor = SubtleGrayBorder,
                            disabledBorderColor = SubtleGrayBorder,
                            cursorColor = TubeP3Red
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (state.urlInput.isNotBlank() && !isBusy) {
                                    onDownloadClick()
                                }
                            }
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.urlInput.isNotEmpty() && !isBusy) {
                                    IconButton(
                                        onClick = onClearUrl,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL",
                                            tint = LightGraySecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else if (!isBusy) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val item = clipboard.primaryClip?.getItemAt(0)
                                            val text = item?.text?.toString() ?: ""
                                            if (text.isNotBlank()) {
                                                onPasteUrl(text.trim())
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste from Clipboard",
                                            tint = LightGraySecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Action Button (flexible height with safe minimum touch target)
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onDownloadClick()
                        },
                        enabled = state.urlInput.isNotBlank() && !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("download_audio_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TubeP3Red,
                            contentColor = WhiteText,
                            disabledContainerColor = DarkGraySurface,
                            disabledContentColor = DarkerGrayDisabled
                        )
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = WhiteText,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.status.displayText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DOWNLOAD AUDIO",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Status Area
                StatusArea(
                    state = state,
                    termuxReady = termuxStatus.isReady,
                    onOpenSetup = { showSetupDialog = true },
                    onToggleLog = { showLogDetails = !showLogDetails },
                    isLogVisible = showLogDetails,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature Specifications Footer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    HorizontalDivider(
                        color = SubtleGrayBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Text(
                        text = "Best available audio  •  High-quality MP3  •  Save to Downloads",
                        color = LightGraySecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    HorizontalDivider(
                        color = SubtleGrayBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    if (showSetupDialog) {
        SetupDialog(
            status = termuxStatus,
            onRequestPermission = onRequestPermission,
            onDismiss = { showSetupDialog = false }
        )
    }
}

@Composable
private fun StatusArea(
    state: DownloadState,
    termuxReady: Boolean,
    onOpenSetup: () -> Unit,
    onToggleLog: () -> Unit,
    isLogVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val logScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .background(DarkGraySurface, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                when (state.status) {
                    DownloadStatus.FAILED -> ErrorRed
                    DownloadStatus.FINISHED -> SuccessGreen
                    else -> SubtleGrayBorder
                },
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    when (state.status) {
                        DownloadStatus.READY -> {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = LightGraySecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DownloadStatus.PREPARING,
                        DownloadStatus.DOWNLOADING,
                        DownloadStatus.CONVERTING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = TubeP3Red,
                                strokeWidth = 2.dp
                            )
                        }
                        DownloadStatus.FINISHED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DownloadStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = state.status.displayText,
                        color = when (state.status) {
                            DownloadStatus.FAILED -> ErrorRed
                            DownloadStatus.FINISHED -> SuccessGreen
                            DownloadStatus.READY -> WhiteText
                            else -> WhiteText
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("status_text")
                    )
                }

                if (!termuxReady) {
                    TextButton(
                        onClick = onOpenSetup,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("termux_setup_badge_button")
                    ) {
                        Text(
                            text = "Setup Needed",
                            color = TubeP3Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Error or Status Details
            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error.description,
                    color = WhiteText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.error.actionableHelp,
                    color = LightGraySecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (state.error is DownloadError.BackendNotConfigured ||
                    state.error is DownloadError.TermuxUnavailable ||
                    state.error is DownloadError.PermissionDenied ||
                    state.error is DownloadError.YtDlpUnavailable ||
                    state.error is DownloadError.FfmpegUnavailable
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenSetup,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TubeP3Red,
                            contentColor = WhiteText
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("resolve_error_setup_button")
                    ) {
                        Text(
                            text = "Open Termux Setup Guide",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (state.statusDetail.isNotBlank() && state.statusDetail != state.status.displayText) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.statusDetail,
                    color = LightGraySecondary,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }

            // Optional raw execution log viewer (for diagnostics)
            if (!state.logOutput.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SubtleGrayBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Execution Log",
                        color = LightGraySecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = onToggleLog,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isLogVisible) "Hide Log" else "Show Log",
                            color = LightGraySecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                AnimatedVisibility(visible = isLogVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AmoledBlack, RoundedCornerShape(6.dp))
                            .border(1.dp, SubtleGrayBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = state.logOutput,
                            color = LightGraySecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .heightIn(max = 140.dp)
                                .verticalScroll(logScrollState)
                        )
                    }
                }
            }
        }
    }
}
