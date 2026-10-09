package com.jangeby.system.ui.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.TaskItem

/** Adapter daftar tugas saya. */
class TaskAdapter(
    private var items: List<TaskItem>,
    private val onItemClick: (TaskItem) -> Unit
) : RecyclerView.Adapter<TaskAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val cardStatus: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardStatus)
    }

    fun update(newItems: List<TaskItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_daily, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.tvNumber.text = "${position + 1}."
        holder.tvTitle.text = item.title ?: "Tugas"
        
        val statusLabel = when(item.status?.lowercase()) {
            "todo" -> "TUNGGU"
            "in_progress" -> "PROSES"
            "done" -> "SELESAI"
            else -> item.status?.uppercase() ?: "PROSES"
        }
        
        holder.tvStatus.text = statusLabel
        
        // Color adjustments
        when(statusLabel) {
            "SELESAI" -> {
                holder.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            }
            "TUNGGU" -> {
                holder.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#F57C00"))
            }
            else -> { // PROSES
                holder.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#1976D2"))
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
}
