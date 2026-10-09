package com.jangeby.system.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

import kotlinx.serialization.json.JsonElement

class TaskViewModel(
    private val appSettings: AppSettings = AppSettings()
) : ViewModel() {

    private val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    val picName: String
        get() = (appSettings.userName ?: "Karyawan").uppercase()

    val departmentName: String
        get() = (appSettings.userPosition ?: "Divisi").uppercase()

    init {
        loadTasks()
    }

    fun goToPrevDay() {
        _selectedDate.value = _selectedDate.value.minus(DatePeriod(days = 1))
        loadTasks()
    }

    fun goToNextDay() {
        _selectedDate.value = _selectedDate.value.plus(DatePeriod(days = 1))
        loadTasks()
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
        loadTasks()
    }

    fun loadTasks() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val date = _selectedDate.value
                val dateStr = formatDateIso(date)
                val client = ApiClient.create()
                val response: ApiResponse<TaskListData> = client.get("${ApiClient.BASE_URL}my/tasks?per_page=50&date=$dateStr").body()
                
                if (response.success) {
                    _tasks.value = response.data?.items ?: emptyList()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat tugas"
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "Terjadi kesalahan saat memuat tugas"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeTask(taskId: Long) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<TaskItem?> = client.put("${ApiClient.BASE_URL}my/tasks/$taskId/status") {
                    contentType(ContentType.Application.Json)
                    setBody(TaskStatusRequest(status = "done"))
                }.body()

                if (response.success) {
                    _successMessage.value = "Tugas ditandai selesai!"
                    loadTasks()
                } else {
                    _errorMessage.value = response.message ?: "Gagal menyelesaikan tugas"
                    _isLoading.value = false
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "Gagal menyelesaikan tugas"
                _isLoading.value = false
            }
        }
    }

    fun completeAllTasks() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val dateStr = formatDateIso(_selectedDate.value)
                val client = ApiClient.create()
                val response: ApiResponse<JsonElement?> = client.post("${ApiClient.BASE_URL}my/tasks/complete-all") {
                    contentType(ContentType.Application.Json)
                    setBody(TaskCompleteAllRequest(date = dateStr))
                }.body()

                if (response.success) {
                    _successMessage.value = response.message ?: "Semua tugas berhasil diselesaikan!"
                    loadTasks()
                } else {
                    _errorMessage.value = response.message ?: "Gagal menyelesaikan semua tugas"
                    _isLoading.value = false
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "Gagal menyelesaikan semua tugas"
                _isLoading.value = false
            }
        }
    }

    fun saveTasksBatch(tasks: List<String>) {
        if (tasks.isEmpty()) {
            _errorMessage.value = "Harap isi minimal 1 tugas"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val dateStr = formatDateIso(_selectedDate.value)
                val client = ApiClient.create()
                val response: ApiResponse<JsonElement?> = client.post("${ApiClient.BASE_URL}my/tasks/batch") {
                    contentType(ContentType.Application.Json)
                    setBody(TaskBatchRequest(date = dateStr, tasks = tasks))
                }.body()

                if (response.success) {
                    _successMessage.value = "Tugas berhasil ditambahkan"
                    loadTasks()
                } else {
                    _errorMessage.value = response.message ?: "Gagal menambahkan tugas"
                    _isLoading.value = false
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "Gagal menambahkan tugas"
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    private fun formatDateIso(date: LocalDate): String {
        val y = date.year.toString().padStart(4, '0')
        val m = date.monthNumber.toString().padStart(2, '0')
        val d = date.dayOfMonth.toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    fun formatDisplayDate(date: LocalDate): String {
        val months = listOf(
            "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
            "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
        )
        val monthName = if (date.monthNumber in 1..12) months[date.monthNumber - 1] else ""
        return "${date.dayOfMonth} $monthName ${date.year}"
    }
}
