package com.jangeby.system.ui.pengajuan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.databinding.ItemPengajuanListBinding

import com.jangeby.system.data.remote.UnifiedPengajuan

class PengajuanListAdapter(
    private var items: List<UnifiedPengajuan>,
    private val onDetailClicked: (UnifiedPengajuan) -> Unit,
    private val onCancelClicked: (UnifiedPengajuan) -> Unit
) : RecyclerView.Adapter<PengajuanListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPengajuanListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UnifiedPengajuan) {
            binding.tvTitle.text = item.title
            
            // Format date to d-M-Y
            try {
                val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val outFmt = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                val d = inFmt.parse(item.date)
                binding.tvDate.text = if (d != null) outFmt.format(d) else item.date
            } catch (e: Exception) {
                binding.tvDate.text = item.date
            }
            
            binding.tvReason.text = item.reason
            binding.tvStatus.text = item.status.uppercase()
            val context = binding.root.context
            
            // Load user photo
            val prefs = com.jangeby.system.data.local.SecurePrefs(context)
            val photoUrl = prefs.employeePhotoUrl
            if (!photoUrl.isNullOrBlank()) {
                com.bumptech.glide.Glide.with(context)
                    .load(photoUrl)
                    .placeholder(com.jangeby.system.R.drawable.ic_person)
                    .error(com.jangeby.system.R.drawable.ic_person)
                    .into(binding.ivIcon)
            }
            
            // Attachment
            val attachmentPath = item.attachmentUrl ?: item.approval?.attachmentUrl
            if (!attachmentPath.isNullOrEmpty()) {
                val baseUrl = com.jangeby.system.data.remote.ApiClient.BASE_URL.replace("api/v1/", "storage/")
                val fullUrl = if (attachmentPath.startsWith("http")) attachmentPath else "$baseUrl$attachmentPath"
                binding.btnAttachment.visibility = android.view.View.VISIBLE
                binding.btnAttachment.setOnClickListener {
                    val dialog = android.app.Dialog(context)
                    dialog.setContentView(com.jangeby.system.R.layout.dialog_image_preview)
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    val ivPreview = dialog.findViewById<android.widget.ImageView>(com.jangeby.system.R.id.ivPreview)
                    val btnClose = dialog.findViewById<android.widget.ImageButton>(com.jangeby.system.R.id.btnClose)
                    
                    com.bumptech.glide.Glide.with(context)
                        .load(fullUrl)
                        .into(ivPreview)
                        
                    btnClose.setOnClickListener { dialog.dismiss() }
                    dialog.show()
                }
            } else {
                binding.btnAttachment.visibility = android.view.View.GONE
            }
            
            binding.tvRejectReason.visibility = android.view.View.GONE
            when (item.status.lowercase()) {
                "approved" -> {
                    binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                }
                "rejected" -> {
                    binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#C62828"))
                    
                    val rejectLog = item.approval?.logs?.findLast { it.action == "reject" }
                    if (rejectLog != null && !rejectLog.note.isNullOrBlank()) {
                        binding.tvRejectReason.visibility = android.view.View.VISIBLE
                        binding.tvRejectReason.text = "Catatan: ${rejectLog.note}"
                    }
                }
                "cancelled" -> {
                    binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#ECEFF1"))
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#546E7A"))
                    
                    val cancelLog = item.approval?.logs?.findLast { it.action == "cancel" }
                    if (cancelLog != null && !cancelLog.note.isNullOrBlank()) {
                        binding.tvRejectReason.visibility = android.view.View.VISIBLE
                        binding.tvRejectReason.text = "Catatan: ${cancelLog.note}"
                    }
                }
                else -> { // pending or in_review
                    binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
                    binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F57C00"))
                }
            }

            binding.btnOptions.setOnClickListener { view ->
                val popup = androidx.appcompat.widget.PopupMenu(context, view)
                popup.menu.add(0, 1, 0, "Detail")
                if (item.status.lowercase() == "pending") {
                    popup.menu.add(0, 2, 1, "Batalkan")
                }
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            onDetailClicked(item)
                            true
                        }
                        2 -> {
                            onCancelClicked(item)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPengajuanListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
    
    fun updateData(newItems: List<UnifiedPengajuan>) {
        items = newItems
        notifyDataSetChanged()
    }
}
