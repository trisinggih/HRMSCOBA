package com.jangeby.system.ui.overtime

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.databinding.ActivityOvertimeBinding
import kotlinx.coroutines.launch

class OvertimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOvertimeBinding
    private lateinit var adapter: OvertimeAdapter

    private fun setupRecyclerView() {
        adapter = OvertimeAdapter({ item ->
            showTrackingDialog(item)
        }, { item ->
            cancelOvertime(item)
        })
        binding.rvOvertime.layoutManager = LinearLayoutManager(this)
        binding.rvOvertime.adapter = adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOvertimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        
        setupRecyclerView()

        binding.swipeRefresh.setOnRefreshListener { loadData() }
        
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, FormOvertimeActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@OvertimeActivity).getOvertimeHistory()
                if (res.success && res.data?.data != null) {
                    val list = res.data.data
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                // handle error
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showTrackingDialog(item: com.jangeby.system.data.remote.OvertimeRequestItem) {
        val approval = item.approval
        if (approval == null) {
            android.widget.Toast.makeText(this, "Data tracking tidak tersedia", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = android.view.LayoutInflater.from(this).inflate(com.jangeby.system.R.layout.layout_bottom_sheet_tracking, null)
        bottomSheetDialog.setContentView(view)
        
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)

        val rvTracking = view.findViewById<androidx.recyclerview.widget.RecyclerView>(com.jangeby.system.R.id.rvTracking)
        rvTracking.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
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
                    android.widget.Toast.makeText(this@OvertimeActivity, "Gagal memuat tracking: ${res.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@OvertimeActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        bottomSheetDialog.show()
    }

    private fun cancelOvertime(item: com.jangeby.system.data.remote.OvertimeRequestItem) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(com.jangeby.system.R.layout.layout_bottom_sheet_confirmation, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val tvTitle = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsTitle)
        val tvMessage = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsMessage)
        val btnPositive = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsPositive)
        val btnNegative = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsNegative)
        
        tvTitle.text = "Batalkan Lembur"
        tvMessage.text = "Apakah Anda yakin ingin membatalkan pengajuan lembur ini?"
        btnPositive.text = "Ya"
        btnNegative.text = "Tidak"
        
        btnPositive.setOnClickListener {
            val service = ApiClient.getService(this)
            lifecycleScope.launch {
                try {
                    val req = mapOf("reason" to "Dibatalkan oleh karyawan")
                    val response = service.cancelOvertime(item.id, req)
                    if (response.success) {
                        android.widget.Toast.makeText(this@OvertimeActivity, "Pengajuan lembur dibatalkan", android.widget.Toast.LENGTH_SHORT).show()
                        loadData()
                    } else {
                        android.widget.Toast.makeText(this@OvertimeActivity, response.message ?: "Gagal membatalkan", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@OvertimeActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
