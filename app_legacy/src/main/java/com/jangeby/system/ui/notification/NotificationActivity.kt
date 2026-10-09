package com.jangeby.system.ui.notification

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.NotificationItem
import com.jangeby.system.ui.UiHelpers
import kotlinx.coroutines.launch

/**
 * Layar Notifikasi: daftar + tandai dibaca (tap item).
 * Terhubung ke GET /my/notifications & PUT /my/notifications/{id}/read.
 */
class NotificationActivity : AppCompatActivity() {

    private lateinit var rvList: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progress: View

    private var items: List<NotificationItem> = emptyList()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvList = findViewById(R.id.rvNotifications)
        tvEmpty = findViewById(R.id.tvEmpty)
        progress = findViewById(R.id.progressBar)
        rvList.layoutManager = LinearLayoutManager(this)

        adapter = NotificationAdapter(emptyList()) { item ->
            markRead(item)
        }
        rvList.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@NotificationActivity).myNotifications(limit = 50)
                items = res.data?.data ?: emptyList()
                adapter.update(items)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (t: Throwable) {
                tvEmpty.text = "Gagal memuat notifikasi.\n${UiHelpers.errorMessageFrom(this@NotificationActivity, t)}"
                tvEmpty.visibility = View.VISIBLE
            } finally {
                setLoading(false)
            }
        }
    }

    private fun markRead(item: NotificationItem) {
        if (item.isRead == true) {
            Toast.makeText(this, item.title ?: "Notifikasi", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                ApiClient.getService(this@NotificationActivity).markNotificationRead(item.id)
                items = items.map { if (it.id == item.id) it.copy(isRead = true) else it }
                adapter.update(items)
            } catch (t: Throwable) {
                Toast.makeText(
                    this@NotificationActivity,
                    UiHelpers.errorMessageFrom(this@NotificationActivity, t),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
