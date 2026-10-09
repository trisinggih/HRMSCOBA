package com.jangeby.system.ui.reimburse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ReimburseActivity : AppCompatActivity() {

    private lateinit var rvReimbursements: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var pbLoading: View
    private lateinit var fabAdd: FloatingActionButton
    
    private val items = mutableListOf<com.jangeby.system.data.remote.UnifiedPengajuan>()
    private lateinit var adapter: com.jangeby.system.ui.pengajuan.PengajuanListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reimburse)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvReimbursements = findViewById(R.id.rvReimbursements)
        tvEmpty = findViewById(R.id.tvEmpty)
        pbLoading = findViewById(R.id.progressBar)
        fabAdd = findViewById(R.id.fabAdd)

        adapter = com.jangeby.system.ui.pengajuan.PengajuanListAdapter(items, { item ->
            showTrackingDialog(item)
        }, { item ->
            cancelReimburse(item)
        })
        rvReimbursements.layoutManager = LinearLayoutManager(this)
        rvReimbursements.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, FormReimburseActivity::class.java))
        }

        loadReimbursements()
    }

    override fun onResume() {
        super.onResume()
        loadReimbursements()
    }

    private fun loadReimbursements() {
        pbLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rvReimbursements.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@ReimburseActivity).myReimbursements()
                val list = response.data?.data ?: emptyList()

                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    items.clear()
                    list.forEach {
                        val amountStr = it.amount?.let { amt -> java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).format(amt) } ?: "Rp 0"
                        val baseUrl = com.jangeby.system.data.remote.ApiClient.BASE_URL.replace("api/v1/", "storage/")
                        val fullUrl = it.attachment?.let { path -> if(path.startsWith("http")) path else "$baseUrl$path" }
                        
                        items.add(
                            com.jangeby.system.data.remote.UnifiedPengajuan(
                                id = it.id,
                                type = "Klaim/Reimburse",
                                title = "${it.type ?: "Reimburse"} - $amountStr",
                                date = it.date ?: "",
                                reason = it.description ?: "-",
                                status = it.status ?: "pending",
                                approval = it.approval,
                                attachmentUrl = fullUrl
                            )
                        )
                    }
                    adapter.updateData(items)
                    rvReimbursements.visibility = View.VISIBLE
                }
            } catch (t: Throwable) {
                Toast.makeText(this@ReimburseActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
            } finally {
                pbLoading.visibility = View.GONE
            }
        }
    }

    private fun showTrackingDialog(item: com.jangeby.system.data.remote.UnifiedPengajuan) {
        val approval = item.approval
        if (approval == null) {
            Toast.makeText(this, "Data tracking tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = android.view.LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet_tracking, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)

        val rvTracking = view.findViewById<RecyclerView>(R.id.rvTracking)
        rvTracking.layoutManager = LinearLayoutManager(this)
        
        val service = ApiClient.getService(this)
        lifecycleScope.launch {
            try {
                val res = service.getApprovalRequest(approval.id)
                if (res.success && res.data != null) {
                    val steps = res.data.workflow?.steps ?: emptyList()
                    val logs = res.data.logs ?: emptyList()
                    val adapter = com.jangeby.system.ui.approval.TrackingAdapter(steps, logs)
                    rvTracking.adapter = adapter
                } else {
                    Toast.makeText(this@ReimburseActivity, "Gagal memuat tracking: ${res.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReimburseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        bottomSheetDialog.show()
    }

    private fun cancelReimburse(item: com.jangeby.system.data.remote.UnifiedPengajuan) {
        val approval = item.approval
        if (approval == null) {
            Toast.makeText(this, "Tidak dapat membatalkan pengajuan ini", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_confirmation, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvBsTitle)
        val tvMessage = view.findViewById<android.widget.TextView>(R.id.tvBsMessage)
        val btnPositive = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBsPositive)
        val btnNegative = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBsNegative)
        
        tvTitle.text = "Batalkan Reimburse"
        tvMessage.text = "Apakah Anda yakin ingin membatalkan pengajuan ini?"
        btnPositive.text = "Ya"
        btnNegative.text = "Tidak"
        
        btnPositive.setOnClickListener {
            val service = ApiClient.getService(this)
            lifecycleScope.launch {
                try {
                    val response = service.cancelApproval(approval.id)
                    if (response.success) {
                        Toast.makeText(this@ReimburseActivity, "Pengajuan dibatalkan", Toast.LENGTH_SHORT).show()
                        loadReimbursements()
                    } else {
                        Toast.makeText(this@ReimburseActivity, response.message ?: "Gagal membatalkan", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ReimburseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            bottomSheetDialog.dismiss()
        }
        
        btnNegative.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }
}
