package com.jangeby.system.hardware

import androidx.compose.runtime.Composable

expect class CameraLauncher {
    fun launchCamera()
}

@Composable
expect fun rememberCameraLauncher(onResult: (CameraResult) -> Unit): CameraLauncher
