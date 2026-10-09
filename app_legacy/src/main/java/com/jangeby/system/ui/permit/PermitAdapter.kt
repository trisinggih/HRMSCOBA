package com.jangeby.system.ui.permit

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.model.PermitRequest
import java.text.SimpleDateFormat
import java.util.Locale

class PermitAdapter(private val items: List<PermitRequest>) :
    RecyclerView.Adapter<PermitAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvReason: TextView = view.findViewById(R.id.tvReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_permit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val date = parser.parse(item.date)
            holder.tvDate.text = if (date != null) formatter.format(date) else item.date
        } catch (e: Exception) {
            holder.tvDate.text = item.date
        }

        holder.tvType.text = if (item.type == "personal") "Keperluan Pribadi" else if (item.type == "business") "Dinas Kantor" else item.type
        val startTimeStr = if (item.start_time.length >= 5) item.start_time.substring(0, 5) else item.start_time
        val endTimeStr = if (item.end_time.length >= 5) item.end_time.substring(0, 5) else item.end_time
        holder.tvTime.text = "$startTimeStr - $endTimeStr"
        holder.tvReason.text = item.reason

        holder.tvStatus.text = item.status.uppercase()
        when (item.status) {
            "approved" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_success)
            }
            "rejected" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge)
            }
            else -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge)
            }
        }
    }

    override fun getItemCount() = items.size
}
