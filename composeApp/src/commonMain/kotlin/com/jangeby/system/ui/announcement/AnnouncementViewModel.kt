package com.jangeby.system.ui.announcement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.AnnouncementData
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.PaginatedResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnnouncementViewModel : ViewModel() {

    private val _announcements = MutableStateFlow<List<AnnouncementData>>(emptyList())
    val announcements: StateFlow<List<AnnouncementData>> = _announcements

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadAnnouncements()
    }

    fun loadAnnouncements() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<PaginatedResponse<AnnouncementData>> =
                    client.get("${ApiClient.BASE_URL}employee/announcements").body()
                if (response.isSuccess) {
                    _announcements.value = response.data?.data ?: emptyList()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat pengumuman"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat pengumuman: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(id: Long) {
        // Optimistic UI update
        _announcements.value = _announcements.value.map { item ->
            if (item.id == id) item.copy(isRead = true) else item
        }

        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                client.post("${ApiClient.BASE_URL}employee/announcements/$id/read")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun formatDateTime(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "-"
        try {
            val cleanStr = dateStr.replace("T", " ").substringBefore(".")
            val parts = cleanStr.split(" ")
            val dateParts = parts[0].split("-")

            if (dateParts.size == 3) {
                val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
                val mIndex = dateParts[1].toIntOrNull() ?: 0
                val monthStr = if (mIndex in 1..12) monthNames[mIndex] else dateParts[1]

                if (parts.size > 1) {
                    val timeParts = parts[1].split(":")
                    if (timeParts.size >= 2) {
                        return "${dateParts[2]} $monthStr ${dateParts[0]}, ${timeParts[0]}:${timeParts[1]}"
                    }
                }
                return "${dateParts[2]} $monthStr ${dateParts[0]}"
            }
        } catch (_: Exception) { }
        return dateStr
    }

    fun parseHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)</li>"), "\n")
            .replace(Regex("(?i)<li>"), "• ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
