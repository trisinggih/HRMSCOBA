package com.jangeby.system.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.RegisterFaceRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * CameraActivity — digunakan untuk dua mode:
 *  - MODE_REGISTER : capture wajah → upload ke backend sebagai referensi
 *  - MODE_VERIFY   : capture wajah → kembalikan base64 ke activity pemanggil untuk dikirim bersama clock-in/out
 */
class CameraActivity : AppCompatActivity() {

    companion object {
        const val MODE_REGISTER = "REGISTER"
        const val MODE_VERIFY   = "VERIFY"
        const val EXTRA_MODE    = "mode"
        const val RESULT_FACE   = "face_base64"
    }

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var tvInstruction: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var tvWatermarkTime: TextView
    private lateinit var tvWatermarkLoc: TextView
    private val timeHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var timeRunnable: java.lang.Runnable

    private var imageCapture: ImageCapture? = null

    private val mode by lazy { intent.getStringExtra(EXTRA_MODE) ?: MODE_VERIFY }
    private val actionType by lazy { intent.getStringExtra("ACTION_TYPE") ?: "clock_in" }
    private val latitude by lazy { intent.getStringExtra("LATITUDE") ?: "-" }
    private val longitude by lazy { intent.getStringExtra("LONGITUDE") ?: "-" }

    private var faceDetector: com.google.mlkit.vision.face.FaceDetector? = null

    private fun getFaceDetector(): com.google.mlkit.vision.face.FaceDetector {
        return faceDetector ?: try {
            FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setMinFaceSize(0.15f)
                    .build()
            ).also { faceDetector = it }
        } catch (e: Exception) {
            // Fallback ke detector default
            FaceDetection.getClient().also { faceDetector = it }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_face_camera)

            previewView   = findViewById(R.id.previewView)
            btnCapture    = findViewById(R.id.btnCapture)
            tvInstruction = findViewById(R.id.tvInstruction)
            tvStatus      = findViewById(R.id.tvStatus)
            progressBar   = findViewById(R.id.progressBar)
            tvWatermarkTime = findViewById(R.id.tvWatermarkTime)
            tvWatermarkLoc  = findViewById(R.id.tvWatermarkLoc)

            tvWatermarkLoc.text = "Lat: $latitude, Long: $longitude"

            timeRunnable = object : java.lang.Runnable {
                override fun run() {
                    if (!isDestroyed && !isFinishing) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        tvWatermarkTime.text = sdf.format(java.util.Date())
                        timeHandler.postDelayed(this, 1000)
                    }
                }
            }
            timeHandler.post(timeRunnable)

            when (mode) {
                MODE_REGISTER -> {
                    tvInstruction.text = "Posisikan wajah Anda, lalu tekan Daftar untuk mendaftar wajah"
                    btnCapture.text = "Daftar Wajah Sekarang"
                }
                else -> {
                    tvInstruction.text = "Posisikan wajah Anda, lalu tekan tombol untuk absensi"
                    if (actionType == "clock_in") {
                        btnCapture.text = "Clock In Sekarang"
                    } else {
                        btnCapture.text = "Clock Out Sekarang"
                        btnCapture.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E53935"))
                    }
                }
            }

            btnCapture.isEnabled = false
            btnCapture.alpha = 0.5f

            btnCapture.setOnClickListener { takePhoto() }
            findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { timeHandler.removeCallbacks(timeRunnable) } catch (_: Exception) {}
        try { faceDetector?.close() } catch (_: Exception) {}
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    getFaceDetector().process(image)
                        .addOnSuccessListener { faces ->
                            if (faces.size == 1) {
                                btnCapture.isEnabled = true
                                btnCapture.alpha = 1.0f
                                showStatus("✅ Wajah terdeteksi, silakan tap tombol", isError = false)
                            } else {
                                btnCapture.isEnabled = false
                                btnCapture.alpha = 0.5f
                                if (faces.isEmpty()) {
                                    showStatus("Arahkan wajah Anda ke kamera", isError = true)
                                } else {
                                    showStatus("Pastikan hanya ada 1 wajah", isError = true)
                                }
                            }
                        }
                        .addOnFailureListener {
                            btnCapture.isEnabled = false
                            btnCapture.alpha = 0.5f
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture, imageAnalysis)
            } catch (e: Exception) {
                showStatus("Gagal membuka kamera: ${e.message}", isError = true)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        setLoading(true)
        showStatus("Mendeteksi wajah...", isError = false)

        capture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(proxy: ImageProxy) {
                val bitmap = proxy.toBitmap()
                proxy.close()
                processFaceAndProceed(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                setLoading(false)
                showStatus("Gagal capture: ${exception.message}", isError = true)
            }
        })
    }

    private fun processFaceAndProceed(rawBitmap: Bitmap?) {
        if (rawBitmap == null) {
            setLoading(false)
            showStatus("Gagal memproses foto. Silakan coba lagi.", isError = true)
            return
        }

        try {
            // Mirror the front camera bitmap
            val matrix = Matrix().apply { preScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f) }
            val mirrored = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, false)

            getFaceDetector().process(InputImage.fromBitmap(mirrored, 0))
                .addOnSuccessListener { faces ->
                    try {
                        var finalBitmap = mirrored
                        if (faces.isNotEmpty()) {
                            val face = faces.first()
                            val bounds = face.boundingBox
                            val margin = (bounds.width() * 0.3).toInt()
                            val left   = maxOf(0, bounds.left - margin)
                            val top    = maxOf(0, bounds.top - margin)
                            val right  = minOf(mirrored.width, bounds.right + margin)
                            val bottom = minOf(mirrored.height, bounds.bottom + margin)
                            val width  = right - left
                            val height = bottom - top

                            if (width > 50 && height > 50 && left + width <= mirrored.width && top + height <= mirrored.height) {
                                finalBitmap = Bitmap.createBitmap(mirrored, left, top, width, height)
                            }
                        }

                        if (mode == MODE_VERIFY) {
                            finalBitmap = addWatermark(finalBitmap)
                        }

                        // Resize bitmap agar base64 kecil (~400px) & aman dari TransactionTooLargeException
                        val maxDim = 400f
                        val scale = Math.min(maxDim / finalBitmap.width, maxDim / finalBitmap.height)
                        if (scale < 1f) {
                            val newW = maxOf(1, (finalBitmap.width * scale).toInt())
                            val newH = maxOf(1, (finalBitmap.height * scale).toInt())
                            finalBitmap = Bitmap.createScaledBitmap(finalBitmap, newW, newH, true)
                        }

                        // Encode ke JPEG base64
                        val outputStream = ByteArrayOutputStream()
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        val dataUri = "data:image/jpeg;base64,$base64"

                        when (mode) {
                            MODE_REGISTER -> uploadFace(dataUri)
                            else          -> returnFaceResult(dataUri)
                        }
                    } catch (e: Exception) {
                        setLoading(false)
                        showStatus("Gagal memproses foto: ${e.message}", isError = true)
                    }
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    showStatus("Error deteksi wajah: ${e.message}", isError = true)
                }
        } catch (e: Exception) {
            setLoading(false)
            showStatus("Gagal memproses gambar: ${e.message}", isError = true)
        }
    }

    /** MODE_REGISTER: upload ke backend */
    private fun uploadFace(base64: String) {
        showStatus("Mendaftarkan wajah ke server...", isError = false)
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@CameraActivity)
                    .registerFace(RegisterFaceRequest(facePhoto = base64))
                if (res.success) {
                    showStatus("✅ Wajah berhasil didaftarkan!", isError = false)
                    Toast.makeText(this@CameraActivity, "Wajah berhasil didaftarkan!", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    showStatus("❌ ${res.message}", isError = true)
                }
            } catch (t: Throwable) {
                showStatus("❌ Gagal: ${t.localizedMessage}", isError = true)
            } finally {
                setLoading(false)
            }
        }
    }

    /** MODE_VERIFY: kembalikan base64 ke AttendanceActivity */
    private fun returnFaceResult(base64: String) {
        setLoading(false)
        showStatus("✅ Wajah terdeteksi!", isError = false)
        try {
            val file = java.io.File(cacheDir, "temp_face.txt")
            file.writeText(base64)
            val resultIntent = Intent()
            resultIntent.putExtra(RESULT_FACE, "file")
            setResult(Activity.RESULT_OK, resultIntent)
        } catch (e: Exception) {
            val resultIntent = Intent()
            resultIntent.putExtra(RESULT_FACE, base64)
            setResult(Activity.RESULT_OK, resultIntent)
        }
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnCapture.isEnabled = !loading
        btnCapture.alpha = if (loading) 0.4f else 1.0f
    }

    private fun showStatus(message: String, isError: Boolean) {
        tvStatus.text = message
        tvStatus.setTextColor(
            if (isError) getColor(android.R.color.holo_red_light)
            else         getColor(android.R.color.holo_green_dark)
        )
        tvStatus.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun addWatermark(src: Bitmap): Bitmap {
        return try {
            val lat = intent.getStringExtra("LATITUDE") ?: "-"
            val lon = intent.getStringExtra("LONGITUDE") ?: "-"
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss", java.util.Locale.getDefault())
            val time = sdf.format(java.util.Date())

            val result = src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
            val canvas = Canvas(result)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = maxOf(16f, result.width * 0.04f)
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            val padding = result.width * 0.05f
            var y = result.height - padding - (paint.textSize * 2)

            canvas.drawText("Lat: $lat, Lon: $lon", padding, y, paint)
            y += paint.textSize * 1.2f
            canvas.drawText("Waktu: $time", padding, y, paint)

            result
        } catch (e: Exception) {
            src
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        return try {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp == null && format == ImageFormat.YUV_420_888) {
                val yBuffer = planes[0].buffer
                val uBuffer = planes[1].buffer
                val vBuffer = planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 80, out)
                val imageBytes = out.toByteArray()
                bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
