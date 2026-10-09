package com.jangeby.system.ui.checkpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.CheckPointItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class CheckPointActivity : AppCompatActivity() {

    private lateinit var rvCheckPoints: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddCheckPoint: FloatingActionButton

    private val checkPoints = mutableListOf<CheckPointItem>()
    private lateinit var adapter: CheckPointAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_point)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvCheckPoints = findViewById(R.id.rvCheckPoints)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        progressBar = findViewById(R.id.progressBar)
        fabAddCheckPoint = findViewById(R.id.fabAddCheckPoint)

        adapter = CheckPointAdapter(checkPoints)
        rvCheckPoints.layoutManager = LinearLayoutManager(this)
        rvCheckPoints.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadData() }
        fabAddCheckPoint.setOnClickListener {
            startActivity(Intent(this, AddCheckPointActivity::class.java))
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@CheckPointActivity).getCheckPoints()
                checkPoints.clear()
                if (res.data != null && res.data.isNotEmpty()) {
                    checkPoints.addAll(res.data)
                    layoutEmpty.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.VISIBLE
                }
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(this@CheckPointActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                layoutEmpty.visibility = if (checkPoints.isEmpty()) View.VISIBLE else View.GONE
            } finally {
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE
            }
        }
    }
}
