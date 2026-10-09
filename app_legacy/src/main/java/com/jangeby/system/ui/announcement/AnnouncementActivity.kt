package com.jangeby.system.ui.announcement

import android.os.Bundle
import android.view.View

import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import kotlinx.coroutines.launch

class AnnouncementActivity : AppCompatActivity() {

    private lateinit var rvAnnouncements: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: AnnouncementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcement)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        if (toolbar == null) throw RuntimeException("Toolbar is null!")
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvAnnouncements)
        if (rv == null) throw RuntimeException("rvAnnouncements is null!")
        rvAnnouncements = rv
        
        val sr = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        if (sr == null) throw RuntimeException("swipeRefresh is null!")
        swipeRefresh = sr

        adapter = AnnouncementAdapter(emptyList())
        rvAnnouncements.layoutManager = LinearLayoutManager(this)
        rvAnnouncements.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            fetchData()
        }

        fetchData()
    }

    private fun fetchData() {
        swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@AnnouncementActivity).fetchAnnouncements()
                adapter.updateData(res.data?.data ?: emptyList())
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AnnouncementActivity, "Gagal memuat pengumuman", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }
}
