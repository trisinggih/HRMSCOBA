package com.jangeby.system.ui.payslip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.model.PayrollRun
import java.text.NumberFormat
import java.util.Locale

class PayslipAdapter(
    private var items: List<PayrollRun>,
    private val onClick: (PayrollRun) -> Unit
) : RecyclerView.Adapter<PayslipAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    fun updateData(newItems: List<PayrollRun>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payslip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvPeriod.text = item.period?.name ?: "Periode ${item.payroll_period_id}"
        holder.tvNetSalary.text = currencyFormat.format(item.net_salary)
        
        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPeriod: TextView = view.findViewById(R.id.tvPeriod)
        val tvNetSalary: TextView = view.findViewById(R.id.tvNetSalary)
    }
}
