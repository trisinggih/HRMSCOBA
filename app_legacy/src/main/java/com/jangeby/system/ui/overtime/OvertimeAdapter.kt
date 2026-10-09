package com.jangeby.system.ui.overtime

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.data.remote.OvertimeRequestItem
import com.jangeby.system.databinding.ItemOvertimeBinding
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class OvertimeAdapter(
    private val onDetailClicked: (OvertimeRequestItem) -> Unit,
    private val onCancelClicked: (OvertimeRequestItem) -> Unit
) : RecyclerView.Adapter<OvertimeAdapter.ViewHolder>() {

    private var items = listOf<OvertimeRequestItem>()

    fun submitList(list: List<OvertimeRequestItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemOvertimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.binding.root.context
        
        // Info User
        val prefs = com.jangeby.system.data.local.SecurePrefs(context)
        holder.binding.tvName.text = prefs.employeeName ?: "Karyawan"
        val photoUrl = prefs.employeePhotoUrl
        if (!photoUrl.isNullOrBlank()) {
            com.bumptech.glide.Glide.with(context)
                .load(photoUrl)
                .placeholder(com.jangeby.system.R.drawable.ic_person)
                .error(com.jangeby.system.R.drawable.ic_person)
                .into(holder.binding.ivIcon)
        }
        
        // Format Date
        try {
            val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val outFmt = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
            val d = inFmt.parse(item.date)
            holder.binding.tvDate.text = if (d != null) outFmt.format(d) else item.date
        } catch (e: Exception) {
            holder.binding.tvDate.text = item.date
        }

        holder.binding.tvReason.text = item.reason ?: "-"
        val start = item.requested_start_time?.take(5) ?: "-"
        val end = item.requested_end_time?.take(5) ?: "-"
        holder.binding.tvTime.text = "$start - $end"

        val statusStr = item.status.lowercase()
        holder.binding.tvStatus.text = statusStr.uppercase()
        when (statusStr) {
            "approved" -> {
                holder.binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            }
            "rejected" -> {
                holder.binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#C62828"))
            }
            "cancelled" -> {
                holder.binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#ECEFF1"))
                holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#546E7A"))
            }
            else -> {
                holder.binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
                holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F57C00"))
            }
        }
        
        holder.binding.btnOptions.setOnClickListener { view ->
            val popup = androidx.appcompat.widget.PopupMenu(context, view)
            popup.menu.add(0, 1, 0, "Detail")
            if (statusStr == "pending") {
                popup.menu.add(0, 2, 1, "Batalkan")
            }
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> {
                        onDetailClicked(item)
                        true
                    }
                    2 -> {
                        onCancelClicked(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(val binding: ItemOvertimeBinding) : RecyclerView.ViewHolder(binding.root)
}
