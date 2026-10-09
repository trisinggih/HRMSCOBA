package com.jangeby.system.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.NotificationItem

/** Adapter daftar notifikasi; dukung tap item untuk tandai dibaca. */
class NotificationAdapter(
    private var items: List<NotificationItem>,
    private val onClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvBody: TextView = view.findViewById(R.id.tvBody)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val unreadDot: View = view.findViewById(R.id.unreadDot)
    }

    fun update(newItems: List<NotificationItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title ?: "Notifikasi"
        holder.tvBody.text = item.body ?: ""
        holder.tvTime.text = item.createdAt?.substring(0, 10) ?: ""
        holder.unreadDot.visibility = if (item.isRead == true) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
