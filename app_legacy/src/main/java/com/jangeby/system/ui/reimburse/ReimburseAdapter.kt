package com.jangeby.system.ui.reimburse

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.ReimburseItem
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class ReimburseAdapter(
    private val onClick: (ReimburseItem) -> Unit
) : ListAdapter<ReimburseItem, ReimburseAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val cardStatus: MaterialCardView = itemView.findViewById(R.id.cardStatus)
        val tvType: TextView = itemView.findViewById(R.id.tvType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)

        fun bind(item: ReimburseItem) {
            tvDate.text = item.date ?: "-"
            tvType.text = "Type: ${item.type ?: "-"}"
            tvDescription.text = item.description ?: "-"
            
            val amountStr = item.amount?.let {
                val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                format.format(it)
            } ?: "Rp 0"
            tvAmount.text = amountStr

            val statusStr = item.status?.replaceFirstChar { it.uppercase() } ?: "Pending"
            tvStatus.text = statusStr

            when (statusStr.lowercase()) {
                "approved" -> {
                    cardStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                    tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                }
                "rejected" -> {
                    cardStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                    tvStatus.setTextColor(Color.parseColor("#C62828"))
                }
                "cancelled" -> {
                    cardStatus.setCardBackgroundColor(Color.parseColor("#ECEFF1"))
                    tvStatus.setTextColor(Color.parseColor("#546E7A"))
                }
                else -> {
                    cardStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                    tvStatus.setTextColor(Color.parseColor("#E65100"))
                }
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reimburse, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<ReimburseItem>() {
        override fun areItemsTheSame(oldItem: ReimburseItem, newItem: ReimburseItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReimburseItem, newItem: ReimburseItem): Boolean {
            return oldItem == newItem
        }
    }
}
