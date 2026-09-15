package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.DownloadState
import com.example.model.TermuxInstallationStatus
import com.example.ui.MainScreen
import com.example.ui.theme.TubeP3Theme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun tubep3_main_screen_screenshot() {
        composeTestRule.setContent {
            TubeP3Theme {
                MainScreen(
                    state = DownloadState(urlInput = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
                    termuxStatus = TermuxInstallationStatus(isInstalled = true, hasRunCommandPermission = true),
                    onUrlChange = {},
                    onPasteUrl = {},
                    onClearUrl = {},
                    onDownloadClick = {},
                    onRequestPermission = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tubep3_main.png")
    }
}
