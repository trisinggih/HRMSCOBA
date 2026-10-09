package com.jangeby.system.ui.overtime

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.databinding.ActivityFormOvertimeBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class FormOvertimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormOvertimeBinding
    private var selectedFileUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        binding.tvFileName.text = it.getString(nameIndex)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormOvertimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                binding.tvDate.text = String.format("%04d-%02d-%02d", year, month + 1, day)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.tvStartTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                if (hour < 20 || (hour == 20 && minute < 30)) {
                    Toast.makeText(this, "Jam mulai lembur minimal pukul 20:30", Toast.LENGTH_SHORT).show()
                    binding.tvStartTime.text = "20:30"
                } else {
                    binding.tvStartTime.text = String.format("%02d:%02d", hour, minute)
                }
            }, 20, 30, true).show()
        }

        binding.tvEndTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                binding.tvEndTime.text = String.format("%02d:%02d", hour, minute)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        binding.btnUpload.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.btnSubmit.setOnClickListener {
            val date = binding.tvDate.text.toString()
            val start = binding.tvStartTime.text.toString()
            val end = binding.tvEndTime.text.toString()
            val reason = binding.etReason.text.toString()

            if (date.contains("Pilih") || start.contains("Pilih") || end.contains("Pilih") || reason.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val dateBody = date.toRequestBody("text/plain".toMediaTypeOrNull())
                    val startBody = start.toRequestBody("text/plain".toMediaTypeOrNull())
                    val endBody = end.toRequestBody("text/plain".toMediaTypeOrNull())
                    val reasonBody = reason.toRequestBody("text/plain".toMediaTypeOrNull())

                    var attachmentPart: MultipartBody.Part? = null
                    if (selectedFileUri != null) {
                        val fileDir = cacheDir
                        val fileName = binding.tvFileName.text.toString()
                        val file = File(fileDir, fileName)
                        contentResolver.openInputStream(selectedFileUri!!)?.use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        attachmentPart = MultipartBody.Part.createFormData("attachment", file.name, requestFile)
                    }

                    val res = ApiClient.getService(this@FormOvertimeActivity).createOvertimeRequest(
                        date = dateBody,
                        reason = reasonBody,
                        requestedStartTime = startBody,
                        requestedEndTime = endBody,
                        attachment = attachmentPart
                    )
                    if (res.success) {
                        Toast.makeText(this@FormOvertimeActivity, "Lembur diajukan", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@FormOvertimeActivity, res.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: retrofit2.HttpException) {
                    var errorMsg = "Terjadi kesalahan"
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jsonObject = org.json.JSONObject(errorBody)
                            if (jsonObject.has("message")) {
                                errorMsg = jsonObject.getString("message")
                            }
                        } catch (ex: Exception) { }
                    }
                    Toast.makeText(this@FormOvertimeActivity, errorMsg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@FormOvertimeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
