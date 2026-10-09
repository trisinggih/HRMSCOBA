package com.jangeby.system.ui.personal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.FamilyModel

class FamilyAdapter(
    private var items: List<FamilyModel>,
    private val onEditClick: (FamilyModel) -> Unit,
    private val onDeleteClick: (FamilyModel) -> Unit
) : RecyclerView.Adapter<FamilyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvRelation: TextView = view.findViewById(R.id.tvRelation)
        val tvPhone: TextView = view.findViewById(R.id.tvPhone)
        val btnOptions: ImageView = view.findViewById(R.id.btnOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_family, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        
        val relationStr = when (item.relation) {
            "spouse" -> "Suami/Istri"
            "child" -> "Anak"
            "parent" -> "Orang Tua"
            else -> "Lainnya"
        }
        
        holder.tvRelation.text = relationStr
        holder.tvPhone.text = item.phone ?: "-"
        
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

    fun updateData(newItems: List<FamilyModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
