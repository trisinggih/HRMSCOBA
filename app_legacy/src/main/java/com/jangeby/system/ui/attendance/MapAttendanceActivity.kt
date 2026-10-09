package com.jangeby.system.ui.attendance

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.R
import com.jangeby.system.data.local.SecurePrefs
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.Branch
import com.jangeby.system.data.remote.ClockInRequest
import com.jangeby.system.data.remote.ClockOutRequest
import com.jangeby.system.ui.UiHelpers
import com.jangeby.system.ui.camera.CameraActivity

import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.views.overlay.Polygon

class MapAttendanceActivity : AppCompatActivity() {

    private lateinit var progress: View
    private lateinit var mapView: MapView
    private lateinit var tvDistance: TextView
    private lateinit var btnProceed: Button
    private lateinit var tvActionTitle: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    
    private var branchData: Branch? = null
    private var currentMarker: Marker? = null
    private var radiusPolygon: Polygon? = null
    
    private var currentLoc: Location? = null
    private var actionType: String = "clock_in" // "clock_in" or "clock_out"

    private val overtimePhotos = mutableListOf<String>()
    private var overtimePhotoContainer: android.widget.LinearLayout? = null
    private var overtimeBottomSheet: com.google.android.material.bottomsheet.BottomSheetDialog? = null
    private var overtimeFaceBase64: String? = null

    private val addPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                var faceBase64 = result.data?.getStringExtra(CameraActivity.RESULT_FACE)
                if (faceBase64 == "file") {
                    try {
                        val file = java.io.File(cacheDir, "temp_face.txt")
                        if (file.exists()) {
                            faceBase64 = file.readText()
                            file.delete()
                        }
                    } catch (e: Exception) {
                        faceBase64 = null
                    }
                }
                if (faceBase64 != null) {
                    val cleanBase64 = if (faceBase64.contains(",")) faceBase64.substringAfter(",") else faceBase64
                    overtimePhotos.add(cleanBase64)
                    refreshOvertimePhotos()
                }
            }
        }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.forEach { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    overtimePhotos.add(base64)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        refreshOvertimePhotos()
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) {
                loadBranchDataAndStartLocation()
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk absensi", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                var faceBase64 = result.data?.getStringExtra(CameraActivity.RESULT_FACE)
                if (faceBase64 == "file") {
                    try {
                        val file = java.io.File(cacheDir, "temp_face.txt")
                        if (file.exists()) {
                            faceBase64 = file.readText()
                            file.delete()
                        }
                    } catch (e: Exception) {
                        faceBase64 = null
                    }
                }
                
                if (faceBase64 != null && currentLoc != null) {

                    if (actionType == "clock_in") {
                        doClockIn(currentLoc!!, faceBase64)
                    } else if (actionType == "clock_out") {
                        doClockOut(currentLoc!!, faceBase64)
                    } else if (actionType == "overtime_clock_in") {
                        doOvertimeClockIn(currentLoc!!, faceBase64)
                    } else if (actionType == "overtime_clock_out") {
                        showOvertimeReportSheet(faceBase64)
                    }
                } else {
                    showMessage("Gagal mendapatkan foto wajah.")
                }
            } else {
                showMessage("Absensi dibatalkan")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_map_attendance)

        actionType = intent.getStringExtra("action") ?: "clock_in"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        setupViews()
        
        if (!LocationHelper.hasLocationPermission(this)) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            loadBranchDataAndStartLocation()
        }
    }

    private fun setupViews() {
        progress = findViewById(R.id.progressBar)
        mapView = findViewById(R.id.mapView)
        tvDistance = findViewById(R.id.tvDistance)
        btnProceed = findViewById(R.id.btnProceed)
        tvActionTitle = findViewById(R.id.tvActionTitle)

        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnRefresh?.setOnClickListener {
            tvDistance.text = "Memperbarui lokasi..."
            tvDistance.setTextColor(Color.GRAY)
            startLocationUpdates()
        }

        if (actionType == "clock_in") {
            tvActionTitle.text = "Lokasi Clock In"
            btnProceed.text = "Ambil Foto Clock In"
            btnProceed.backgroundTintList = getColorStateList(R.color.primary)
        } else if (actionType == "clock_out") {
            tvActionTitle.text = "Lokasi Clock Out"
            btnProceed.text = "Ambil Foto Clock Out"
            btnProceed.backgroundTintList = getColorStateList(android.R.color.holo_red_light)
        } else if (actionType == "overtime_clock_in") {
            tvActionTitle.text = "Lokasi Mulai Lembur"
            btnProceed.text = "Ambil Foto Lembur"
            btnProceed.backgroundTintList = getColorStateList(android.R.color.holo_orange_dark)
        } else if (actionType == "overtime_clock_out") {
            tvActionTitle.text = "Lokasi Selesai Lembur"
            btnProceed.text = "Ambil Foto Selesai Lembur"
            btnProceed.backgroundTintList = getColorStateList(android.R.color.holo_red_light)
        }

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.0)

        btnProceed.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_VERIFY)
            intent.putExtra("ACTION_TYPE", actionType)
            
            // Kirim extra location strings agar CameraActivity bisa membuat stempel (Watermark)
            currentLoc?.let {
                intent.putExtra("LATITUDE", it.latitude.toString())
                intent.putExtra("LONGITUDE", it.longitude.toString())
            }
            
            cameraLauncher.launch(intent)
        }
    }

    private var allBranchLocations: List<com.jangeby.system.data.remote.BranchLocationData> = emptyList()

    private fun loadBranchDataAndStartLocation() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val prefs = SecurePrefs(this@MapAttendanceActivity)
                val branchId = prefs.employeeBranchId
                
                if (branchId == -1L) {
                    showMessage("Data cabang tidak ditemukan. Silakan login ulang.")
                    finish()
                    return@launch
                }
                
                branchData = ApiClient.getService(this@MapAttendanceActivity).getBranchDetail(branchId).data
                
                try {
                    val permissionRes = ApiClient.getService(this@MapAttendanceActivity).checkRadiusPermission()
                    prefs.canClockInAnywhere = permissionRes.data?.canClockInAnywhere ?: false
                } catch (e: Exception) {
                    // Ignore
                }

                try {
                    val branchesRes = ApiClient.getService(this@MapAttendanceActivity).branchLocations()
                    allBranchLocations = branchesRes.data ?: emptyList()
                } catch (e: Exception) {
                    // Ignore
                }
                
                startLocationUpdates()
            } catch (t: Throwable) {
                showMessage("Gagal memuat data cabang: ${t.localizedMessage}")
                finish()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun startLocationUpdates() {
        val branch = branchData ?: return
        val branchPoint = GeoPoint(branch.latitude ?: 0.0, branch.longitude ?: 0.0)
        
        // Draw primary branch radius
        radiusPolygon = Polygon().apply {
            points = Polygon.pointsAsCircle(branchPoint, branch.radius ?: 100.0)
            fillPaint.color = Color.parseColor("#3300FF00")
            outlinePaint.color = Color.parseColor("#00FF00")
            outlinePaint.strokeWidth = 2f
        }
        mapView.overlays.add(radiusPolygon)

        // Draw other branch radiuses
        for (b in allBranchLocations) {
            if (b.id == branch.id || b.latitude == null || b.longitude == null) continue
            val otherPoint = GeoPoint(b.latitude, b.longitude)
            val otherPolygon = Polygon().apply {
                points = Polygon.pointsAsCircle(otherPoint, b.attendanceRadius ?: 100.0)
                fillPaint.color = Color.parseColor("#330000FF") // Semitransparent blue for other branches
                outlinePaint.color = Color.parseColor("#0000FF")
                outlinePaint.strokeWidth = 2f
            }
            mapView.overlays.add(otherPolygon)
        }
        
        mapView.controller.setCenter(branchPoint)
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (com.jangeby.system.utils.SecurityHelper.isLocationMocked(location)) {
                        val intent = Intent(this@MapAttendanceActivity, com.jangeby.system.ui.BlockedActivity::class.java).apply {
                            putExtra("EXTRA_REASON", "Aplikasi mendeteksi lokasi yang dipalsukan (Fake GPS). Akses diblokir.")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        return
                    }
                    currentLoc = location
                    updateMapAndValidateRadius(location, branch)
                }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateMapAndValidateRadius(location: Location, branch: Branch) {
        val userPoint = GeoPoint(location.latitude, location.longitude)
        
        if (currentMarker == null) {
            currentMarker = Marker(mapView).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Lokasi Anda"
            }
            mapView.overlays.add(currentMarker)
        }
        currentMarker?.position = userPoint
        mapView.controller.animateTo(userPoint)
        mapView.invalidate()
        
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            branch.latitude ?: 0.0, branch.longitude ?: 0.0, results
        )
        
        val distance = results[0]
        val maxRadius = branch.radius ?: 100.0
        
        val prefs = SecurePrefs(this)
        val canClockInAnywhere = prefs.canClockInAnywhere
        
        var inOtherBranch = false
        var otherBranchName = ""
        
        if (distance > maxRadius) {
            for (b in allBranchLocations) {
                if (b.id == branch.id || b.latitude == null || b.longitude == null) continue
                
                val res = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    b.latitude, b.longitude, res
                )
                val d = res[0]
                val r = b.attendanceRadius?.toFloat() ?: 100f
                if (d <= r) {
                    inOtherBranch = true
                    otherBranchName = b.name ?: "Cabang Lain"
                    break
                }
            }
        }
        
        val radiusLimit = maxRadius.toInt()
        if (distance <= maxRadius) {
            tvDistance.text = "Di dalam area kantor (Jarak: ${distance.toInt()}m / Radius Max: ${radiusLimit}m)"
            tvDistance.setTextColor(Color.parseColor("#388E3C"))
            btnProceed.isEnabled = true
        } else if (inOtherBranch) {
            tvDistance.text = "Di $otherBranchName (Jarak: ${distance.toInt()}m / Radius Max: ${radiusLimit}m)"
            tvDistance.setTextColor(Color.parseColor("#388E3C"))
            btnProceed.isEnabled = true
        } else {
            if (canClockInAnywhere) {
                tvDistance.text = "Bebas Presensi (Jarak: ${distance.toInt()}m / Radius Max: ${radiusLimit}m)"
                tvDistance.setTextColor(Color.parseColor("#388E3C"))
                btnProceed.isEnabled = true
            } else {
                tvDistance.text = "Di luar area kantor (Jarak: ${distance.toInt()}m / Radius Max: ${radiusLimit}m)"
                tvDistance.setTextColor(Color.RED)
                btnProceed.isEnabled = false
            }
        }
    }

    private fun doClockIn(loc: Location, faceBase64: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@MapAttendanceActivity).clockIn(
                    ClockInRequest(
                        latitude   = loc.latitude,
                        longitude  = loc.longitude,
                        accuracy   = loc.accuracy.toDouble(),
                        deviceInfo = android.os.Build.MODEL,
                        facePhoto  = faceBase64
                    )
                )
                
                if (res.data?.status == "late") {
                    com.jangeby.system.service.LocationService.startService(this@MapAttendanceActivity)
                    setLoading(false)
                    promptLateReason(res.data)
                } else {
                    com.jangeby.system.service.LocationService.startService(this@MapAttendanceActivity)
                    showMessage(res.message ?: "Clock-in berhasil")
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (t: Throwable) {
                showMessage(UiHelpers.errorMessageFrom(this@MapAttendanceActivity, t))
                setLoading(false)
            }
        }
    }

    private fun promptLateReason(attendanceItem: com.jangeby.system.data.remote.AttendanceItem) {
        val editText = android.widget.EditText(this).apply {
            hint = "Alasan Keterlambatan"
            setPadding(48, 48, 48, 48)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Anda Terlambat")
            .setMessage("Clock-in tercatat terlambat. Silakan masukkan alasan keterlambatan Anda untuk pengajuan (approval).")
            .setView(editText)
            .setCancelable(false)
            .setPositiveButton("Kirim") { _, _ ->
                val reason = editText.text.toString()
                if (reason.isNotBlank()) {
                    submitLateReason(attendanceItem.date ?: "", reason)
                } else {
                    showMessage("Alasan tidak boleh kosong")
                    promptLateReason(attendanceItem)
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                showMessage("Clock-in terlambat (tanpa alasan)")
                setResult(Activity.RESULT_OK)
                finish()
            }
            .show()
    }

    private fun submitLateReason(date: String, reason: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@MapAttendanceActivity).lateRequest(
                    com.jangeby.system.data.remote.LateRequestData(date = date, reason = reason)
                )
                showMessage("Alasan keterlambatan berhasil dikirim")
                setResult(Activity.RESULT_OK)
                finish()
            } catch (t: Throwable) {
                showMessage(UiHelpers.errorMessageFrom(this@MapAttendanceActivity, t))
                setLoading(false)
            }
        }
    }

    
    private fun doOvertimeClockIn(location: Location, facePhoto: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                // Actually the current overtime APIs don't take location and photo.
                // In MobileOvertimeController.php, clockIn() takes no parameters except user token.
                // But let's call it anyway.
                val res = ApiClient.getService(this@MapAttendanceActivity).overtimeClockIn()
                if (res.success) {
                    showMessage("Berhasil clock in lembur")
                    finish()
                } else {
                    showMessage(res.message ?: "Gagal clock in lembur")
                }
            } catch (e: Exception) {
                showMessage("Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showOvertimeReportSheet(faceBase64: String) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_overtime_report, null)
        dialog.setContentView(view)
        
        val etReport = view.findViewById<android.widget.EditText>(R.id.etReport)
        val btnAddPhoto = view.findViewById<View>(R.id.btnAddPhoto)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitReport)
        overtimePhotoContainer = view.findViewById(R.id.llPhotos)
        
        overtimePhotos.clear()
        val cleanBase64 = if (faceBase64.contains(",")) faceBase64.substringAfter(",") else faceBase64
        overtimeFaceBase64 = cleanBase64
        refreshOvertimePhotos()
        
        btnAddPhoto.setOnClickListener {
            val options = arrayOf("Kamera", "Galeri")
            android.app.AlertDialog.Builder(this)
                .setTitle("Pilih Sumber Foto")
                .setItems(options) { _, which ->
                    if (which == 0) {
                        val intent = Intent(this, CameraActivity::class.java)
                        intent.putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_VERIFY)
                        addPhotoLauncher.launch(intent)
                    } else {
                        galleryLauncher.launch("image/*")
                    }
                }
                .show()
        }
        
        btnSubmit.setOnClickListener {
            val report = etReport.text.toString().trim()
            if (report.isEmpty()) {
                showMessage("Laporan tidak boleh kosong")
                return@setOnClickListener
            }
            if (overtimePhotos.isEmpty()) {
                showMessage("Wajib melampirkan minimal 1 foto")
                return@setOnClickListener
            }
            dialog.dismiss()
            submitOvertimeClockOut(report, overtimePhotos)
        }
        
        overtimeBottomSheet = dialog
        dialog.show()
    }

    private fun refreshOvertimePhotos() {
        val container = overtimePhotoContainer ?: return
        if (container.childCount > 1) {
            container.removeViews(1, container.childCount - 1)
        }
        
        for (i in overtimePhotos.indices) {
            val base64 = overtimePhotos[i]
            val cleanBase64 = if (base64.contains(",")) base64.substringAfter(",") else base64
            val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    (80 * resources.displayMetrics.density).toInt(),
                    (80 * resources.displayMetrics.density).toInt()
                ).apply {
                    setMargins(0, 0, (8 * resources.displayMetrics.density).toInt(), 0)
                }
                radius = 8 * resources.displayMetrics.density
            }
            
            val iv = android.widget.ImageView(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bitmap)
            }
            
            card.setOnClickListener {
                android.app.AlertDialog.Builder(this@MapAttendanceActivity)
                    .setTitle("Hapus Foto?")
                    .setPositiveButton("Ya") { _, _ ->
                        overtimePhotos.removeAt(i)
                        refreshOvertimePhotos()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
            
            card.addView(iv)
            container.addView(card)
        }
    }

    private fun submitOvertimeClockOut(reportText: String, photos: List<String>) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val facePhoto = overtimeFaceBase64 ?: return@launch
                
                val textBody = reportText.toRequestBody("text/plain".toMediaTypeOrNull())
                val faceBody = facePhoto.toRequestBody("text/plain".toMediaTypeOrNull())
                
                val parts = mutableListOf<okhttp3.MultipartBody.Part>()
                for ((index, base64) in photos.withIndex()) {
                    val cleanBase64 = if (base64.contains(",")) base64.substringAfter(",") else base64
                    val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                    val reqFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, bytes.size)
                    parts.add(okhttp3.MultipartBody.Part.createFormData("report_photos[]", "report_$index.jpg", reqFile))
                }

                val res = ApiClient.getService(this@MapAttendanceActivity).overtimeClockOut(textBody, faceBody, parts)
                if (res.success) {
                    showMessage("Berhasil clock out lembur")
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    showMessage(res.message ?: "Gagal clock out lembur")
                }
            } catch (e: Exception) {
                showMessage("Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun doClockOut(loc: Location, faceBase64: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@MapAttendanceActivity).clockOut(
                    ClockOutRequest(
                        latitude   = loc.latitude,
                        longitude  = loc.longitude,
                        accuracy   = loc.accuracy.toDouble(),
                        deviceInfo = android.os.Build.MODEL,
                        facePhoto  = faceBase64
                    )
                )
                
                com.jangeby.system.service.LocationService.stopService(this@MapAttendanceActivity)
                
                showMessage(res.message ?: "Clock-out berhasil")
                setResult(Activity.RESULT_OK)
                finish()
            } catch (t: Throwable) {
                showMessage(UiHelpers.errorMessageFrom(this@MapAttendanceActivity, t))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progress.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnProceed.isEnabled = !isLoading && btnProceed.isEnabled
    }

    private fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("actionType", actionType)
        currentLoc?.let {
            outState.putParcelable("currentLoc", it)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getString("actionType")?.let { actionType = it }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            savedInstanceState.getParcelable("currentLoc", Location::class.java)?.let { currentLoc = it }
        } else {
            @Suppress("DEPRECATION")
            savedInstanceState.getParcelable<Location>("currentLoc")?.let { currentLoc = it }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}
