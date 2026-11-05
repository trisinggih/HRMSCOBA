@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jangeby.system.hardware

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlin.concurrent.Volatile
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSError
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

@Composable
actual fun CameraPreviewWithFaceDetection(
    modifier: Modifier,
    onFaceStatusChanged: (Boolean) -> Unit,
    onPhotoCaptured: (String?) -> Unit,
    captureTrigger: Long
) {
    val faceCallback by rememberUpdatedState(onFaceStatusChanged)
    val photoCallback by rememberUpdatedState(onPhotoCaptured)
    val uriHandler = LocalUriHandler.current
    val camera = remember {
        IosFaceCamera(onFace = { faceCallback(it) }, onPhoto = { photoCallback(it) })
    }
    // A newly mounted view must not replay an old capture request.
    var lastCaptureTrigger by remember { mutableStateOf(captureTrigger) }

    DisposableEffect(camera) {
        val center = NSNotificationCenter.defaultCenter
        val inactive = center.addObserverForName(
            UIApplicationWillResignActiveNotification, null, NSOperationQueue.mainQueue
        ) { camera.pause() }
        val active = center.addObserverForName(
            UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue
        ) { camera.resume() }
        val interrupted = center.addObserverForName(
            AVCaptureSessionWasInterruptedNotification, camera.session, NSOperationQueue.mainQueue
        ) { camera.interrupted() }
        val interruptionEnded = center.addObserverForName(
            AVCaptureSessionInterruptionEndedNotification, camera.session, NSOperationQueue.mainQueue
        ) { camera.resume() }
        val runtimeError = center.addObserverForName(
            AVCaptureSessionRuntimeErrorNotification, camera.session, NSOperationQueue.mainQueue
        ) { camera.failed() }
        camera.resume()
        onDispose {
            listOf(inactive, active, interrupted, interruptionEnded, runtimeError).forEach {
                center.removeObserver(it)
            }
            camera.close()
        }
    }

    LaunchedEffect(captureTrigger) {
        if (captureTrigger > lastCaptureTrigger) camera.capture()
        lastCaptureTrigger = captureTrigger
    }

    LaunchedEffect(camera) {
        while (true) {
            delay(250)
            camera.expireFace()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(isInteractive = false, isNativeAccessibilityEnabled = false),
            factory = {
                object : UIView(frame = CGRectZero.readValue()) {
                    private val preview = AVCaptureVideoPreviewLayer(session = camera.session).apply {
                        videoGravity = AVLayerVideoGravityResizeAspectFill
                    }

                    init {
                        backgroundColor = UIColor.blackColor
                        layer.addSublayer(preview)
                    }

                    override fun layoutSubviews() {
                        super.layoutSubviews()
                        CATransaction.begin()
                        CATransaction.setValue(true, kCATransactionDisableActions)
                        // Local bounds follow Compose layout changes correctly.
                        preview.setFrame(bounds)
                        preview.connection?.let { connection ->
                            if (connection.isVideoOrientationSupported()) {
                                connection.videoOrientation = AVCaptureVideoOrientationPortrait
                            }
                            if (connection.isVideoMirroringSupported()) {
                                connection.automaticallyAdjustsVideoMirroring = false
                                connection.videoMirrored = true
                            }
                        }
                        CATransaction.commit()
                    }
                }
            },
            update = { view ->
                if (camera.ready) {
                    view.setNeedsLayout()
                    view.layoutIfNeeded()
                }
            }
        )
        camera.errorMessage?.let { message ->
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(message, color = Color.White, textAlign = TextAlign.Center)
                if (camera.permissionDenied) {
                    TextButton(onClick = { uriHandler.openUri(UIApplicationOpenSettingsURLString) }) {
                        Text("Buka Pengaturan", color = Color.White)
                    }
                } else {
                    TextButton(onClick = { camera.resume() }) { Text("Coba lagi", color = Color.White) }
                }
            }
        }
    }
}

/** Keeps native capture delegates alive while the camera screen is visible. */
private class IosFaceCamera(
    private val onFace: (Boolean) -> Unit,
    private val onPhoto: (String?) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol, AVCapturePhotoCaptureDelegateProtocol {
    val session = AVCaptureSession()
    private val sessionQueue = dispatch_queue_create("com.jangeby.system.camera", null)
    private val photoOutput = AVCapturePhotoOutput()
    private val faceOutput = AVCaptureMetadataOutput()
    private var configured = false
    @Volatile private var disposed = false
    @Volatile private var active = false
    private var faceVisible = false
    private var capturing = false
    private var pendingPhotoId: Long? = null
    private var lastFaceAt = 0.0
    var ready by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var permissionDenied by mutableStateOf(false)
        private set

    fun resume() {
        if (disposed || UIApplication.sharedApplication.applicationState != UIApplicationStateActive) return
        active = true
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> {
                permissionDenied = false
                errorMessage = null
                dispatch_async(sessionQueue) {
                    if (disposed || !active) return@dispatch_async
                    if (!configured && !configure()) return@dispatch_async
                    if (!session.running) session.startRunning()
                    dispatch_async(dispatch_get_main_queue()) {
                        if (!disposed && active) ready = session.running
                    }
                }
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) {
                    dispatch_async(dispatch_get_main_queue()) { if (!disposed) resume() }
                }
            }
            else -> {
                permissionDenied = true
                showError("Izin kamera belum diberikan. Aktifkan akses kamera di Pengaturan iPhone.")
            }
        }
    }

    /** Configure and start/stop the capture session off the main thread. */
    private fun configure(): Boolean {
        val device = AVCaptureDevice.defaultDeviceWithDeviceType(
            AVCaptureDeviceTypeBuiltInWideAngleCamera, AVMediaTypeVideo, AVCaptureDevicePositionFront
        ) ?: run {
            showError("Kamera depan tidak tersedia. Gunakan iPhone atau iPad fisik, bukan simulator.")
            return false
        }
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: run {
            showError("Kamera tidak dapat dibuka. Tutup aplikasi lain yang memakai kamera dan coba lagi.")
            return false
        }
        session.beginConfiguration()
        var success = false
        try {
            if (session.canSetSessionPreset(AVCaptureSessionPresetPhoto)) {
                session.sessionPreset = AVCaptureSessionPresetPhoto
            }
            if (!session.canAddInput(input)) return false
            session.addInput(input)
            if (!session.canAddOutput(photoOutput)) return false
            session.addOutput(photoOutput)
            if (!session.canAddOutput(faceOutput)) return false
            session.addOutput(faceOutput)
            if (!faceOutput.availableMetadataObjectTypes.contains(AVMetadataObjectTypeFace)) return false
            faceOutput.setMetadataObjectsDelegate(this, dispatch_get_main_queue())
            faceOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeFace)
            configured = true
            success = true
            return true
        } finally {
            if (!success) {
                faceOutput.setMetadataObjectsDelegate(null, null)
                session.outputs.toList().filterIsInstance<AVCaptureOutput>().forEach { session.removeOutput(it) }
                session.inputs.toList().filterIsInstance<AVCaptureInput>().forEach { session.removeInput(it) }
                showError("Preview dan deteksi wajah tidak dapat diaktifkan pada kamera ini. Silakan coba lagi.")
            }
            session.commitConfiguration()
        }
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (disposed || !active || errorMessage != null) return
        faceVisible = didOutputMetadataObjects.filterIsInstance<AVMetadataFaceObject>().size == 1
        lastFaceAt = NSDate().timeIntervalSince1970
        onFace(faceVisible)
    }

    fun expireFace() {
        if (faceVisible && NSDate().timeIntervalSince1970 - lastFaceAt > 1.0) {
            faceVisible = false
            onFace(false)
        }
    }

    fun capture() {
        if (disposed) return
        expireFace()
        if (!active || !faceVisible || !session.running || capturing) {
            onPhoto(null)
            return
        }
        capturing = true
        val settings = AVCapturePhotoSettings.photoSettingsWithFormat(mapOf(AVVideoCodecKey to AVVideoCodecTypeJPEG))
        pendingPhotoId = settings.uniqueID
        dispatch_async(sessionQueue) {
            if (disposed || !active || !session.running) {
                finishPhoto(null, settings.uniqueID)
                return@dispatch_async
            }
            photoOutput.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
                if (connection.isVideoOrientationSupported()) connection.videoOrientation = AVCaptureVideoOrientationPortrait
                if (connection.isVideoMirroringSupported()) {
                    connection.automaticallyAdjustsVideoMirroring = false
                    connection.videoMirrored = true
                }
            }
            photoOutput.capturePhotoWithSettings(
                settings, this
            )
        }
    }

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        val photoId = didFinishProcessingPhoto.resolvedSettings.uniqueID
        if (error != null) {
            finishPhoto(null, photoId)
            return
        }
        val data = didFinishProcessingPhoto.fileDataRepresentation()
        val image = data?.let { UIImage(it) }
        if (image == null) {
            finishPhoto(null, photoId)
            return
        }
        // Normalize orientation and bound the JPEG payload like Android (400 px).
        val width = image.size.useContents { width }
        val height = image.size.useContents { height }
        if (width <= 0.0 || height <= 0.0) {
            finishPhoto(null, photoId)
            return
        }
        val scale = minOf(1.0, 400.0 / maxOf(width, height))
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(width * scale, height * scale), false, 1.0)
        val jpeg = try {
            image.drawInRect(platform.CoreGraphics.CGRectMake(0.0, 0.0, width * scale, height * scale))
            UIGraphicsGetImageFromCurrentImageContext()?.let { UIImageJPEGRepresentation(it, 0.8) }
        } finally {
            UIGraphicsEndImageContext()
        }
        finishPhoto(jpeg?.base64EncodedStringWithOptions(0UL)?.let { "data:image/jpeg;base64,$it" }, photoId)
    }

    private fun finishPhoto(photo: String?, photoId: Long? = pendingPhotoId) {
        dispatch_async(dispatch_get_main_queue()) {
            if (!capturing || pendingPhotoId != photoId) return@dispatch_async
            capturing = false
            pendingPhotoId = null
            if (!disposed) onPhoto(photo)
        }
    }

    private fun showError(message: String) {
        dispatch_async(dispatch_get_main_queue()) {
            if (!disposed) {
                errorMessage = message
                faceVisible = false
                onFace(false)
                if (capturing) finishPhoto(null)
            }
        }
    }

    fun interrupted() {
        pause()
        showError("Kamera sedang tidak tersedia. Kembali ke aplikasi untuk mencoba lagi.")
    }

    fun failed() {
        pause()
        showError("Sesi kamera terhenti. Silakan coba lagi.")
    }

    fun pause() {
        active = false
        ready = false
        faceVisible = false
        onFace(false)
        if (capturing) finishPhoto(null)
        dispatch_async(sessionQueue) { if (session.running) session.stopRunning() }
    }

    fun close() {
        disposed = true
        pause()
        dispatch_async(sessionQueue) { faceOutput.setMetadataObjectsDelegate(null, null) }
    }
}
