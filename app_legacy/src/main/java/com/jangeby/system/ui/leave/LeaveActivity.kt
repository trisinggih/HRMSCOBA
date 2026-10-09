package com.jangeby.system.ui.leave

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.LeaveBalanceItem
import com.jangeby.system.data.remote.LeaveRequestCreate
import com.jangeby.system.ui.UiHelpers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Layar Cuti: saldo + daftar permohonan + form ajukan.
 * Terhubung ke endpoint /leave/balance, /leave/request.
 *
 * Catatan: endpoint /leave-types bersifat admin; sebagai gantinya opsi tipe cuti
 * diambil dari relasi leaveType yang ada pada data saldo (/leave/balance).
 */
class LeaveActivity : AppCompatActivity() {

    private lateinit var tvSaldo: TextView
    private lateinit var spinnerType: Spinner
    private lateinit var etStart: EditText
    private lateinit var etEnd: EditText
    private lateinit var etReason: EditText
    private lateinit var btnSubmit: Button
    private lateinit var progress: View
    private lateinit var rvRequests: RecyclerView
    private lateinit var tvRequestsTitle: TextView

    private var balances: List<LeaveBalanceItem> = emptyList()
    private var requestsAdapter: LeaveRequestAdapter = LeaveRequestAdapter(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leave)
        supportActionBar?.title = "Cuti"

        tvSaldo = findViewById(R.id.tvSaldo)
        spinnerType = findViewById(R.id.spinnerLeaveType)
        etStart = findViewById(R.id.etStartDate)
        etEnd = findViewById(R.id.etEndDate)
        etReason = findViewById(R.id.etReason)
        btnSubmit = findViewById(R.id.btnSubmitLeave)
        progress = findViewById(R.id.progressBar)
        tvRequestsTitle = findViewById(R.id.tvRequestsTitle)
        rvRequests = findViewById(R.id.rvLeaveRequests)
        rvRequests.layoutManager = LinearLayoutManager(this)

        btnSubmit.setOnClickListener { submitLeave() }

        loadData()
    }

    private fun loadData() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(this@LeaveActivity)
                val balanceRes = api.leaveBalance(year = java.time.Year.now().value)
                balances = balanceRes.data ?: emptyList()
                bindBalance(balances)
                setupTypeSpinner()
            } catch (t: Throwable) {
                tvSaldo.text = "Gagal memuat saldo: ${UiHelpers.errorMessageFrom(this@LeaveActivity, t)}"
            } finally {
                setLoading(false)
            }
            loadRequests()
        }
    }

    private fun bindBalance(items: List<LeaveBalanceItem>) {
        if (items.isEmpty()) {
            tvSaldo.text = "Tidak ada saldo cuti tahun ini."
            return
        }
        tvSaldo.text = buildString {
            append("Saldo Cuti Tahun Ini\n")
            items.forEach { b ->
                val name = b.leaveTypeName
                append("• $name: sisa ${fmt(b.remaining)} / kuota ${fmt(b.quota)}\n")
            }
        }.trim()
    }

    /** Opsi tipe cuti dari relasi leaveType pada data saldo. */
    private fun setupTypeSpinner() {
        val options = balances
        if (options.isEmpty()) {
            spinnerType.setSelection(0)
            return
        }
        val labels = options.map { "${it.leaveTypeName} (sisa ${fmt(it.remaining)})" }
        spinnerType.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, labels
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun submitLeave() {
        val options = balances
        if (options.isEmpty()) {
            showMessage("Tidak ada tipe cuti yang tersedia")
            return
        }
        val index = spinnerType.selectedItemPosition
        if (index < 0 || index >= options.size) {
            showMessage("Pilih tipe cuti terlebih dahulu")
            return
        }
        val selected = options[index]
        val start = etStart.text.toString().trim()
        val end = etEnd.text.toString().trim()
        val reason = etReason.text.toString().trim()

        if (start.isEmpty() || end.isEmpty()) {
            showMessage("Tanggal mulai dan selesai wajib diisi (format YYYY-MM-DD)")
            return
        }
        if (reason.isEmpty()) {
            showMessage("Alasan wajib diisi")
            return
        }
        if (start > end) {
            showMessage("Tanggal selesai tidak boleh sebelum tanggal mulai")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@LeaveActivity).createLeaveRequest(
                    LeaveRequestCreate(
                        leaveTypeId = selected.leaveTypeId,
                        startDate = start,
                        endDate = end,
                        reason = reason
                    )
                )
                showMessage(res.message ?: "Permohonan cuti dikirim")
                etReason.text?.clear()
                loadData()
            } catch (t: Throwable) {
                showMessage(UiHelpers.errorMessageFrom(this@LeaveActivity, t))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun loadRequests() {
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@LeaveActivity).myLeaveRequests(perPage = 30)
                val items = res.data?.data ?: emptyList()
                requestsAdapter = LeaveRequestAdapter(items)
                rvRequests.adapter = requestsAdapter
                tvRequestsTitle.text = "Permohonan Cuti (${items.size})"
            } catch (t: Throwable) {
                tvRequestsTitle.text = "Gagal memuat permohonan"
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !loading
    }

    private fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun fmt(v: Double?): String = v?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it) } ?: "0"
}
