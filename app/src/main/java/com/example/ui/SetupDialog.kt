package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.TermuxInstallationStatus
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkGraySurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightGraySecondary
import com.example.ui.theme.SubtleGrayBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TubeP3Red
import com.example.ui.theme.WhiteText
import com.example.util.TermuxScriptProvider

@Composable
fun SetupDialog(
    status: TermuxInstallationStatus,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .background(AmoledBlack, RoundedCornerShape(16.dp))
                    .border(1.dp, SubtleGrayBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    // Title
                    Text(
                        text = "Termux Backend Setup",
                        color = WhiteText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "TubeP3 uses Termux as the local yt-dlp & FFmpeg execution engine.",
                        color = LightGraySecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Environment Status Cards
                    StatusRow(
                        title = "Termux App",
                        isReady = status.isInstalled,
                        readyText = "Installed (${status.versionName ?: "v0.118+"})",
                        notReadyText = "Not Detected",
                        action = if (!status.isInstalled) {
                            {
                                Text(
                                    text = "Install from F-Droid or GitHub",
                                    color = LightGraySecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StatusRow(
                        title = "Execute Permission",
                        isReady = status.hasRunCommandPermission,
                        readyText = "Granted",
                        notReadyText = "Permission Needed",
                        action = if (!status.hasRunCommandPermission) {
                            {
                                Button(
                                    onClick = onRequestPermission,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TubeP3Red,
                                        contentColor = WhiteText
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .defaultMinSize(minHeight = 40.dp)
                                        .testTag("grant_permission_button")
                                ) {
                                    Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 1: Open Termux button
                    if (status.isInstalled) {
                        OutlinedButton(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux")
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    Toast.makeText(context, "Cannot open Termux automatically.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WhiteText),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("open_termux_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = WhiteText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Termux", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 1-Step Setup & Repair Command
                    Text(
                        text = "1-Step Setup & Repair Command (v0.2)",
                        color = WhiteText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Paste this into Termux for initial setup or to diagnose and repair broken dependencies. It runs non-destructive checks across Storage, FFmpeg, yt-dlp, and permissions:",
                        color = LightGraySecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Command Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkGraySurface, RoundedCornerShape(8.dp))
                            .border(1.dp, SubtleGrayBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        val hScroll = rememberScrollState()
                        Text(
                            text = TermuxScriptProvider.ONE_STEP_SETUP_COMMAND,
                            color = Color(0xFF80D8FF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .horizontalScroll(hScroll)
                                .heightIn(max = 110.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Copy Button
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("TubeP3 Setup Command", TermuxScriptProvider.ONE_STEP_SETUP_COMMAND)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Setup command copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TubeP3Red,
                            contentColor = WhiteText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("copy_setup_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Setup & Repair Command", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stages & Diagnostics Guide Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkGraySurface),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SubtleGrayBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Automated Diagnostic Stages",
                                color = WhiteText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Storage: Verifies write access to Android Downloads\n" +
                                        "• FFmpeg: Verifies 'ffmpeg -version' execution\n" +
                                        "• yt-dlp: Verifies 'yt-dlp --version' execution\n" +
                                        "• External Apps: Configures allow-external-apps safely\n" +
                                        "• Self-Test: Invokes backend verification without downloading\n" +
                                        "• Result: Emits TUBEP3_SETUP:READY on success or TUBEP3_SETUP:FAILED with recovery steps",
                                color = LightGraySecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Close Button
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("close_setup_dialog_button")
                    ) {
                        Text("Close", color = LightGraySecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    isReady: Boolean,
    readyText: String,
    notReadyText: String,
    action: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkGraySurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SubtleGrayBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = WhiteText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isReady) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isReady) readyText else notReadyText,
                        color = if (isReady) SuccessGreen else ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (action != null) {
                action()
            }
        }
    }
}
