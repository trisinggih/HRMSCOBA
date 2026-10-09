package com.jangeby.system.ui.pengajuan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jangeby.system.databinding.ActivityFormPengajuanBinding
import java.util.Calendar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.model.CreatePermitRequest
import com.jangeby.system.data.remote.LeaveRequestCreate
import com.jangeby.system.data.remote.LateRequestData
import com.jangeby.system.data.remote.LeaveBalanceItem
import android.widget.ArrayAdapter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody


class FormPengajuanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormPengajuanBinding
    private var selectedImageUri: Uri? = null
    private var leaveBalances: List<LeaveBalanceItem> = emptyList()
    private var selectedLeaveTypeId: Long? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPhoto.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormPengajuanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra("TYPE") ?: "Pengajuan"
        binding.toolbar.title = "Pengajuan $type"
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupFormFields(type)
        
        binding.btnUpload.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSubmit.setOnClickListener {
            val reason = binding.etReason.text.toString()
            val startDate = binding.etStartDate.text.toString()
            val endDate = binding.etEndDate.text.toString()
            val startTime = binding.etStartTime.text.toString()
            val endTime = binding.etEndTime.text.toString()

            if (type == "Sakit" && selectedImageUri == null) {
                Toast.makeText(this, "Bukti foto sakit wajib disertakan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (reason.isEmpty()) {
                Toast.makeText(this, "Harap isi alasan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmit.isEnabled = false

            val apiService = ApiClient.getService(this)

            lifecycleScope.launch {
                try {
                    val response = when (type) {
                        "Cuti" -> {
                            if (selectedLeaveTypeId == null) {
                                runOnUiThread { Toast.makeText(this@FormPengajuanActivity, "Pilih tipe cuti", Toast.LENGTH_SHORT).show() }
                                binding.progressBar.visibility = View.GONE
                                binding.btnSubmit.isEnabled = true
                                return@launch
                            }
                            val req = LeaveRequestCreate(
                                leaveTypeId = selectedLeaveTypeId!!,
                                startDate = startDate,
                                endDate = endDate,
                                reason = reason
                            )
                            apiService.createLeaveRequest(req)
                        }
                        "Sakit" -> {
                            val finalEndDate = if (endDate.isNotBlank()) endDate else startDate
                            val rbStartDate = startDate.toRequestBody("text/plain".toMediaTypeOrNull())
                            val rbEndDate = finalEndDate.toRequestBody("text/plain".toMediaTypeOrNull())
                            val rbReason = reason.toRequestBody("text/plain".toMediaTypeOrNull())
                            
                            var photoPart: okhttp3.MultipartBody.Part? = null
                            selectedImageUri?.let { uri ->
                                val inputStream = contentResolver.openInputStream(uri)
                                val bytes = inputStream?.readBytes()
                                inputStream?.close()
                                if (bytes != null) {
                                    val reqFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                                    photoPart = okhttp3.MultipartBody.Part.createFormData("medical_certificate", "sakit.jpg", reqFile)
                                }
                            }
                            
                            apiService.createSickLeaveRequest(rbStartDate, rbEndDate, rbReason, photoPart)
                        }
                        "Ijin" -> {
                            val typePart = "personal".toRequestBody("text/plain".toMediaTypeOrNull())
                            val datePart = startDate.toRequestBody("text/plain".toMediaTypeOrNull())
                            val startPart = startTime.toRequestBody("text/plain".toMediaTypeOrNull())
                            val endPart = endTime.toRequestBody("text/plain".toMediaTypeOrNull())
                            val reasonPart = reason.toRequestBody("text/plain".toMediaTypeOrNull())
                            
                            var photoPart: okhttp3.MultipartBody.Part? = null
                            selectedImageUri?.let { uri ->
                                val inputStream = contentResolver.openInputStream(uri)
                                val bytes = inputStream?.readBytes()
                                inputStream?.close()
                                if (bytes != null) {
                                    val reqFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                                    photoPart = okhttp3.MultipartBody.Part.createFormData("attachment", "ijin.jpg", reqFile)
                                }
                            }
                            
                            apiService.createPermitRequest(typePart, datePart, startPart, endPart, reasonPart, photoPart)
                        }
                        "Koreksi Absen" -> {
                            val correctionType = binding.etType.text.toString()
                            if (correctionType.isEmpty()) {
                                throw Exception("Harap pilih tipe koreksi")
                            }
                            val reqTypeStr = if (correctionType == "Clock In") "clock_in" else "clock_out"
                            
                            val rbType = reqTypeStr.toRequestBody("text/plain".toMediaTypeOrNull())
                            val rbDate = startDate.toRequestBody("text/plain".toMediaTypeOrNull())
                            val rbTime = startTime.toRequestBody("text/plain".toMediaTypeOrNull())
                            val rbReason = reason.toRequestBody("text/plain".toMediaTypeOrNull())
                            
                            var photoPart: okhttp3.MultipartBody.Part? = null
                            selectedImageUri?.let { uri ->
                                val inputStream = contentResolver.openInputStream(uri)
                                val bytes = inputStream?.readBytes()
                                inputStream?.close()
                                if (bytes != null) {
                                    val reqFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                                    photoPart = okhttp3.MultipartBody.Part.createFormData("attachment", "koreksi.jpg", reqFile)
                                }
                            }
                            apiService.createAttendanceCorrection(rbType, rbDate, rbTime, rbReason, photoPart)
                        }
                        "Tukar Hari" -> {
                            val map = mapOf(
                                "work_date" to startDate,
                                "swap_to_date" to endDate,
                                "reason" to reason
                            )
                            apiService.createWorkdaySwap(map)
                        }
                        else -> throw Exception("Tipe pengajuan tidak valid")
                    }

                    if (response.success) {
                        Toast.makeText(this@FormPengajuanActivity, "Pengajuan $type berhasil dikirim", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@FormPengajuanActivity, response.message, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@FormPengajuanActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                }
            }
        }
        
        setupDatePickers()
        setupTimePickers()
    }

    private fun setupFormFields(type: String) {
        when (type) {
            "Cuti" -> {
                binding.tilStartDate.visibility = View.VISIBLE
                binding.tilEndDate.visibility = View.VISIBLE
                binding.tilType.visibility = View.VISIBLE
                binding.tilType.hint = "Tipe Cuti"
                fetchLeaveBalances()
            }
            "Sakit" -> {
                binding.tilStartDate.visibility = View.VISIBLE
                binding.tilEndDate.visibility = View.VISIBLE
                // Request mandatory photo will be handled at submit
            }
            "Ijin" -> {
                binding.tilStartDate.visibility = View.VISIBLE
                binding.tilStartTime.visibility = View.VISIBLE
                binding.tilEndTime.visibility = View.VISIBLE
            }
            "Koreksi Absen" -> {
                binding.tilType.visibility = View.VISIBLE
                binding.tilStartDate.visibility = View.VISIBLE
                binding.tilStartTime.visibility = View.VISIBLE
                
                // Set dropdown options
                val types = arrayOf("Clock In", "Clock Out")
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
                (binding.etType as android.widget.AutoCompleteTextView).setAdapter(adapter)
            }
            "Tukar Hari" -> {
                binding.tilStartDate.visibility = View.VISIBLE
                binding.tilStartDate.hint = "Tanggal (Kerja)"
                binding.tilEndDate.visibility = View.VISIBLE
                binding.tilEndDate.hint = "Ditukar ke Tanggal (Tujuan)"
            }
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        binding.etStartDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                binding.etStartDate.setText("$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}")
            }, year, month, day).show()
        }

        binding.etEndDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                binding.etEndDate.setText("$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}")
            }, year, month, day).show()
        }
    }

    private fun setupTimePickers() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        binding.etStartTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                val timeString = String.format("%02d:%02d", h, m)
                binding.etStartTime.setText(timeString)
            }, hour, minute, true).show()
        }

        binding.etEndTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                val timeString = String.format("%02d:%02d", h, m)
                binding.etEndTime.setText(timeString)
            }, hour, minute, true).show()
        }
    }

    private fun fetchLeaveBalances() {
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getService(this@FormPengajuanActivity)
                val response = apiService.leaveBalance()
                if (response.success && response.data != null) {
                    leaveBalances = response.data
                    val names = leaveBalances.map { it.leaveTypeName }
                    val adapter = ArrayAdapter(this@FormPengajuanActivity, android.R.layout.simple_dropdown_item_1line, names)
                    binding.etType.setAdapter(adapter)
                    
                    binding.etType.setOnItemClickListener { _, _, position, _ ->
                        val selected = leaveBalances[position]
                        selectedLeaveTypeId = selected.leaveTypeId
                        binding.tvLeaveBalance.visibility = View.VISIBLE
                        binding.tvLeaveBalance.text = "Saldo Cuti: ${selected.remaining} hari"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
