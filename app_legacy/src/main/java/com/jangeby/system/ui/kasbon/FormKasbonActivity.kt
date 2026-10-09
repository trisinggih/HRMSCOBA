package com.jangeby.system.ui.kasbon

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.CreateLoanRequest
import com.jangeby.system.ui.UiHelpers
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class FormKasbonActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var etReason: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var cardPhoto: View
    private lateinit var ivPhoto: android.widget.ImageView
    private lateinit var tvPhotoLabel: android.widget.TextView
    private var selectedImageUri: android.net.Uri? = null

    private val pickImage = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let {
            selectedImageUri = it
            ivPhoto.setImageURI(it)
            ivPhoto.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            tvPhotoLabel.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_kasbon)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etAmount = findViewById(R.id.etAmount)
        etReason = findViewById(R.id.etReason)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)
        cardPhoto = findViewById(R.id.cardPhoto)
        ivPhoto = findViewById(R.id.ivPhoto)
        tvPhotoLabel = findViewById(R.id.tvPhotoLabel)

        cardPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString() != current) {
                    etAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace(Regex("[Rp,.]"), "").trim()
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toDouble()
                            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(parsed)
                            current = formatted
                            etAmount.setText(formatted)
                            etAmount.setSelection(formatted.length)
                        } catch (e: Exception) {
                            // ignore
                        }
                    } else {
                        current = ""
                        etAmount.setText("")
                    }

                    etAmount.addTextChangedListener(this)
                }
            }
        })

        btnSubmit.setOnClickListener {
            submitForm()
        }
    }

    private fun getFileFromUri(uri: android.net.Uri): java.io.File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(cacheDir, "kasbon_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun submitForm() {
        val amountStr = etAmount.text.toString().replace(Regex("[Rp,.]"), "").trim()
        val reason = etReason.text.toString().trim()

        if (amountStr.isEmpty() || reason.isEmpty()) {
            Toast.makeText(this, "Harap isi nominal dan alasan", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val reqAmount = amountStr.toRequestBody(okhttp3.MultipartBody.FORM)
                val reqReason = reason.toRequestBody(okhttp3.MultipartBody.FORM)

                var photoPart: okhttp3.MultipartBody.Part? = null
                if (selectedImageUri != null) {
                    val file = getFileFromUri(selectedImageUri!!)
                    if (file != null) {
                        val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        photoPart = okhttp3.MultipartBody.Part.createFormData("photo", file.name, reqFile)
                    }
                }

                val response = ApiClient.getService(this@FormKasbonActivity).createLoanRequest(reqAmount, reqReason, photoPart)
                Toast.makeText(this@FormKasbonActivity, response.message ?: "Pengajuan kasbon berhasil dikirim", Toast.LENGTH_SHORT).show()
                finish()
            } catch (t: Throwable) {
                btnSubmit.isEnabled = true
                Toast.makeText(this@FormKasbonActivity, UiHelpers.errorMessageFrom(this@FormKasbonActivity, t), Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}
