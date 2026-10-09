package com.jangeby.system.hardware

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraPreviewWithFaceDetection(
    modifier: Modifier = Modifier,
    onFaceStatusChanged: (Boolean) -> Unit,
    onPhotoCaptured: (String?) -> Unit,
    captureTrigger: Long
)
