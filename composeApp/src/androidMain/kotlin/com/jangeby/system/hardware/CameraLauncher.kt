package com.jangeby.system.hardware

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

actual class CameraLauncher(
    private val onLaunch: () -> Unit
) {
    actual fun launchCamera() {
        onLaunch()
    }
}

@Composable
actual fun rememberCameraLauncher(onResult: (CameraResult) -> Unit): CameraLauncher {
    val context = LocalContext.current

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                onResult(CameraResult("data:image/jpeg;base64,$base64Image", true))
            } catch (e: Exception) {
                onResult(CameraResult(null, false, "Gagal memproses foto: ${e.message}"))
            }
        } else {
            onResult(CameraResult(null, false, "Pengambilan foto dibatalkan atau tidak tersedia"))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                takePictureLauncher.launch(null)
            } catch (e: Exception) {
                onResult(CameraResult(null, false, "Gagal membuka kamera: ${e.message}"))
            }
        } else {
            onResult(CameraResult(null, false, "Izin kamera ditolak. Harap izinkan akses kamera di Pengaturan."))
        }
    }

    return remember {
        CameraLauncher {
            try {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    takePictureLauncher.launch(null)
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            } catch (e: Exception) {
                onResult(CameraResult(null, false, "Gagal meluncurkan kamera: ${e.message}"))
            }
        }
    }
}

