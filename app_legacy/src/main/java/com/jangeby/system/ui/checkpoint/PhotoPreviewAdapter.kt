package com.jangeby.system.ui.checkpoint

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import java.io.File

class PhotoPreviewAdapter(
    private val photos: MutableList<File>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<PhotoPreviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = photos[position]
        
        // In a real app we'd use Glide to load the file, but we can decode bitmap here
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        holder.ivPhoto.setImageBitmap(bitmap)

        holder.btnRemove.setOnClickListener {
            onRemove(position)
        }
    }

    override fun getItemCount(): Int = photos.size
}
