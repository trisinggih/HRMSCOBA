package com.jangeby.system.ui.approval

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.databinding.ActivityApprovalBinding
import kotlinx.coroutines.launch

class ApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApprovalBinding
    private lateinit var adapter: ApprovalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ApprovalAdapter(emptyList(), { item ->
            approveRequest(item)
        }, { item ->
            askRejectRemark(item.id)
        }, { item ->
            showTrackingDialog(item)
        })

        binding.rvApprovals.layoutManager = LinearLayoutManager(this)
        binding.rvApprovals.adapter = adapter

        fetchApprovals()
    }

    private fun fetchApprovals() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@ApprovalActivity).getApprovals()
                if (response.success && response.data?.data != null) {
                    val list = response.data.data
                    if (list.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        adapter.updateData(emptyList())
                    } else {
                        adapter.updateData(list)
                    }
                } else {
                    Toast.makeText(this@ApprovalActivity, response.message ?: "Failed to load", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ApprovalActivity, "Error loading approvals", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun approveRequest(item: com.jangeby.system.data.remote.ApprovalItem) {
        val id = item.id
        
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(com.jangeby.system.R.layout.layout_bottom_sheet_confirmation, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val tvTitle = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsTitle)
        val tvMessage = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsMessage)
        val llContent = view.findViewById<android.widget.LinearLayout>(com.jangeby.system.R.id.llBsContent)
        val btnPositive = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsPositive)
        val btnNegative = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsNegative)
        
        val isKasbon = item.module?.contains("Loan", ignoreCase = true) == true || item.module?.contains("Kasbon", ignoreCase = true) == true
        
        tvTitle.text = "Persetujuan"
        tvMessage.text = "Masukkan catatan" + if (isKasbon) " dan tenor cicilan." else "."
        btnPositive.text = "Setujui"
        btnNegative.text = "Batal"
        
        val remarkInput = android.widget.EditText(this)
        remarkInput.hint = "Catatan (Opsional)"
        remarkInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        remarkInput.minLines = 3
        remarkInput.gravity = android.view.Gravity.TOP
        remarkInput.setBackgroundResource(com.jangeby.system.R.drawable.bg_edit_text_bordered)
        remarkInput.setPadding(30, 30, 30, 30)
        llContent.addView(remarkInput)
        
        var tenorInput: android.widget.EditText? = null
        if (isKasbon) {
            tenorInput = android.widget.EditText(this)
            tenorInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            tenorInput.hint = "Berapa kali cicilan (bulan)?"
            tenorInput.setBackgroundResource(com.jangeby.system.R.drawable.bg_edit_text_bordered)
            tenorInput.setPadding(30, 30, 30, 30)
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 30
            llContent.addView(tenorInput, params)
        }

        btnPositive.setOnClickListener {
            val body = mutableMapOf<String, String>()
            val note = remarkInput.text.toString()
            if (note.isNotEmpty()) {
                body["note"] = note
            }
            if (isKasbon) {
                val tenor = tenorInput?.text?.toString()?.toIntOrNull() ?: 1
                body["tenor"] = tenor.toString()
            }
            doApprove(id, body)
            bottomSheetDialog.dismiss()
        }
        
        btnNegative.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }

    private fun doApprove(id: Long, body: Map<String, String>) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@ApprovalActivity).approveRequest(id, body)
                if (response.success) {
                    Toast.makeText(this@ApprovalActivity, "Pengajuan disetujui", Toast.LENGTH_SHORT).show()
                    fetchApprovals() // Refresh list
                } else {
                    Toast.makeText(this@ApprovalActivity, response.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ApprovalActivity, "Error", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun askRejectRemark(id: Long) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(com.jangeby.system.R.layout.layout_bottom_sheet_confirmation, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val tvTitle = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsTitle)
        val tvMessage = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsMessage)
        val llContent = view.findViewById<android.widget.LinearLayout>(com.jangeby.system.R.id.llBsContent)
        val btnPositive = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsPositive)
        val btnNegative = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsNegative)
        
        tvTitle.text = "Tolak Pengajuan"
        tvMessage.text = "Tambahkan alasan kenapa pengajuan ini ditolak."
        btnPositive.text = "Tolak"
        btnNegative.text = "Batal"
        
        val input = android.widget.EditText(this)
        input.hint = "Alasan penolakan (Opsional)"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.minLines = 3
        input.gravity = android.view.Gravity.TOP
        input.setBackgroundResource(com.jangeby.system.R.drawable.bg_edit_text_bordered)
        input.setPadding(30, 30, 30, 30)
        llContent.addView(input, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        
        btnPositive.setOnClickListener {
            val note = input.text.toString()
            val body = if (note.isNotEmpty()) mapOf("note" to note) else emptyMap()
            doReject(id, body)
            bottomSheetDialog.dismiss()
        }
        
        btnNegative.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }

    private fun doReject(id: Long, body: Map<String, String>) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@ApprovalActivity).rejectRequest(id, body)
                if (response.success) {
                    Toast.makeText(this@ApprovalActivity, "Pengajuan ditolak", Toast.LENGTH_SHORT).show()
                    fetchApprovals() // Refresh list
                } else {
                    Toast.makeText(this@ApprovalActivity, response.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ApprovalActivity, "Error", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

        private fun showTrackingDialog(item: com.jangeby.system.data.remote.ApprovalItem) {
        val bottomSheetDialog = BottomSheetDialog(this) // Default theme, can be changed if needed
        val view = LayoutInflater.from(this).inflate(com.jangeby.system.R.layout.layout_bottom_sheet_tracking, null)
        bottomSheetDialog.setContentView(view)
        
        // Transparent background needed for curved corners
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)

        val rvTracking = view.findViewById<RecyclerView>(com.jangeby.system.R.id.rvTracking)
        rvTracking.layoutManager = LinearLayoutManager(this)
        
        // Fetch tracking data from server
        val service = com.jangeby.system.data.remote.ApiClient.getService(this)
        lifecycleScope.launchWhenStarted {
            try {
                val res = service.getApprovalRequest(item.id)
                if (res.success && res.data != null) {
                    val steps = res.data.workflow?.steps ?: emptyList()
                    val logs = res.data.logs ?: emptyList()
                    val adapter = TrackingAdapter(steps, logs)
                    rvTracking.adapter = adapter
                } else {
                    android.widget.Toast.makeText(this@ApprovalActivity, "Gagal memuat tracking: ${res.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ApprovalActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        bottomSheetDialog.show()
    }
}
