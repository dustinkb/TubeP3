package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.backend.TermuxConstants
import com.example.backend.TermuxRunCommandBackend
import com.example.ui.MainScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.TubeP3Theme
import com.example.viewmodel.TubeP3ViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TubeP3ViewModel by viewModels {
        TubeP3ViewModel.Factory(TermuxRunCommandBackend(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TubeP3Theme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val termuxStatus by viewModel.termuxStatus.collectAsStateWithLifecycle()

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    viewModel.refreshTermuxStatus()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    MainScreen(
                        state = state,
                        termuxStatus = termuxStatus,
                        onUrlChange = viewModel::onUrlChanged,
                        onPasteUrl = viewModel::pasteUrl,
                        onClearUrl = viewModel::clearUrl,
                        onDownloadClick = viewModel::startDownload,
                        onRequestPermission = {
                            permissionLauncher.launch(TermuxConstants.PERMISSION_RUN_COMMAND)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTermuxStatus()
    }
}
