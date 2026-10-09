package com.jangeby.system.hardware

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FaceCameraView(
    modifier: Modifier = Modifier,
    title: String = "Scan Wajah",
    buttonText: String = "Ambil Foto",
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    onPhotoCaptured: (String?) -> Unit,
    onClose: () -> Unit
) {
    var faceDetected by remember { mutableStateOf(false) }
    var captureTrigger by remember { mutableStateOf(0L) }
    var isCapturing by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview underlying layer
        CameraPreviewWithFaceDetection(
            modifier = Modifier.fillMaxSize(),
            onFaceStatusChanged = { detected ->
                faceDetected = detected
            },
            onPhotoCaptured = { base64 ->
                isCapturing = false
                onPhotoCaptured(base64)
            },
            captureTrigger = captureTrigger
        )

        // Top Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xBB000000))
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Watermark Overlay (bottom left before the panel)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 180.dp, start = 16.dp)
                .background(Color(0x66000000))
                .padding(8.dp)
        ) {
            Text(text = "Lat: $latitude", color = Color.White, fontSize = 12.sp)
            Text(text = "Long: $longitude", color = Color.White, fontSize = 12.sp)
        }

        // Bottom Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xBB000000))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (faceDetected) "✅ Wajah terdeteksi, silakan tap tombol" else "Arahkan wajah Anda ke kamera",
                color = if (faceDetected) Color(0xFF4CAF50) else Color(0xFFCCFFFFFF),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    if (faceDetected && !isCapturing) {
                        isCapturing = true
                        captureTrigger += 1L
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = faceDetected && !isCapturing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
