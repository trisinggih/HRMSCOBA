package com.jangeby.system.hardware

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun CameraPreviewWithFaceDetection(
    modifier: Modifier,
    onFaceStatusChanged: (Boolean) -> Unit,
    onPhotoCaptured: (String?) -> Unit,
    captureTrigger: Long
) {
    // Basic fallback for iOS - in a real app you'd wrap AVCaptureSession via UIKitView
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Camera Preview (iOS)")
    }
}
