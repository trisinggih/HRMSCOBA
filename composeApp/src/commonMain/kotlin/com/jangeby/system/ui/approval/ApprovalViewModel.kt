package com.jangeby.system.ui.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ApprovalViewModel : ViewModel() {

    private val _approvals = MutableStateFlow<List<ApprovalItem>>(emptyList())
    val approvals: StateFlow<List<ApprovalItem>> = _approvals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _selectedTab = MutableStateFlow(0) // 0: Menunggu, 1: Disetujui, 2: Ditolak, 3: Semua
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _badgeCount = MutableStateFlow(0)
    val badgeCount: StateFlow<Int> = _badgeCount

    private val _trackingItem = MutableStateFlow<ApprovalItem?>(null)
    val trackingItem: StateFlow<ApprovalItem?> = _trackingItem

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading

    init {
        loadApprovals(0)
        loadBadges()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
        loadApprovals(index)
    }

    fun loadApprovals(tabIndex: Int = _selectedTab.value) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val statusQuery = when (tabIndex) {
                    0 -> "?status=pending"
                    1 -> "?status=approved"
                    2 -> "?status=rejected"
                    else -> ""
                }
                val url = ApiClient.BASE_URL + "approval/requests$statusQuery"
                val response = client.get(url)
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<PaginatedResponse<ApprovalItem>> = response.body()
                    val items = apiResponse.data?.data ?: emptyList()
                    _approvals.value = items
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _errorMessage.value = err?.message ?: "Gagal memuat persetujuan (${response.status.value})"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Koneksi gagal"
            } finally {
                _isLoading.value = false
            }
        }
        loadBadges()
    }

    fun loadBadges() {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "employee/dashboard/badges")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<BadgeResponse> = response.body()
                    _badgeCount.value = apiResponse.data?.approvalCount ?: 0
                }
            } catch (e: Exception) {
                // Ignore badge error
            }
        }
    }

    fun loadTracking(id: Long) {
        _trackingItem.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "approval/requests/$id")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<ApprovalItem> = response.body()
                    _trackingItem.value = apiResponse.data
                }
            } catch (e: Exception) {
                // Ignore tracking error
            }
        }
    }

    fun clearTracking() {
        _trackingItem.value = null
    }

    fun approveRequest(id: Long, note: String?, tenor: Int?, onComplete: (Boolean, String?) -> Unit) {
        _isActionLoading.value = true
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "approval/requests/$id/approve") {
                    contentType(ContentType.Application.Json)
                    setBody(ActionApprovalRequest(note = note?.takeIf { it.isNotBlank() }, tenor = tenor?.toString()))
                }
                if (response.status.value in 200..299) {
                    val res: ApiResponse<ApprovalItem> = response.body()
                    loadApprovals()
                    onComplete(true, res.message ?: "Pengajuan disetujui")
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    onComplete(false, err?.message ?: "Gagal menyetujui pengajuan")
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Koneksi gagal")
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun rejectRequest(id: Long, note: String?, onComplete: (Boolean, String?) -> Unit) {
        _isActionLoading.value = true
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "approval/requests/$id/reject") {
                    contentType(ContentType.Application.Json)
                    setBody(ActionApprovalRequest(note = note?.takeIf { it.isNotBlank() }))
                }
                if (response.status.value in 200..299) {
                    val res: ApiResponse<ApprovalItem> = response.body()
                    loadApprovals()
                    onComplete(true, res.message ?: "Pengajuan ditolak")
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    onComplete(false, err?.message ?: "Gagal menolak pengajuan")
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Koneksi gagal")
            } finally {
                _isActionLoading.value = false
            }
        }
    }
}
