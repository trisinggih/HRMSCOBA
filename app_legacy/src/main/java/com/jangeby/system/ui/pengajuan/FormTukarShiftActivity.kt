package com.jangeby.system.ui.pengajuan

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.databinding.ActivityFormTukarShiftBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormTukarShiftActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormTukarShiftBinding

    private var workDateStr = ""
    private var swapToDateStr = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormTukarShiftBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupDatePickers()

        binding.btnSubmit.setOnClickListener {
            submitData()
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        binding.etWorkDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    workDateStr = dateFormat.format(calendar.time)
                    binding.etWorkDate.setText(displayFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etSwapToDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    swapToDateStr = dateFormat.format(calendar.time)
                    binding.etSwapToDate.setText(displayFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun submitData() {
        val reason = binding.etReason.text.toString().trim()

        if (workDateStr.isEmpty() || swapToDateStr.isEmpty() || reason.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua field", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = "Mengirim..."

        val service = ApiClient.getService(this)

        lifecycleScope.launchWhenStarted {
            try {
                // Assuming you have createWorkdaySwapRequest in ApiService
                val body = mapOf(
                    "work_date" to workDateStr,
                    "swap_to_date" to swapToDateStr,
                    "reason" to reason
                )
                
                // For now, let's just make a raw retrofit call or add it to ApiService if needed
                val res = service.createWorkdaySwap(body)
                if (res.success) {
                    Toast.makeText(this@FormTukarShiftActivity, "Pengajuan berhasil dikirim", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@FormTukarShiftActivity, res.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FormTukarShiftActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = "AJUKAN TUKAR SHIFT"
            }
        }
    }
}
