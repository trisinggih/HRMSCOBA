package com.jangeby.system.ui.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.CalendarItem

/** Adapter daftar riwayat absensi. */
class HistoryAdapter(
    private var items: List<CalendarItem>
) : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvShiftTime: TextView = view.findViewById(R.id.tvShiftTime)
        val tvStatusInfo: TextView = view.findViewById(R.id.tvStatusInfo)
        val tvClockIn: TextView = view.findViewById(R.id.tvClockIn)
        val tvClockOut: TextView = view.findViewById(R.id.tvClockOut)
        
        val defaultClockInColor: Int = tvClockIn.currentTextColor
        val defaultStatusColor: Int = tvStatusInfo.currentTextColor
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_row, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        
        // Format date: YYYY-MM-DD -> DD-MMM-YYYY (e.g. 01-Sep-2026)
        val dateParts = item.date?.split("-")
        if (dateParts?.size == 3) {
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthIdx = dateParts[1].toIntOrNull() ?: 0
            val monthStr = if (monthIdx in 1..12) months[monthIdx] else dateParts[1]
            holder.tvDate.text = "${dateParts[2]}-$monthStr-${dateParts[0]}"
        } else {
            holder.tvDate.text = item.date ?: "-"
        }
        
        val shift = item.shift
        holder.tvShiftTime.text = if (shift?.startTime != null && shift.endTime != null) 
            "${shift.startTime.take(5)} - ${shift.endTime.take(5)}" else "-"
            
        val isHoliday = item.isHoliday == true
        holder.tvStatusInfo.text = if (isHoliday) "Libur" else (item.attendance?.status?.replaceFirstChar { it.uppercase() } ?: "Working Day")
        
        holder.tvClockIn.text = item.attendance?.clockIn?.take(5) ?: "-"
        
        // Out info => Shift berakhir : XX:XX
        val tvOutLabel = holder.itemView.findViewById<TextView>(R.id.tvOutLabel)
        if (shift?.endTime != null) {
            tvOutLabel.text = "Shift berakhir: "
            holder.tvClockOut.text = shift.endTime.take(5)
        } else {
            tvOutLabel.text = "Out: "
            holder.tvClockOut.text = item.attendance?.clockOut?.take(5) ?: "-"
        }
        
        // Overtime logic
        val layoutOvertime = holder.itemView.findViewById<View>(R.id.layoutOvertime)
        val tvOvertimeIn = holder.itemView.findViewById<TextView>(R.id.tvOvertimeIn)
        val tvOvertimeOut = holder.itemView.findViewById<TextView>(R.id.tvOvertimeOut)
        
        val otIn = item.attendance?.overtimeIn
        val otOut = item.attendance?.overtimeOut
        if (otIn != null || otOut != null) {
            layoutOvertime.visibility = View.VISIBLE
            tvOvertimeIn.text = otIn?.take(5) ?: "-"
            tvOvertimeOut.text = otOut?.take(5) ?: "-"
        } else {
            layoutOvertime.visibility = View.GONE
        }

        var isLate = (item.attendance?.lateMinutes ?: 0) > 0
        val clockInStr = item.attendance?.clockIn?.take(5)
        val shiftStartStr = item.shift?.startTime?.take(5)
        if (!isLate && clockInStr != null && shiftStartStr != null && clockInStr > shiftStartStr) {
            isLate = true
        }
        
        if (isLate) {
            holder.tvClockIn.setTextColor(android.graphics.Color.parseColor("#FF5252"))
            holder.tvStatusInfo.setTextColor(android.graphics.Color.parseColor("#FF5252"))
            holder.tvStatusInfo.text = "${holder.tvStatusInfo.text} (Telat)"
        } else {
            holder.tvClockIn.setTextColor(holder.defaultClockInColor)
            holder.tvStatusInfo.setTextColor(holder.defaultStatusColor)
        }
    }
}
