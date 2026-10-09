package com.jangeby.system.ui.announcement

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.AnnouncementData
import com.jangeby.system.ui.UiHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnouncementAdapter(
    private var data: List<AnnouncementData>
) : RecyclerView.Adapter<AnnouncementAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val ivAttachment: ImageView = itemView.findViewById(R.id.ivAttachment)
        val btnAttachment: Button = itemView.findViewById(R.id.btnAttachment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_announcement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.tvTitle.text = item.title
        
        // Parse HTML content
        val htmlContent = item.body ?: ""
        holder.tvBody.text = HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_COMPACT)
        
        holder.tvDate.text = UiHelpers.formatDateTime(item.createdAt)

        holder.ivAttachment.visibility = View.GONE
        holder.btnAttachment.visibility = View.GONE

        if (!item.attachmentUrl.isNullOrEmpty()) {
            if (item.attachmentType == "image") {
                holder.ivAttachment.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(item.attachmentUrl)
                    .into(holder.ivAttachment)
            } else if (item.attachmentType == "pdf") {
                holder.btnAttachment.visibility = View.VISIBLE
                holder.btnAttachment.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(item.attachmentUrl)
                    holder.itemView.context.startActivity(intent)
                }
            }
        }

        if (item.isRead) {
            holder.tvTitle.typeface = android.graphics.Typeface.DEFAULT
            holder.tvTitle.setTextColor(android.graphics.Color.parseColor("#333333"))
        } else {
            holder.tvTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
            holder.tvTitle.setTextColor(android.graphics.Color.BLACK)
        }

        holder.itemView.setOnClickListener {
            if (!item.isRead) {
                item.isRead = true
                notifyItemChanged(position)
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ApiClient.getService(holder.itemView.context).markAnnouncementAsRead(item.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // Open Detail Activity
            val intent = Intent(holder.itemView.context, AnnouncementDetailActivity::class.java).apply {
                putExtra("EXTRA_TITLE", item.title)
                putExtra("EXTRA_BODY", item.body)
                putExtra("EXTRA_DATE", UiHelpers.formatDateTime(item.createdAt))
                putExtra("EXTRA_ATTACHMENT_URL", item.attachmentUrl)
                putExtra("EXTRA_ATTACHMENT_TYPE", item.attachmentType)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = data.size

    fun updateData(newData: List<AnnouncementData>) {
        data = newData
        notifyDataSetChanged()
    }
}
