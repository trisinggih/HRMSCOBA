package com.jangeby.system.ui.pengajuan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.databinding.ActivityPengajuanBinding
import com.google.android.material.tabs.TabLayout

class PengajuanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPengajuanBinding
    private lateinit var adapter: PengajuanListAdapter

    private val tabs = listOf("Sakit", "Ijin", "Tukar Hari", "Cuti")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPengajuanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupTabs()
        setupRecyclerView()
        setupFab()
    }

    private fun setupTabs() {
        tabs.forEach { title ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { pos ->
                    if (pos >= 0 && pos < tabs.size) {
                        loadData(tabs[pos])
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        val currentPos = binding.tabLayout.selectedTabPosition
        if (currentPos >= 0 && currentPos < tabs.size) {
            loadData(tabs[currentPos])
        } else {
            loadData(tabs[0])
        }
        fetchTabBadges()
    }
    
    private fun fetchTabBadges() {
        lifecycleScope.launchWhenStarted {
            try {
                val res = com.jangeby.system.data.remote.ApiClient.getService(this@PengajuanActivity).getBadges()
                if (res.success && res.data != null) {
                    val data = res.data
                    for (i in 0 until binding.tabLayout.tabCount) {
                        val tab = binding.tabLayout.getTabAt(i)
                        val title = tabs.getOrNull(i) ?: continue
                        val count = when (title) {
                            "Sakit" -> data.pengajuanSakitCount
                            "Ijin" -> data.pengajuanIjinCount
                            "Tukar Hari" -> data.pengajuanTukarHariCount
                            "Cuti" -> data.pengajuanCutiCount
                            else -> 0
                        }
                        tab?.text = if (count > 0) "$title ($count)" else title
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = PengajuanListAdapter(emptyList(), { item ->
            showTrackingDialog(item)
        }, { item ->
            cancelPengajuan(item)
        })
        binding.rvPengajuan.layoutManager = LinearLayoutManager(this)
        binding.rvPengajuan.adapter = adapter
    }

    private fun showTrackingDialog(item: com.jangeby.system.data.remote.UnifiedPengajuan) {
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
        
        // Fetch tracking data from server
        val service = com.jangeby.system.data.remote.ApiClient.getService(this)
        lifecycleScope.launchWhenStarted {
            try {
                val res = service.getApprovalRequest(approval.id)
                if (res.success && res.data != null) {
                    val steps = res.data.workflow?.steps ?: emptyList()
                    val logs = res.data.logs ?: emptyList()
                    val adapter = com.jangeby.system.ui.approval.TrackingAdapter(steps, logs)
                    rvTracking.adapter = adapter
                } else {
                    android.widget.Toast.makeText(this@PengajuanActivity, "Gagal memuat tracking: ${res.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@PengajuanActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        bottomSheetDialog.show()
    }

    private fun cancelPengajuan(item: com.jangeby.system.data.remote.UnifiedPengajuan) {
        val approval = item.approval
        if (approval == null) {
            android.widget.Toast.makeText(this, "Tidak dapat membatalkan pengajuan ini", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(com.jangeby.system.R.layout.layout_bottom_sheet_confirmation, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val tvTitle = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsTitle)
        val tvMessage = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsMessage)
        val btnPositive = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsPositive)
        val btnNegative = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsNegative)
        
        tvTitle.text = "Batalkan Pengajuan"
        tvMessage.text = "Apakah Anda yakin ingin membatalkan pengajuan ini?"
        btnPositive.text = "Ya"
        btnNegative.text = "Tidak"
        
        btnPositive.setOnClickListener {
            val service = com.jangeby.system.data.remote.ApiClient.getService(this)
            lifecycleScope.launchWhenStarted {
                try {
                    val response = service.cancelApproval(approval.id)
                    if (response.success) {
                        android.widget.Toast.makeText(this@PengajuanActivity, "Pengajuan dibatalkan", android.widget.Toast.LENGTH_SHORT).show()
                        val currentPos = binding.tabLayout.selectedTabPosition
                        if (currentPos >= 0 && currentPos < tabs.size) {
                            loadData(tabs[currentPos])
                        } else {
                            loadData(tabs[0])
                        }
                        fetchTabBadges()
                    } else {
                        android.widget.Toast.makeText(this@PengajuanActivity, response.message ?: "Gagal membatalkan", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@PengajuanActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            bottomSheetDialog.dismiss()
        }
        
        btnNegative.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            val currentTabPos = binding.tabLayout.selectedTabPosition
            if (currentTabPos >= 0 && currentTabPos < tabs.size) {
                val type = tabs[currentTabPos]
                val intent = when (type) {
                    "Tukar Hari" -> Intent(this, com.jangeby.system.ui.pengajuan.FormTukarShiftActivity::class.java)
                    "Ijin" -> Intent(this, com.jangeby.system.ui.permit.FormPermitActivity::class.java)
                    else -> Intent(this, FormPengajuanActivity::class.java).apply {
                        putExtra("TYPE", type)
                    }
                }
                startActivity(intent)
            }
        }
    }

    private fun loadData(type: String) {
        val service = com.jangeby.system.data.remote.ApiClient.getService(this)
        
        // Clear old data and show loading
        adapter.updateData(emptyList())
        binding.tvEmpty.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launchWhenStarted {
            try {
                val resultList = mutableListOf<com.jangeby.system.data.remote.UnifiedPengajuan>()
                when (type) {
                    "Cuti" -> {
                        val res = service.myLeaveRequests()
                        if (res.success && res.data != null) {
                            res.data.data?.forEach {
                                resultList.add(
                                    com.jangeby.system.data.remote.UnifiedPengajuan(
                                        id = it.id,
                                        type = "Cuti",
                                        title = it.leaveType?.name ?: "Cuti",
                                        date = "${it.startDate} - ${it.endDate}",
                                        reason = it.reason ?: "-",
                                        status = it.status ?: "pending",
                                        approval = it.approval
                                    )
                                )
                            }
                        }
                    }
                    "Sakit" -> {
                        val res = service.mySickLeaveRequests()
                        if (res.success && res.data != null) {
                            res.data.data?.forEach {
                                resultList.add(
                                    com.jangeby.system.data.remote.UnifiedPengajuan(
                                        id = it.id,
                                        type = "Sakit",
                                        title = "Sakit",
                                        date = "${it.startDate} - ${it.endDate}",
                                        reason = it.reason ?: "-",
                                        status = it.status ?: "pending",
                                        approval = it.approval
                                    )
                                )
                            }
                        }
                    }
                    "Ijin" -> {
                        val res = service.getPermitHistory()
                        if (res.success && res.data != null) {
                            res.data.data?.forEach {
                                resultList.add(
                                    com.jangeby.system.data.remote.UnifiedPengajuan(
                                        id = it.id,
                                        type = "Ijin",
                                        title = "Ijin",
                                        date = "${it.date}, ${it.startTime?.take(5)} - ${it.endTime?.take(5)}",
                                        reason = it.reason ?: "-",
                                        status = it.status ?: "pending",
                                        approval = it.approval,
                                        attachmentUrl = it.attachment
                                    )
                                )
                            }
                        }
                    }
                    "Absen" -> {
                        val res = service.myLateRequests() // Assuming backend handles this endpoint
                        if (res.success && res.data != null) {
                            res.data.data?.forEach {
                                resultList.add(
                                    com.jangeby.system.data.remote.UnifiedPengajuan(
                                        id = it.id,
                                        type = "Koreksi Absen",
                                        title = "Koreksi Absen",
                                        date = it.date ?: "-",
                                        reason = it.reason ?: "-",
                                        status = it.status ?: "pending",
                                        approval = it.approval
                                    )
                                )
                            }
                        }
                    }
                    "Tukar Hari" -> {
                        val res = service.getWorkdaySwapRequests() // we need this in ApiService
                        if (res.success && res.data != null) {
                            res.data.data?.forEach {
                                resultList.add(
                                    com.jangeby.system.data.remote.UnifiedPengajuan(
                                        id = it.id,
                                        type = "Tukar Hari",
                                        title = "Tukar Hari",
                                        date = "${it.workDate} -> ${it.swapToDate}",
                                        reason = it.reason ?: "-",
                                        status = it.status ?: "pending",
                                        approval = it.approval
                                    )
                                )
                            }
                        }
                    }
                }
                
                binding.progressBar.visibility = View.GONE
                if (resultList.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }
                adapter.updateData(resultList)
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                android.widget.Toast.makeText(this@PengajuanActivity, "Gagal memuat data: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
