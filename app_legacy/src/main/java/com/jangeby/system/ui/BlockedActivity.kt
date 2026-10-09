package com.jangeby.system.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jangeby.system.R
import kotlin.system.exitProcess

class BlockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)

        val tvReason = findViewById<TextView>(R.id.tvReason)
        val btnClose = findViewById<Button>(R.id.btnClose)

        val reason = intent.getStringExtra("EXTRA_REASON") ?: "Aplikasi diblokir karena alasan keamanan."
        tvReason.text = reason

        btnClose.setOnClickListener {
            finishAffinity()
            exitProcess(0)
        }
    }

    override fun onBackPressed() {
        // Kunci tombol back
    }
}
