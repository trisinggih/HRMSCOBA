package com.jangeby.system.ui.leave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.LeaveRequestItem

/** Adapter daftar permohonan cuti. */
class LeaveRequestAdapter(
    private var items: List<LeaveRequestItem>
) : RecyclerView.Adapter<LeaveRequestAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLeaveType: TextView = view.findViewById(R.id.tvLeaveType)
        val tvLeaveStatus: TextView = view.findViewById(R.id.tvLeaveStatus)
        val tvLeaveDate: TextView = view.findViewById(R.id.tvLeaveDate)
        val tvLeaveReason: TextView = view.findViewById(R.id.tvLeaveReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leave_row, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.tvLeaveType.text = item.leaveType?.name ?: "Cuti"
        holder.tvLeaveStatus.text = "Status: ${item.status ?: "-"}"
        holder.tvLeaveDate.text = "${item.startDate ?: "-"} - ${item.endDate ?: "-"} (${item.totalDays ?: 0} hari)"
        holder.tvLeaveReason.text = "Alasan: ${item.reason ?: "-"}"
    }
}
