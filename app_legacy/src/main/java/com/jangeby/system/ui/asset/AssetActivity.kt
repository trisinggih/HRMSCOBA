package com.jangeby.system.ui.asset

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.AssetItem
import kotlinx.coroutines.launch

class AssetActivity : AppCompatActivity() {

    private lateinit var rvAssets: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: AssetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvAssets = findViewById(R.id.rvAssets)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = AssetAdapter()
        rvAssets.layoutManager = LinearLayoutManager(this)
        rvAssets.adapter = adapter

        loadAssets()
    }

    private fun loadAssets() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rvAssets.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@AssetActivity).myAssets()
                progressBar.visibility = View.GONE
                
                val assets = response.data ?: emptyList()
                if (assets.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rvAssets.visibility = View.VISIBLE
                    adapter.submitList(assets)
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AssetActivity, "Gagal memuat aset: ${e.message}", Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Gagal memuat data aset."
            }
        }
    }
}
