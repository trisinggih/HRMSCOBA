import os

FILE_PATH = r"d:\Project\HRMS\mobile\app\src\main\java\com\jangeby\hrms\ui\pengajuan\FormPengajuanActivity.kt"

with open(FILE_PATH, "r", encoding="utf-8") as f:
    content = f.read()

IMPORTS = """import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.model.CreatePermitRequest
import com.jangeby.system.data.model.LeaveRequestCreate
import com.jangeby.system.data.remote.LateRequestData
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
"""

if "import androidx.lifecycle.lifecycleScope" not in content:
    content = content.replace("import java.util.Calendar", "import java.util.Calendar\n" + IMPORTS)


OLD_SUBMIT = """        binding.btnSubmit.setOnClickListener {
            val reason = binding.etReason.text.toString()
            if (reason.isEmpty()) {
                Toast.makeText(this, "Harap isi alasan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmit.isEnabled = false

            it.postDelayed({
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Pengajuan $type berhasil dikirim", Toast.LENGTH_LONG).show()
                finish()
            }, 1000)
        }"""

NEW_SUBMIT = """        binding.btnSubmit.setOnClickListener {
            val reason = binding.etReason.text.toString()
            val startDate = binding.etStartDate.text.toString()
            val endDate = binding.etEndDate.text.toString()
            val startTime = binding.etStartTime.text.toString()
            val endTime = binding.etEndTime.text.toString()

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
                            val req = LeaveRequestCreate(
                                leave_type_id = 1, // Defaulting to 1 for now, ideally selected from a dropdown
                                start_date = startDate,
                                end_date = endDate,
                                reason = reason
                            )
                            apiService.createLeaveRequest(req)
                        }
                        "Sakit" -> {
                            val rbStartDate = startDate.toRequestBody("text/plain".toMediaTypeOrNull())
                            val rbEndDate = endDate.toRequestBody("text/plain".toMediaTypeOrNull())
                            val rbReason = reason.toRequestBody("text/plain".toMediaTypeOrNull())
                            // Ignore photo upload for now or implement if needed
                            apiService.createSickLeaveRequest(rbStartDate, rbEndDate, rbReason, null)
                        }
                        "Ijin" -> {
                            val req = CreatePermitRequest(
                                type = "Izin",
                                date = startDate,
                                start_time = startTime,
                                end_time = endTime,
                                reason = reason
                            )
                            apiService.createPermitRequest(req)
                        }
                        "Telat" -> {
                            val req = LateRequestData(
                                date = startDate,
                                late_time = startTime,
                                reason = reason
                            )
                            apiService.lateRequest(req)
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
        }"""

if OLD_SUBMIT in content:
    content = content.replace(OLD_SUBMIT, NEW_SUBMIT)
    with open(FILE_PATH, "w", encoding="utf-8") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Could not find submit block")
