package com.jangeby.system.ui.kasbon

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.LoanRequestItem
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class KasbonAdapter : ListAdapter<LoanRequestItem, KasbonAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val cardStatus: MaterialCardView = itemView.findViewById(R.id.cardStatus)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvInstallment: TextView = itemView.findViewById(R.id.tvInstallment)
        val tvReason: TextView = itemView.findViewById(R.id.tvReason)

        fun bind(item: LoanRequestItem) {
            try {
                val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val sdfOut = java.text.SimpleDateFormat("d-M-yyyy", java.util.Locale.getDefault())
                val parsedDate = sdfIn.parse(item.date)
                tvDate.text = if (parsedDate != null) sdfOut.format(parsedDate) else item.date
            } catch (e: Exception) {
                tvDate.text = item.date ?: "-"
            }

            tvReason.text = item.reason ?: "-"
            
            val statusStr = item.status.replaceFirstChar { it.uppercase() }
            if (item.installmentMonths != null && item.installmentMonths > 0 && statusStr.lowercase() != "pending") {
                tvInstallment.text = "Tenor: ${item.installmentMonths} Bulan"
                tvInstallment.visibility = View.VISIBLE
            } else {
                tvInstallment.visibility = View.GONE
            }
            
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            tvAmount.text = format.format(item.amount)

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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kasbon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<LoanRequestItem>() {
        override fun areItemsTheSame(oldItem: LoanRequestItem, newItem: LoanRequestItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LoanRequestItem, newItem: LoanRequestItem): Boolean {
            return oldItem == newItem
        }
    }
}
