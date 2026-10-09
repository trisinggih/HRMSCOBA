package com.jangeby.system.hardware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.*
import platform.Foundation.*
import platform.darwin.NSObject

actual class CameraLauncher(
    private val onLaunch: () -> Unit
) {
    actual fun launchCamera() {
        onLaunch()
    }
}

@Composable
actual fun rememberCameraLauncher(onResult: (CameraResult) -> Unit): CameraLauncher {
    
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
                    onResult(CameraResult(base64, true))
                } else {
                    picker.dismissViewControllerAnimated(true, null)
                    onResult(CameraResult(null, false, "Failed to get image"))
                }
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                onResult(CameraResult(null, false, "Camera cancelled"))
            }
        }
    }

    return remember {
        CameraLauncher {
            val imagePickerController = UIImagePickerController()
            if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                imagePickerController.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            }
            imagePickerController.delegate = delegate
            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                imagePickerController, animated = true, completion = null
            )
        }
    }
}
