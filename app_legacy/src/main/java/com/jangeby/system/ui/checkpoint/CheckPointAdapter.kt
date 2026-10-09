package com.jangeby.system.ui.checkpoint

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.CheckPointItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CheckPointAdapter(private val items: List<CheckPointItem>) :
    RecyclerView.Adapter<CheckPointAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvPurpose: TextView = view.findViewById(R.id.tvPurpose)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val tvPhotoCount: TextView = view.findViewById(R.id.tvPhotoCount)
        val tvEmployeeName: TextView = view.findViewById(R.id.tvEmployeeName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_check_point, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvClientName.text = item.clientName ?: "-"
        holder.tvPurpose.text = "Tujuan: ${item.purpose ?: "-"}"
        holder.tvAddress.text = item.address ?: "${item.latitude}, ${item.longitude}"
        
        if (item.employee != null && !item.employee.name.isNullOrEmpty()) {
            holder.tvEmployeeName.text = "Oleh: ${item.employee.name}"
            holder.tvEmployeeName.visibility = View.VISIBLE
        } else {
            holder.tvEmployeeName.visibility = View.GONE
        }

        val photoCount = item.photos?.size ?: if (!item.photoPath.isNullOrEmpty()) 1 else 0
        if (photoCount > 1) {
            holder.tvPhotoCount.visibility = View.VISIBLE
            holder.tvPhotoCount.text = "+${photoCount - 1}"
        } else {
            holder.tvPhotoCount.visibility = View.GONE
        }

        // Format date
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val dateObj = parser.parse(item.createdAt ?: "")
            if (dateObj != null) {
                val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                holder.tvDate.text = formatter.format(dateObj)
            } else {
                holder.tvDate.text = item.createdAt
            }
        } catch (e: Exception) {
            holder.tvDate.text = item.createdAt
        }
        
        val photoPath = item.photos?.firstOrNull() ?: item.photoPath
        if (!photoPath.isNullOrEmpty()) {
            val baseUrl = com.jangeby.system.data.remote.ApiClient.BASE_URL.replace("api/v1/", "storage/")
            val fullUrl = if (photoPath.startsWith("http")) photoPath else "$baseUrl$photoPath"
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(fullUrl)
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_camera)
        }
    }

    override fun getItemCount() = items.size
}
