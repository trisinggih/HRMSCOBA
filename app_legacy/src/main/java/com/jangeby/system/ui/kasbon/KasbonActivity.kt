package com.jangeby.system.ui.kasbon

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.ui.UiHelpers
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class KasbonActivity : AppCompatActivity() {

    private lateinit var rvKasbon: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvEmpty: TextView
    private lateinit var pbLoading: View
    
    private val items = mutableListOf<com.jangeby.system.data.remote.UnifiedPengajuan>()
    private lateinit var adapter: com.jangeby.system.ui.pengajuan.PengajuanListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kasbon)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvKasbon = findViewById(R.id.rvKasbon)
        fabAdd = findViewById(R.id.fabAdd)
        tvEmpty = findViewById(R.id.tvEmpty)
        pbLoading = findViewById(R.id.pbLoading)

        adapter = com.jangeby.system.ui.pengajuan.PengajuanListAdapter(items, { item ->
            showTrackingDialog(item)
        }, { item ->
            cancelKasbon(item)
        })

        rvKasbon.layoutManager = LinearLayoutManager(this)
        rvKasbon.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, FormKasbonActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        pbLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rvKasbon.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@KasbonActivity).getLoanRequests()
                val list = response.data?.data ?: emptyList()
                
                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    items.clear()
                    list.forEach {
                        val amountStr = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).format(it.amount)
                        items.add(
                            com.jangeby.system.data.remote.UnifiedPengajuan(
                                id = it.id,
                                type = "Kasbon",
                                title = "Kasbon: $amountStr" + if (it.installmentMonths != null && it.installmentMonths > 0) " (${it.installmentMonths} Bln)" else "",
                                date = it.date ?: "",
                                reason = it.reason ?: "-",
                                status = it.status ?: "pending",
                                approval = it.approval,
                                attachmentUrl = it.attachmentUrl
                            )
                        )
                    }
                    adapter.updateData(items)
                    rvKasbon.visibility = View.VISIBLE
                }
            } catch (t: Throwable) {
                android.widget.Toast.makeText(this@KasbonActivity, UiHelpers.errorMessageFrom(this@KasbonActivity, t), android.widget.Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
            } finally {
                pbLoading.visibility = View.GONE
            }
        }
    }

    private fun showTrackingDialog(item: com.jangeby.system.data.remote.UnifiedPengajuan) {
        val approval = item.approval
        if (approval == null) {
            android.widget.Toast.makeText(this, "Data tracking tidak tersedia", android.widget.Toast.LENGTH_SHORT).show()
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
                    android.widget.Toast.makeText(this@KasbonActivity, "Gagal memuat tracking: ${res.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@KasbonActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        bottomSheetDialog.show()
    }

    private fun cancelKasbon(item: com.jangeby.system.data.remote.UnifiedPengajuan) {
        val approval = item.approval
        if (approval == null) {
            android.widget.Toast.makeText(this, "Tidak dapat membatalkan pengajuan ini", android.widget.Toast.LENGTH_SHORT).show()
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
        
        tvTitle.text = "Batalkan Kasbon"
        tvMessage.text = "Apakah Anda yakin ingin membatalkan kasbon ini?"
        btnPositive.text = "Ya"
        btnNegative.text = "Tidak"
        
        btnPositive.setOnClickListener {
            val service = ApiClient.getService(this)
            lifecycleScope.launch {
                try {
                    val response = service.cancelApproval(approval.id)
                    if (response.success) {
                        android.widget.Toast.makeText(this@KasbonActivity, "Kasbon dibatalkan", android.widget.Toast.LENGTH_SHORT).show()
                        loadData()
                    } else {
                        android.widget.Toast.makeText(this@KasbonActivity, response.message ?: "Gagal membatalkan", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@KasbonActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
