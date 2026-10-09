package com.jangeby.system.ui.attendance

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.local.SecurePrefs
import com.jangeby.system.data.remote.*
import com.jangeby.system.ui.UiHelpers
import com.jangeby.system.ui.camera.CameraActivity
import kotlinx.coroutines.launch

class AttendanceActivity : AppCompatActivity() {

    private lateinit var progress: View
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var btnClockIn: Button
        private lateinit var btnClockOut: Button
    private lateinit var btnOvertimeClockIn: Button
    private lateinit var btnOvertimeClockOut: Button
    private lateinit var tvFaceStatus: TextView

    private var historyAdapter: HistoryAdapter = HistoryAdapter(emptyList())

    /** Launcher untuk MapAttendanceActivity */
    private val mapLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Berhasil clock in/out di MapAttendanceActivity
                loadTodayStatus()
            }
        }

    /** Launcher untuk Registrasi Wajah */
    private val registerFaceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                checkFaceStatus() // refresh status
            }
        }

    // ─── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)
        setupViews()
        loadTodayStatus()
        checkFaceStatus()
    }

    private fun setupViews() {
        progress    = findViewById(R.id.progressBar)
        rvHistory   = findViewById(R.id.rvHistory)
        tvStatus    = findViewById(R.id.tvStatus)
        btnClockIn  = findViewById(R.id.btnClockIn)
                btnClockOut = findViewById(R.id.btnClockOut)
        btnOvertimeClockIn = findViewById(R.id.btnOvertimeClockIn)
        btnOvertimeClockOut = findViewById(R.id.btnOvertimeClockOut)

        //tvFaceStatus = findViewById<TextView?>(R.id.tvFaceStatus) ?: TextView(this)

        
        btnOvertimeClockIn.setOnClickListener {
            val intent = Intent(this, MapAttendanceActivity::class.java)
            intent.putExtra("action", "overtime_clock_in")
            mapLauncher.launch(intent)
        }
        btnOvertimeClockOut.setOnClickListener {
            val intent = Intent(this, MapAttendanceActivity::class.java)
            intent.putExtra("action", "overtime_clock_out")
            mapLauncher.launch(intent)
        }

        btnClockIn.setOnClickListener {
            val intent = Intent(this, MapAttendanceActivity::class.java)
            intent.putExtra("action", "clock_in")
            mapLauncher.launch(intent)
        }

        btnClockOut.setOnClickListener {
            val intent = Intent(this, MapAttendanceActivity::class.java)
            intent.putExtra("action", "clock_out")
            mapLauncher.launch(intent)
        }

//        findViewById<View?>(R.id.btnRegisterFace)?.setOnClickListener {
//            openRegisterFace()
//        }
        
        rvHistory.layoutManager = LinearLayoutManager(this)
    }

    // ─── Face Status ────────────────────────────────────────────────────

    private fun checkFaceStatus() {
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@AttendanceActivity).faceStatus()
                val hasFace = res.data?.hasFace == true
                tvFaceStatus.text = if (hasFace) "✅ Wajah terdaftar" else "⚠️ Wajah belum terdaftar – tap untuk daftar"
                tvFaceStatus.setOnClickListener { if (!hasFace) openRegisterFace() }
            } catch (_: Throwable) { /* silent */ }
        }
    }

    private fun openRegisterFace() {
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_REGISTER)
        registerFaceLauncher.launch(intent)
    }

    // ─── Status & History ────────────────────────────────────────────────

    private fun loadTodayStatus() {
        setLoading(true)
        val empDbId = SecurePrefs(this).dbId
        val today = java.time.LocalDate.now()
        
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@AttendanceActivity).getShiftCalendar(
                    employeeId = empDbId,
                    year  = today.year,
                    month = today.monthValue
                )
                
                val items = res.data ?: emptyList()
                historyAdapter = HistoryAdapter(items)
                rvHistory.adapter = historyAdapter
                
                // Find today's shift and bind to UI
                val todayStr = today.toString()
                val todayItem = items.find { it.date == todayStr }
                bindTodayStatus(todayItem, todayStr)
                
            } catch (t: Throwable) {
                showMessage("Gagal memuat data: ${t.localizedMessage}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun bindTodayStatus(item: CalendarItem?, todayStr: String) {
        val dateHeader = findViewById<TextView>(R.id.tvDateHeader)
        dateHeader.text = formatDateHeader(item?.date ?: todayStr)

        tvStatus.text = if (item == null) {
            "Belum ada jadwal hari ini."
        } else {
            val shiftText = item.shift?.name ?: if (item.isHoliday == true) "Libur Nasional" else "Libur/Off"
            val sStart = item.shift?.startTime?.substring(0, 5) ?: "-"
            val sEnd = item.shift?.endTime?.substring(0, 5) ?: "-"
            
            if (item.shift == null) {
                shiftText
            } else {
                "$shiftText ($sStart - $sEnd)"
            }
        }

        val att = item?.attendance
        val isCheckedIn = att != null && !att.clockIn.isNullOrBlank()
        val isCheckedOut = att != null && !att.clockOut.isNullOrBlank()

        
        val hasApprovedOvertime = item?.hasApprovedOvertime == true
        val isOvertimeIn = att != null && !att.overtimeIn.isNullOrBlank()
        val isOvertimeOut = att != null && !att.overtimeOut.isNullOrBlank()

        if (isCheckedOut) {
            btnClockIn.visibility = View.GONE
            btnClockOut.visibility = View.GONE
            
            if (hasApprovedOvertime) {
                if (isOvertimeOut) {
                    btnOvertimeClockIn.visibility = View.GONE
                    btnOvertimeClockOut.visibility = View.GONE
                } else if (isOvertimeIn) {
                    btnOvertimeClockIn.visibility = View.GONE
                    btnOvertimeClockOut.visibility = View.VISIBLE
                } else {
                    btnOvertimeClockIn.visibility = View.VISIBLE
                    btnOvertimeClockOut.visibility = View.GONE
                }
            } else {
                btnOvertimeClockIn.visibility = View.GONE
                btnOvertimeClockOut.visibility = View.GONE
            }
        } else if (isCheckedIn) {
            btnClockIn.visibility = View.GONE
            btnClockOut.visibility = View.VISIBLE
            btnOvertimeClockIn.visibility = View.GONE
            btnOvertimeClockOut.visibility = View.GONE
        } else {
            btnClockIn.visibility = View.VISIBLE
            btnClockOut.visibility = View.GONE
            btnOvertimeClockIn.visibility = View.GONE
            btnOvertimeClockOut.visibility = View.GONE
        }

    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun formatDateHeader(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val ld = java.time.LocalDate.parse(dateStr)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", java.util.Locale("id", "ID"))
            ld.format(formatter)
        } catch (_: Throwable) { 
            dateStr 
        }
    }

    private fun formatTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val dt = java.time.OffsetDateTime.parse(iso).withNano(0)
            dt.toLocalTime().toString().substring(0, 5)
        } catch (_: Throwable) { iso }
    }

    private fun fmt(v: Double?) = v?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it)
    } ?: "0"
}
