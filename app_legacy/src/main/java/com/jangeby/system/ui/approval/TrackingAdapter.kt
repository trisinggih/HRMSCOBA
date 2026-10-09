package com.jangeby.system.ui.approval

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.data.remote.ApprovalStep
import com.jangeby.system.data.remote.ApprovalLog
import com.jangeby.system.databinding.ItemTrackingStepBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TrackingAdapter(
    private val steps: List<ApprovalStep>,
    private val logs: List<ApprovalLog>
) : RecyclerView.Adapter<TrackingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrackingStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = steps[position]
        
        // Find corresponding log if any (backend might save step.order instead of step.id in old data)
        // Use findLast to get the latest action (e.g. approve instead of escalate)
        val log = logs.findLast { it.stepId == step.id || (step.order != null && it.stepId == step.order.toLong()) }
        
        // UI logic
        val isFirst = position == 0
        val isLast = position == steps.size - 1
        
        holder.bind(step, log, isFirst, isLast)
    }

    override fun getItemCount(): Int = steps.size

    class ViewHolder(private val binding: ItemTrackingStepBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(step: ApprovalStep, log: ApprovalLog?, isFirst: Boolean, isLast: Boolean) {
            // Line visibility
            binding.vLineTop.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
            binding.vLineBottom.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
            
            // Step Name
            val expectedName = step.approverName ?: step.expectedApproverName ?: step.approverType
            binding.tvStepName.text = "Tahap ${step.order}: $expectedName"
            
            // Status & Coloring
            val statusStr = log?.action ?: "PENDING"
            binding.tvStatus.text = statusStr.uppercase()
            
            when (statusStr.lowercase()) {
                "approve", "approved" -> {
                    binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                    binding.vDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                }
                "reject", "rejected" -> {
                    binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
                    binding.vDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
                }
                "cancel", "cancelled" -> {
                    binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                    binding.vDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                }
                else -> {
                    binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
                    binding.vDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
                }
            }
            
            // Actor
            if (log?.approverName != null) {
                binding.tvActionBy.text = "Aktor: " + log.approverName
            } else {
                binding.tvActionBy.text = "Menunggu: $expectedName"
            }
            
            // Date formatting
            val createdAt = log?.createdAt
            if (!createdAt.isNullOrEmpty()) {
                try {
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                    val dateObj = parser.parse(createdAt)
                    if (dateObj != null) {
                        val formatter = SimpleDateFormat("d-MMM-yyyy HH:mm", Locale.getDefault())
                        binding.tvDate.text = "Tanggal: " + formatter.format(dateObj)
                    } else {
                        binding.tvDate.text = "Tanggal: " + createdAt
                    }
                } catch (e: Exception) {
                    try {
                        val p2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                        p2.timeZone = TimeZone.getTimeZone("UTC")
                        val d2 = p2.parse(createdAt)
                        if (d2 != null) {
                            val f2 = SimpleDateFormat("d-MMM-yyyy HH:mm", Locale.getDefault())
                            binding.tvDate.text = "Tanggal: " + f2.format(d2)
                        } else {
                            binding.tvDate.text = "Tanggal: " + createdAt
                        }
                    } catch (e2: Exception) {
                        binding.tvDate.text = "Tanggal: " + createdAt
                    }
                }
            } else {
                binding.tvDate.text = "Tanggal: -"
            }
            
            // Notes
            val note = log?.note
            if (!note.isNullOrEmpty()) {
                binding.tvNote.visibility = View.VISIBLE
                binding.tvNote.text = "Catatan: $note"
            } else {
                binding.tvNote.visibility = View.GONE
            }
        }
    }
}
