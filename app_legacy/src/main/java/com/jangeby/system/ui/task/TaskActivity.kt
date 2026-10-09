package com.jangeby.system.ui.task

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.TaskItem
import com.jangeby.system.ui.UiHelpers
import kotlinx.coroutines.launch

/**
 * Layar Tugas Saya (My Tasks).
 * Terhubung ke GET /my/tasks (Phase 7).
 */
class TaskActivity : AppCompatActivity() {

    private lateinit var rvList: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progress: ProgressBar
    private lateinit var tvDateSelector: TextView
    private lateinit var tvPicName: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var ivPrevDay: View
    private lateinit var ivNextDay: View
    private lateinit var btnMarkComplete: View

    private lateinit var adapter: TaskAdapter
    private var currentDate: java.util.Calendar = java.util.Calendar.getInstance()
    private val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    private val displayFormat = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)
        
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        rvList = findViewById(R.id.rvTasks)
        tvEmpty = findViewById(R.id.tvEmpty)
        progress = findViewById(R.id.progressBar)
        tvDateSelector = findViewById(R.id.tvDateSelector)
        ivPrevDay = findViewById(R.id.ivPrevDay)
        ivNextDay = findViewById(R.id.ivNextDay)
        btnMarkComplete = findViewById(R.id.btnMarkComplete)
        tvPicName = findViewById(R.id.tvPicName)
        tvDepartment = findViewById(R.id.tvDepartment)
        
        rvList.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(emptyList()) { task ->
            if (task.status != "done" && task.status != "SELESAI") {
                confirmCompleteTask(task)
            }
        }
        rvList.adapter = adapter

        btnMarkComplete.visibility = View.GONE

        setupProfile()
        setupDateNavigator()

        // btnMarkComplete is now unused, as we complete per task item
        btnMarkComplete.visibility = View.GONE
        
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            val sheet = BottomSheetAddTask()
            sheet.onSaveListener = { tasks ->
                saveTasksBatch(tasks)
            }
            sheet.show(supportFragmentManager, "AddTask")
        }

        loadTasks()
    }

    private fun confirmCompleteTask(task: com.jangeby.system.data.remote.TaskItem) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(com.jangeby.system.R.layout.layout_bottom_sheet_confirmation, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val tvTitle = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsTitle)
        val tvMessage = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsMessage)
        val btnPositive = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsPositive)
        val btnNegative = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsNegative)
        
        tvTitle.text = "Selesaikan Tugas"
        tvMessage.text = "Apakah Anda yakin tugas '${task.title}' sudah selesai?"
        btnPositive.text = "Ya"
        btnNegative.text = "Belum"
        
        btnPositive.setOnClickListener {
            completeTask(task.id)
            bottomSheetDialog.dismiss()
        }
        
        btnNegative.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }

    private fun completeTask(taskId: Long) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val req = mapOf("status" to "done")
                ApiClient.getService(this@TaskActivity).myTasksUpdateStatus(taskId, req)
                Toast.makeText(this@TaskActivity, "Tugas ditandai selesai!", Toast.LENGTH_SHORT).show()
                loadTasks()
            } catch (t: Throwable) {
                Toast.makeText(this@TaskActivity, "Gagal menyelesaikan: ${UiHelpers.errorMessageFrom(this@TaskActivity, t)}", Toast.LENGTH_SHORT).show()
                setLoading(false)
            }
        }
    }
    
    private fun saveTasksBatch(tasks: List<String>) {
        setLoading(true)
        val dateStr = dateFormat.format(currentDate.time)
        lifecycleScope.launch {
            try {
                val req = mapOf(
                    "date" to dateStr,
                    "tasks" to tasks
                )
                ApiClient.getService(this@TaskActivity).myTasksBatchStore(req)
                Toast.makeText(this@TaskActivity, "Tugas berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                loadTasks()
            } catch (t: Throwable) {
                Toast.makeText(this@TaskActivity, "Gagal menambahkan: ${UiHelpers.errorMessageFrom(this@TaskActivity, t)}", Toast.LENGTH_SHORT).show()
                setLoading(false)
            }
        }
    }

    private fun setupProfile() {
        val prefs = com.jangeby.system.data.local.SecurePrefs(this)
        val name = prefs.employeeName ?: "Karyawan"
        val dept = prefs.employeePosition ?: "Divisi"
        
        tvPicName.text = "PIC: ${name.uppercase()}"
        tvDepartment.text = dept.uppercase()
    }

    private fun setupDateNavigator() {
        updateDateText()
        
        ivPrevDay.setOnClickListener {
            currentDate.add(java.util.Calendar.DAY_OF_MONTH, -1)
            updateDateText()
            loadTasks()
        }
        
        ivNextDay.setOnClickListener {
            currentDate.add(java.util.Calendar.DAY_OF_MONTH, 1)
            updateDateText()
            loadTasks()
        }
    }
    
    private fun updateDateText() {
        tvDateSelector.text = displayFormat.format(currentDate.time).uppercase()
    }

    private fun loadTasks() {
        setLoading(true)
        val dateStr = dateFormat.format(currentDate.time)
        
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@TaskActivity).myTasks(perPage = 50, date = dateStr)
                val items = res.data?.items ?: emptyList()
                adapter.update(items)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (t: Throwable) {
                tvEmpty.text = "Gagal memuat tugas.\n${UiHelpers.errorMessageFrom(this@TaskActivity, t)}"
                tvEmpty.visibility = View.VISIBLE
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
