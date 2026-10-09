package com.jangeby.system.ui.permit

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormPermitActivity : AppCompatActivity() {

    private lateinit var spinnerType: Spinner
    private lateinit var tvDate: TextView
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var etReason: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnAttach: LinearLayout
    private lateinit var ivPreview: ImageView

    private var selectedDate = ""
    private var selectedStartTime = ""
    private var selectedEndTime = ""
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                if (selectedImageUri != null) {
                    ivPreview.visibility = View.VISIBLE
                    Glide.with(this).load(selectedImageUri).into(ivPreview)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_permit)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        spinnerType = findViewById(R.id.spinnerType)
        tvDate = findViewById(R.id.tvDate)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvEndTime = findViewById(R.id.tvEndTime)
        etReason = findViewById(R.id.etReason)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnAttach = findViewById(R.id.btnAttach)
        ivPreview = findViewById(R.id.ivPreview)

        val typeOptions = arrayOf("Keperluan Pribadi", "Dinas Kantor")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeOptions)
        spinnerType.adapter = adapter

        spinnerType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position == 0) { // Pribadi
                    findViewById<TextView>(R.id.tvLabelStartTime).text = "Jam Keluar"
                    findViewById<TextView>(R.id.tvLabelEndTime).text = "Jam Kembali"
                } else { // Dinas Kantor
                    findViewById<TextView>(R.id.tvLabelStartTime).text = "Jam Mulai"
                    findViewById<TextView>(R.id.tvLabelEndTime).text = "Jam Selesai"
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val m = String.format("%02d", month + 1)
                    val d = String.format("%02d", dayOfMonth)
                    selectedDate = "$year-$m-$d"
                    
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                    cal.set(year, month, dayOfMonth)
                    tvDate.text = sdf.format(cal.time)
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        tvStartTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    val h = String.format("%02d", hourOfDay)
                    val m = String.format("%02d", minute)
                    selectedStartTime = "$h:$m"
                    tvStartTime.text = selectedStartTime
                },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
            ).show()
        }

        tvEndTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    val h = String.format("%02d", hourOfDay)
                    val m = String.format("%02d", minute)
                    selectedEndTime = "$h:$m"
                    tvEndTime.text = selectedEndTime
                },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
            ).show()
        }

        btnAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        btnSubmit.setOnClickListener {
            submitForm()
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "permit_attachment_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun submitForm() {
        val typeIdx = spinnerType.selectedItemPosition
        val type = if (typeIdx == 0) "personal" else "business"
        val reason = etReason.text.toString().trim()

        if (selectedDate.isEmpty() || selectedStartTime.isEmpty() || selectedEndTime.isEmpty() || reason.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua isian", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        btnSubmit.text = "Mengirim..."

        val typePart = type.toRequestBody("text/plain".toMediaTypeOrNull())
        val datePart = selectedDate.toRequestBody("text/plain".toMediaTypeOrNull())
        val startPart = selectedStartTime.toRequestBody("text/plain".toMediaTypeOrNull())
        val endPart = selectedEndTime.toRequestBody("text/plain".toMediaTypeOrNull())
        val reasonPart = reason.toRequestBody("text/plain".toMediaTypeOrNull())

        var attachmentPart: MultipartBody.Part? = null
        if (selectedImageUri != null) {
            val file = getFileFromUri(selectedImageUri!!)
            if (file != null) {
                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                attachmentPart = MultipartBody.Part.createFormData("attachment", file.name, reqFile)
            }
        }

        val apiService = ApiClient.getService(this)
        lifecycleScope.launch {
            try {
                val response = apiService.createPermitRequest(
                    typePart, datePart, startPart, endPart, reasonPart, attachmentPart
                )
                if (response.success) {
                    Toast.makeText(this@FormPermitActivity, "Izin berhasil diajukan", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Pengajuan"
                    Toast.makeText(this@FormPermitActivity, response.message ?: "Gagal mengirim pengajuan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Kirim Pengajuan"
                Toast.makeText(this@FormPermitActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
