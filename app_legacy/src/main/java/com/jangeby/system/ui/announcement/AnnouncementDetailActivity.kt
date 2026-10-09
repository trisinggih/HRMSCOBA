package com.jangeby.system.ui.announcement

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.jangeby.system.R

class AnnouncementDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcement_detail)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val title = intent.getStringExtra("EXTRA_TITLE") ?: ""
        val body = intent.getStringExtra("EXTRA_BODY") ?: ""
        val date = intent.getStringExtra("EXTRA_DATE") ?: ""
        val attachmentUrl = intent.getStringExtra("EXTRA_ATTACHMENT_URL")
        val attachmentType = intent.getStringExtra("EXTRA_ATTACHMENT_TYPE")

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvBody = findViewById<TextView>(R.id.tvBody)
        val ivAttachment = findViewById<ImageView>(R.id.ivAttachment)
        val btnAttachment = findViewById<Button>(R.id.btnAttachment)

        tvTitle.text = title
        tvDate.text = date
        tvBody.text = HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_COMPACT)

        if (!attachmentUrl.isNullOrEmpty()) {
            if (attachmentType == "image") {
                ivAttachment.visibility = View.VISIBLE
                Glide.with(this)
                    .load(attachmentUrl)
                    .into(ivAttachment)
            } else if (attachmentType == "pdf") {
                btnAttachment.visibility = View.VISIBLE
                btnAttachment.setOnClickListener {
                    val viewIntent = Intent(Intent.ACTION_VIEW)
                    viewIntent.data = Uri.parse(attachmentUrl)
                    startActivity(viewIntent)
                }
            }
        }
    }
}
