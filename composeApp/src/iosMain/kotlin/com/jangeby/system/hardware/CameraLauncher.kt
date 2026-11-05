@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jangeby.system.hardware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import platform.AVFoundation.*
import platform.UIKit.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class CameraLauncher(
    private val onLaunch: () -> Unit
) {
    actual fun launchCamera() {
        onLaunch()
    }
}

@Composable
actual fun rememberCameraLauncher(onResult: (CameraResult) -> Unit): CameraLauncher {
    val currentResult by rememberUpdatedState(onResult)
    
    val delegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                if (image != null) {
                    val imageData = UIImageJPEGRepresentation(image, 0.8)
                    val base64 = imageData?.base64EncodedStringWithOptions(0UL)
                    picker.dismissViewControllerAnimated(true, null)
                    currentResult(CameraResult(base64, base64 != null))
                } else {
                    picker.dismissViewControllerAnimated(true, null)
                    currentResult(CameraResult(null, false, "Foto tidak dapat diambil"))
                }
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                currentResult(CameraResult(null, false, "Pengambilan foto dibatalkan"))
            }
        }
    }

    return remember {
        CameraLauncher {
            fun presentCamera() {
                if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                    currentResult(CameraResult(null, false, "Kamera tidak tersedia pada perangkat ini"))
                    return
                }
                val sceneWindow = UIApplication.sharedApplication.connectedScenes
                    .filterIsInstance<UIWindowScene>()
                    .flatMap { it.windows.filterIsInstance<UIWindow>() }
                    .firstOrNull { it.isKeyWindow() }
                var presenter = (sceneWindow ?: UIApplication.sharedApplication.keyWindow)?.rootViewController
                while (presenter?.presentedViewController != null) {
                    presenter = presenter.presentedViewController
                }
                if (presenter == null || presenter is UIImagePickerController || presenter.isBeingDismissed()) {
                    currentResult(CameraResult(null, false, "Kamera belum dapat dibuka. Silakan coba lagi."))
                    return
                }
                val picker = UIImagePickerController().apply {
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                    modalPresentationStyle = UIModalPresentationFullScreen
                }
                picker.delegate = delegate
                presenter.presentViewController(picker, animated = true, completion = null)
            }
            dispatch_async(dispatch_get_main_queue()) {
                when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                    AVAuthorizationStatusAuthorized -> presentCamera()
                    AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        dispatch_async(dispatch_get_main_queue()) {
                            if (granted) presentCamera()
                            else currentResult(CameraResult(null, false, "Izin kamera ditolak. Aktifkan akses kamera di Pengaturan iPhone."))
                        }
                    }
                    else -> currentResult(CameraResult(null, false, "Izin kamera ditolak. Aktifkan akses kamera di Pengaturan iPhone."))
                }
            }
        }
    }
}
