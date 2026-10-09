package com.jangeby.system.ui.reimburse

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormReimburseActivity : AppCompatActivity() {

    private lateinit var etDate: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnUpload: MaterialButton
    private lateinit var ivAttachment: ImageView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var selectedDate: String = ""
    private var imageUri: Uri? = null
    private var imageFile: File? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val resultCode = result.resultCode
        val data = result.data
        if (resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imageUri?.let {
                imageFile = File(it.path ?: "")
                ivAttachment.visibility = View.VISIBLE
                ivAttachment.setImageURI(it)
                btnUpload.text = "Ubah Gambar"
            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_reimburse)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etDate = findViewById(R.id.etDate)
        spinnerType = findViewById(R.id.spinnerType)
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        btnUpload = findViewById(R.id.btnUpload)
        ivAttachment = findViewById(R.id.ivAttachment)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)

        setupSpinner()
        
        etDate.setOnClickListener {
            showDatePicker()
        }

        btnUpload.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .createIntent { intent ->
                    imagePickerLauncher.launch(intent)
                }
        }

        btnSubmit.setOnClickListener {
            submitForm()
        }
    }

    private fun setupSpinner() {
        val types = arrayOf("Medical", "Travel", "Supplies", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = format.format(calendar.time)
                etDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun submitForm() {
        val type = spinnerType.selectedItem.toString()
        val amountStr = etAmount.text.toString().trim()
        val desc = etDescription.text.toString().trim()

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Silakan pilih tanggal", Toast.LENGTH_SHORT).show()
            return
        }
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Silakan isi nominal", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())
        val dateBody = selectedDate.toRequestBody("text/plain".toMediaTypeOrNull())
        val amountBody = amountStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val descBody = desc.toRequestBody("text/plain".toMediaTypeOrNull())

        var imagePart: MultipartBody.Part? = null
        imageFile?.let { file ->
            if (file.exists()) {
                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("attachment", file.name, reqFile)
            }
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@FormReimburseActivity).createReimbursement(
                    date = dateBody,
                    type = typeBody,
                    amount = amountBody,
                    description = descBody,
                    attachment = imagePart
                )
                progressBar.visibility = View.GONE
                Toast.makeText(this@FormReimburseActivity, "Reimburse berhasil diajukan", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true
                Toast.makeText(this@FormReimburseActivity, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
