package com.jangeby.system.ui.announcement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.AnnouncementData
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.MilestoneItemData
import com.jangeby.system.data.remote.MilestoneResponseData
import com.jangeby.system.data.remote.MilestoneWishData
import com.jangeby.system.data.remote.PaginatedResponse
import com.jangeby.system.data.remote.SendMilestoneWishRequest
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnnouncementViewModel : ViewModel() {

    // Tab state: 0 = Pengumuman Perusahaan, 1 = Momen & Ucapan
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    // ================= PENGUMUMAN STATE =================
    private val _announcements = MutableStateFlow<List<AnnouncementData>>(emptyList())
    val announcements: StateFlow<List<AnnouncementData>> = _announcements

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ================= MILESTONES & WISHES STATE =================
    private val _milestones = MutableStateFlow<List<MilestoneItemData>>(emptyList())
    val milestones: StateFlow<List<MilestoneItemData>> = _milestones

    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount

    private val _isMilestonesLoading = MutableStateFlow(false)
    val isMilestonesLoading: StateFlow<Boolean> = _isMilestonesLoading

    private val _milestonesError = MutableStateFlow<String?>(null)
    val milestonesError: StateFlow<String?> = _milestonesError

    // Filter: "all", "birthday", "anniversary"
    private val _milestoneFilter = MutableStateFlow("all")
    val milestoneFilter: StateFlow<String> = _milestoneFilter

    // Wishes sheet state
    private val _wishesMilestone = MutableStateFlow<MilestoneItemData?>(null)
    val wishesMilestone: StateFlow<MilestoneItemData?> = _wishesMilestone

    private val _wishesList = MutableStateFlow<List<MilestoneWishData>>(emptyList())
    val wishesList: StateFlow<List<MilestoneWishData>> = _wishesList

    private val _isWishesLoading = MutableStateFlow(false)
    val isWishesLoading: StateFlow<Boolean> = _isWishesLoading

    private val _isSendingWish = MutableStateFlow(false)
    val isSendingWish: StateFlow<Boolean> = _isSendingWish

    init {
        loadAnnouncements()
        loadMilestones()
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
        if (tab == 1 && _milestones.value.isEmpty() && !_isMilestonesLoading.value) {
            loadMilestones()
        }
    }

    fun setMilestoneFilter(filter: String) {
        _milestoneFilter.value = filter
        loadMilestones(filter)
    }

    // ================= PENGUMUMAN METHODS =================
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

    // ================= MILESTONES & WISHES METHODS =================
    fun loadMilestones(filter: String = _milestoneFilter.value) {
        _isMilestonesLoading.value = true
        _milestonesError.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<MilestoneResponseData> =
                    client.get("${ApiClient.BASE_URL}employee/milestones?type=$filter").body()
                if (response.isSuccess) {
                    val data = response.data
                    _milestones.value = data?.milestones ?: emptyList()
                    _todayCount.value = data?.todayCount ?: 0
                } else {
                    _milestonesError.value = response.message ?: "Gagal memuat momen & perayaan"
                }
            } catch (e: Exception) {
                _milestonesError.value = "Gagal memuat data: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isMilestonesLoading.value = false
            }
        }
    }

    fun openWishesSheet(milestone: MilestoneItemData) {
        _wishesMilestone.value = milestone
        _wishesList.value = milestone.recentWishes
        loadFullWishes(milestone)
    }

    fun closeWishesSheet() {
        _wishesMilestone.value = null
        _wishesList.value = emptyList()
    }

    private fun loadFullWishes(milestone: MilestoneItemData) {
        _isWishesLoading.value = true
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val url = "${ApiClient.BASE_URL}employee/milestones/wishes?" +
                        "celebrated_employee_id=${milestone.employeeId}&" +
                        "milestone_type=${milestone.type}&" +
                        "milestone_year=${milestone.milestoneYear}"
                val response: ApiResponse<List<MilestoneWishData>> = client.get(url).body()
                if (response.isSuccess) {
                    _wishesList.value = response.data ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isWishesLoading.value = false
            }
        }
    }

    fun sendWish(
        milestone: MilestoneItemData,
        message: String,
        onSuccess: () -> Unit = {}
    ) {
        if (message.isBlank()) return
        _isSendingWish.value = true
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val req = SendMilestoneWishRequest(
                    celebratedEmployeeId = milestone.employeeId,
                    milestoneType = milestone.type,
                    milestoneYear = milestone.milestoneYear,
                    message = message.trim()
                )
                val response: ApiResponse<MilestoneWishData> = client.post("${ApiClient.BASE_URL}employee/milestones/wishes") {
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }.body()

                if (response.isSuccess && response.data != null) {
                    val newWish = response.data

                    // Prepend to wishes list
                    val currentList = _wishesList.value.toMutableList()
                    currentList.add(0, newWish)
                    _wishesList.value = currentList

                    // Update in milestones list
                    _milestones.value = _milestones.value.map { m ->
                        if (m.id == milestone.id) {
                            val updatedRecent = (listOf(newWish) + m.recentWishes).take(3)
                            m.copy(
                                wishesCount = m.wishesCount + 1,
                                hasWished = true,
                                recentWishes = updatedRecent
                            )
                        } else m
                    }

                    // Also update active milestone in sheet
                    _wishesMilestone.value?.let { currentMilestone ->
                        if (currentMilestone.id == milestone.id) {
                            _wishesMilestone.value = currentMilestone.copy(
                                wishesCount = currentMilestone.wishesCount + 1,
                                hasWished = true
                            )
                        }
                    }

                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSendingWish.value = false
            }
        }
    }

    fun deleteWish(wishId: Long, milestone: MilestoneItemData) {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<Unit> = client.delete("${ApiClient.BASE_URL}employee/milestones/wishes/$wishId").body()
                if (response.isSuccess) {
                    _wishesList.value = _wishesList.value.filter { it.id != wishId }
                    _milestones.value = _milestones.value.map { m ->
                        if (m.id == milestone.id) {
                            m.copy(
                                wishesCount = (m.wishesCount - 1).coerceAtLeast(0),
                                recentWishes = m.recentWishes.filter { it.id != wishId }
                            )
                        } else m
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ================= HELPERS =================
    fun formatDateTime(dateStr: String?): String {
        return com.jangeby.system.utils.DateTimeUtils.formatDateTime(dateStr)
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
