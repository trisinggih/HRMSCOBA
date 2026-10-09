package com.jangeby.system.ui.personal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.EducationModel

class EducationAdapter(
    private var items: List<EducationModel>,
    private val onEditClick: (EducationModel) -> Unit,
    private val onDeleteClick: (EducationModel) -> Unit
) : RecyclerView.Adapter<EducationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInstitution: TextView = view.findViewById(R.id.tvInstitution)
        val tvDegree: TextView = view.findViewById(R.id.tvDegree)
        val tvMajor: TextView = view.findViewById(R.id.tvMajor)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val btnOptions: ImageView = view.findViewById(R.id.btnOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_education, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvInstitution.text = item.institution_name
        holder.tvDegree.text = item.level
        holder.tvMajor.text = item.major ?: "-"
        holder.tvYear.text = item.end_year?.toString() ?: "-"
        
        holder.btnOptions.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(view.context, view)
            popup.inflate(R.menu.menu_item_options)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEditClick(item)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<EducationModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
