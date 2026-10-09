package com.jangeby.system.ui.asset

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.AssetItem

class AssetAdapter : ListAdapter<AssetItem, AssetAdapter.ViewHolder>(AssetDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvAssetName)
        val tvStatus: TextView = view.findViewById(R.id.tvAssetStatus)
        val tvType: TextView = view.findViewById(R.id.tvAssetType)
        val tvSerial: TextView = view.findViewById(R.id.tvAssetSerial)
        val tvNotes: TextView = view.findViewById(R.id.tvAssetNotes)

        fun bind(item: AssetItem) {
            tvName.text = item.name
            tvStatus.text = item.status ?: "-"
            tvType.text = "Jenis: ${item.type ?: "-"}"
            tvSerial.text = "Serial: ${item.serialNumber ?: "-"}"
            tvNotes.text = "Catatan: ${item.notes ?: "-"}"

            when (item.status) {
                "Tersedia" -> tvStatus.setTextColor(itemView.context.getColor(R.color.success))
                "Dipinjamkan" -> tvStatus.setTextColor(itemView.context.getColor(R.color.warning))
                "Pemeliharaan" -> tvStatus.setTextColor(itemView.context.getColor(R.color.danger))
                else -> tvStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_asset, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class AssetDiffCallback : DiffUtil.ItemCallback<AssetItem>() {
    override fun areItemsTheSame(oldItem: AssetItem, newItem: AssetItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AssetItem, newItem: AssetItem): Boolean {
        return oldItem == newItem
    }
}
