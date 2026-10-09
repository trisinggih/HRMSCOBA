package com.jangeby.system.ui.permit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jangeby.system.R
import com.jangeby.system.data.remote.UnifiedPengajuan
import com.jangeby.system.data.remote.ApiClient
import androidx.lifecycle.lifecycleScope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PermitActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvPermit: RecyclerView
    private lateinit var tvEmpty: android.widget.TextView
    private lateinit var fabAdd: FloatingActionButton

    private val items = mutableListOf<UnifiedPengajuan>()
    private lateinit var adapter: com.jangeby.system.ui.pengajuan.PengajuanListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permit)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvPermit = findViewById(R.id.rvPermit)
        tvEmpty = findViewById(R.id.tvEmpty)
        fabAdd = findViewById(R.id.fabAdd)

        adapter = com.jangeby.system.ui.pengajuan.PengajuanListAdapter(items, { item ->
            showTrackingDialog(item)
        }, { item ->
            cancelPermit(item)
        })
        rvPermit.layoutManager = LinearLayoutManager(this)
        rvPermit.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            loadData()
        }

        fabAdd.setOnClickListener {
            startActivity(Intent(this, FormPermitActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        val apiService = ApiClient.getService(this)
        
        lifecycleScope.launchWhenStarted {
            try {
                val response = apiService.getPermitHistory()
                swipeRefresh.isRefreshing = false
                
                if (response.success && response.data != null) {
                    items.clear()
                    response.data.data?.forEach {
                        items.add(
                            UnifiedPengajuan(
                                id = it.id.toLong(),
                                type = "Permit User",
                                title = "Permit User: " + (if(it.type == "business") "Dinas Kantor" else "Keperluan Pribadi"),
                                date = it.date ?: "",
                                reason = it.reason ?: "-",
                                status = it.status ?: "pending",
                                approval = it.approval,
                                attachmentUrl = it.attachment
                            )
                        )
                    }
                    adapter.updateData(items)

                    if (items.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvPermit.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvPermit.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@PermitActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@PermitActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTrackingDialog(item: UnifiedPengajuan) {
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
        lifecycleScope.launchWhenStarted {
            try {
                val res = service.getApprovalRequest(approval.id)
                if (res.success && res.data != null) {
                    val steps = res.data.workflow?.steps ?: emptyList()
                    val logs = res.data.logs ?: emptyList()
                    val adapter = com.jangeby.system.ui.approval.TrackingAdapter(steps, logs)
                    rvTracking.adapter = adapter
                } else {
                    Toast.makeText(this@PermitActivity, "Gagal memuat tracking: ${res.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PermitActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        bottomSheetDialog.show()
    }

    private fun cancelPermit(item: UnifiedPengajuan) {
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
        
        tvTitle.text = "Batalkan Permit"
        tvMessage.text = "Apakah Anda yakin ingin membatalkan permit ini?"
        btnPositive.text = "Ya"
        btnNegative.text = "Tidak"
        
        btnPositive.setOnClickListener {
            val service = ApiClient.getService(this)
            lifecycleScope.launchWhenStarted {
                try {
                    val response = service.cancelApproval(approval.id)
                    if (response.success) {
                        Toast.makeText(this@PermitActivity, "Permit dibatalkan", Toast.LENGTH_SHORT).show()
                        loadData()
                    } else {
                        Toast.makeText(this@PermitActivity, response.message ?: "Gagal membatalkan", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@PermitActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
