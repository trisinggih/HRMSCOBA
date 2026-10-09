package com.jangeby.system.ui.checkpoint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.widget.EditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class AddCheckPointActivity : AppCompatActivity() {

    private lateinit var etClientName: EditText
    private lateinit var etPurpose: EditText
    private lateinit var etNotes: EditText
    private lateinit var tvLocation: TextView
    private lateinit var rvPhotos: androidx.recyclerview.widget.RecyclerView
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var btnSubmit: MaterialButton
    private lateinit var layoutLoading: FrameLayout

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val photoFiles = mutableListOf<File>()
    private lateinit var photoAdapter: PhotoPreviewAdapter

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                fetchLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchLocation()
            }
            else -> {
                tvLocation.text = "Izin lokasi ditolak"
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap?
            if (imageBitmap != null) {
                // Save to file
                val file = File(cacheDir, "checkpoint_photo_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(file)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()
                
                photoFiles.add(file)
                photoAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_check_point)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        etClientName = findViewById(R.id.etClientName)
        etPurpose = findViewById(R.id.etPurpose)
        etNotes = findViewById(R.id.etNotes)
        tvLocation = findViewById(R.id.tvLocation)
        rvPhotos = findViewById(R.id.rvPhotos)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        btnSubmit = findViewById(R.id.btnSubmit)
        layoutLoading = findViewById(R.id.layoutLoading)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        photoAdapter = PhotoPreviewAdapter(photoFiles) { position ->
            photoFiles.removeAt(position)
            photoAdapter.notifyDataSetChanged()
        }
        rvPhotos.adapter = photoAdapter

        btnAddPhoto.setOnClickListener {
            // Live camera only as per implementation plan rules
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(intent)
        }

        btnSubmit.setOnClickListener {
            submitCheckPoint()
        }

        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                    tvLocation.text = "${location.latitude}, ${location.longitude}"
                } else {
                    tvLocation.text = "Lokasi tidak ditemukan, coba lagi"
                }
            }
        }
    }

    private fun submitCheckPoint() {
        val clientName = etClientName.text.toString().trim()
        val purpose = etPurpose.text.toString().trim()
        val notes = etNotes.text.toString().trim()

        if (clientName.isEmpty() || purpose.isEmpty()) {
            Toast.makeText(this, "Nama Klien dan Tujuan harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentLocation == null) {
            Toast.makeText(this, "Lokasi belum didapatkan. Mohon tunggu...", Toast.LENGTH_SHORT).show()
            fetchLocation()
            return
        }

        if (photoFiles.isEmpty()) {
            Toast.makeText(this, "Mohon ambil minimal 1 foto bukti kunjungan", Toast.LENGTH_SHORT).show()
            return
        }

        layoutLoading.visibility = View.VISIBLE
        btnSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                val reqClientName = clientName.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqPurpose = purpose.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqLat = currentLocation!!.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val reqLng = currentLocation!!.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val reqNotes = notes.toRequestBody("text/plain".toMediaTypeOrNull())
                
                val parts = photoFiles.map { file ->
                    val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("photos[]", file.name, reqFile)
                }

                val response = ApiClient.getService(this@AddCheckPointActivity).submitCheckPoint(
                    clientName = reqClientName,
                    purpose = reqPurpose,
                    latitude = reqLat,
                    longitude = reqLng,
                    address = null,
                    notes = reqNotes,
                    photos = parts
                )

                if (response.success) {
                    Toast.makeText(this@AddCheckPointActivity, "Berhasil disubmit", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddCheckPointActivity, response.message ?: "Gagal submit", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddCheckPointActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                layoutLoading.visibility = View.GONE
                btnSubmit.isEnabled = true
            }
        }
    }
}
