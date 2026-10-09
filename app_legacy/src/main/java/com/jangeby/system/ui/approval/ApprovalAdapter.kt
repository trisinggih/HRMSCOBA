package com.jangeby.system.ui.approval

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.util.Log
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApprovalItem
import com.jangeby.system.databinding.ItemApprovalBinding

class ApprovalAdapter(
    private var items: List<ApprovalItem>,
    private val onApprove: (ApprovalItem) -> Unit,
    private val onReject: (ApprovalItem) -> Unit,
    private val onTrack: (ApprovalItem) -> Unit
) : RecyclerView.Adapter<ApprovalAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemApprovalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ApprovalItem) {
            // Requester Info
            binding.tvRequesterName.text = item.requester?.name ?: "Karyawan"
            val photoUrl = item.requester?.photo?.let { "http://192.168.10.6:8011/storage/$it" }
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(photoUrl)
                    .transform(CircleCrop())
                    .into(binding.ivRequesterPhoto)
            } else {
                binding.ivRequesterPhoto.setImageResource(R.mipmap.ic_launcher_round)
            }

            // Date
            
            try {
                if (!item.createdAt.isNullOrEmpty()) {
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                    val dateObj = parser.parse(item.createdAt)
                    
                    if (dateObj != null) {
                        val formatter = SimpleDateFormat("d-MMM-yyyy HH:mm", Locale.getDefault())
                        binding.tvDate.text = formatter.format(dateObj)
                    } else {
                        binding.tvDate.text = item.createdAt
                    }
                } else {
                    binding.tvDate.text = "-"
                }
            } catch (e: Exception) {
                // Try fallback parser without microseconds
                try {
                    val parser2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    parser2.timeZone = TimeZone.getTimeZone("UTC")
                    val dateObj2 = parser2.parse(item.createdAt)
                    if (dateObj2 != null) {
                        val formatter2 = SimpleDateFormat("d-MMM-yyyy HH:mm", Locale.getDefault())
                        binding.tvDate.text = formatter2.format(dateObj2)
                    } else {
                        binding.tvDate.text = item.createdAt
                    }
                } catch (e2: Exception) {
                    binding.tvDate.text = item.createdAt ?: "-"
                }
            }

            
            // Module mapping
            val moduleLabel = when (item.module?.lowercase()) {
                "leave" -> "Cuti"
                "permission" -> "Izin"
                "permit" -> "Izin"
                "overtime" -> "Lembur"
                "attendance_correction" -> "Koreksi Absen"
                "shift_exchange" -> "Tukar Shift"
                "workday_swap" -> "Tukar Hari"
                "loan_request" -> "Kasbon"
                "reimbursement" -> "Klaim/Reimburse"
                else -> item.module?.replaceFirstChar { it.uppercase() }?.replace("_", " ")
            }
            binding.tvModule.text = "Modul: $moduleLabel"
            
            // Payload parsing (reason & attachment)
            val p = item.payload
            val infoBuilder = StringBuilder()
            
            // For Leave / Cuti
            val leaveType = p?.get("leave_type")?.toString()
            if (!leaveType.isNullOrEmpty()) {
                infoBuilder.append("Cuti: $leaveType\n")
                val sd = p["start_date"]?.toString()?.take(10)
                val ed = p["end_date"]?.toString()?.take(10)
                val days = p["total_days"]?.toString()
                if (sd != null && ed != null) {
                    infoBuilder.append("Tanggal: $sd s/d $ed ($days Hari)\n")
                }
            }

            // For Loan / Kasbon / Reimburse
            val amount = p?.get("amount")?.toString()
            val type = p?.get("type")?.toString()
            
            if (!type.isNullOrEmpty()) {
                infoBuilder.append("Tipe: $type\n")
            }
            if (!amount.isNullOrEmpty()) {
                infoBuilder.append("Nominal: Rp $amount\n")
            }
            
            // For Tukar Hari
            val workDate = p?.get("work_date")?.toString()
            val swapToDate = p?.get("swap_to_date")?.toString()
            if (!workDate.isNullOrEmpty() && !swapToDate.isNullOrEmpty()) {
                infoBuilder.append("Tukar: $workDate -> $swapToDate\n")
            }
            
            val reason = (p?.get("reason") ?: p?.get("remark") ?: p?.get("note") ?: p?.get("description"))?.toString()
            if (!reason.isNullOrEmpty()) {
                infoBuilder.append("Keterangan: $reason")
            }
            
            val finalInfo = infoBuilder.toString().trim()
            if (finalInfo.isNotEmpty()) {
                binding.tvReason.visibility = View.VISIBLE
                binding.tvReason.text = finalInfo
            } else {
                binding.tvReason.visibility = View.GONE
            }

            if (!item.attachmentUrl.isNullOrEmpty()) {
                binding.ivAttachment.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(item.attachmentUrl)
                    .into(binding.ivAttachment)
            } else {
                binding.ivAttachment.visibility = View.GONE
            }

            // Status Badge
            val statusStr = item.status?.replaceFirstChar { it.uppercase() } ?: "Pending"
            binding.tvStatus.text = statusStr
            
            when (statusStr.lowercase()) {
                "approved" -> {
                    binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                }
                "rejected" -> {
                    binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#C62828"))
                }
                "cancelled" -> {
                    binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#ECEFF1"))
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#546E7A"))
                }
                else -> {
                    binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#E65100"))
                }
            }
            
            // Action Buttons
            if (item.status == "pending" || item.status == "in_review") {
                binding.layoutActionButtons.visibility = View.VISIBLE
                binding.btnApprove.setOnClickListener { onApprove(item) }
                binding.btnReject.setOnClickListener { onReject(item) }
            } else {
                binding.layoutActionButtons.visibility = View.GONE
            }

            // Options Menu (Lacak Persetujuan)
            binding.btnOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, "Detail Approval")
                popup.setOnMenuItemClickListener { menuItem ->
                    if (menuItem.itemId == 1) {
                        onTrack(item)
                        true
                    } else {
                        false
                    }
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApprovalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
    
    fun updateData(newItems: List<ApprovalItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
