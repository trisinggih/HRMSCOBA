package com.jangeby.system.ui.info

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.EmployeeProfile
import kotlinx.coroutines.launch

class InfoActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvPosition: TextView
    private lateinit var layoutEducation: LinearLayout
    private lateinit var layoutTraining: LinearLayout
    private lateinit var layoutDocument: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvName = findViewById(R.id.tvName)
        tvPosition = findViewById(R.id.tvPosition)
        layoutEducation = findViewById(R.id.layoutEducation)
        layoutTraining = findViewById(R.id.layoutTraining)
        layoutDocument = findViewById(R.id.layoutDocument)
        progressBar = findViewById(R.id.progressBar)
        
        // val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        // You can load image using Glide if needed

        fetchProfile()
    }

    private fun fetchProfile() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@InfoActivity).getProfile()
                if (res.success && res.data != null) {
                    populateData(res.data)
                } else {
                    Toast.makeText(this@InfoActivity, res.message ?: "Gagal memuat profil", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InfoActivity, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun populateData(profile: EmployeeProfile) {
        tvName.text = profile.name ?: "-"
        val position = listOf(profile.position, profile.branch).filter { !it.isNullOrBlank() }.joinToString(" - ")
        tvPosition.text = if (position.isBlank()) "-" else position

        // Populate Educations
        layoutEducation.removeAllViews()
        if (profile.educations.isNullOrEmpty()) {
            layoutEducation.addView(createEmptyTextView("Tidak ada riwayat pendidikan"))
        } else {
            profile.educations.forEach { edu ->
                val text = "${edu.institutionName ?: "-"} (${edu.startYear ?: "-"} - ${edu.endYear ?: "-"})\n${edu.degree ?: "-"} - ${edu.major ?: "-"}"
                layoutEducation.addView(createItemTextView(text))
            }
        }

        // Populate Trainings
        layoutTraining.removeAllViews()
        if (profile.trainings.isNullOrEmpty()) {
            layoutTraining.addView(createEmptyTextView("Tidak ada riwayat pelatihan"))
        } else {
            profile.trainings.forEach { tr ->
                val text = "${tr.trainingName ?: "-"} (${tr.year ?: "-"})\n${tr.institution ?: "-"}"
                layoutTraining.addView(createItemTextView(text))
            }
        }

        // Populate Documents
        layoutDocument.removeAllViews()
        if (profile.documents.isNullOrEmpty()) {
            layoutDocument.addView(createEmptyTextView("Tidak ada dokumen terlampir"))
        } else {
            profile.documents.forEach { doc ->
                val text = "${doc.documentType ?: "-"}: ${doc.documentName ?: "-"}"
                layoutDocument.addView(createItemTextView(text))
            }
        }
    }

    private fun createEmptyTextView(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(resources.getColor(R.color.text_grey, null))
        tv.textSize = 14f
        tv.setPadding(0, 8, 0, 8)
        return tv
    }

    private fun createItemTextView(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(resources.getColor(R.color.black, null))
        tv.textSize = 14f
        tv.setPadding(0, 8, 0, 16)
        return tv
    }
}
