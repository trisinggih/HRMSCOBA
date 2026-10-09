package com.jangeby.system.ui.permit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.*
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PermitViewModel : ViewModel() {

    private val _permits = MutableStateFlow<List<PermitRequestItem>>(emptyList())
    val permits: StateFlow<List<PermitRequestItem>> = _permits

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _trackingDetail = MutableStateFlow<ApprovalItem?>(null)
    val trackingDetail: StateFlow<ApprovalItem?> = _trackingDetail

    private val _isTrackingLoading = MutableStateFlow(false)
    val isTrackingLoading: StateFlow<Boolean> = _isTrackingLoading

    init {
        loadPermits()
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun loadPermits() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<PaginatedResponse<PermitRequestItem>> =
                    client.get("${ApiClient.BASE_URL}permission/request").body()
                if (response.isSuccess) {
                    _permits.value = response.data?.data ?: emptyList()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat data izin"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data izin: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitPermit(
        type: String,
        date: String,
        startTime: String,
        endTime: String,
        reason: String,
        attachment: PlatformFile?,
        onSuccess: () -> Unit
    ) {
        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val attachmentBytes = attachment?.readBytes()

                val response = client.post("${ApiClient.BASE_URL}permission/request") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("type", type)
                                append("date", date)
                                append("start_time", startTime)
                                append("end_time", endTime)
                                append("reason", reason)
                                if (attachmentBytes != null && attachment != null) {
                                    append("attachment", attachmentBytes, Headers.build {
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                        append(HttpHeaders.ContentDisposition, "filename=\"${attachment.name}\"")
                                    })
                                }
                            }
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    _successMessage.value = "Pengajuan izin berhasil dikirim"
                    loadPermits()
                    onSuccess()
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _errorMessage.value = err?.message ?: "Gagal mengajukan izin (${response.status.value})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengirim pengajuan: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun loadTracking(approvalId: Long) {
        _isTrackingLoading.value = true
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<ApprovalItem> =
                    client.get("${ApiClient.BASE_URL}approval/requests/$approvalId").body()
                if (response.isSuccess) {
                    _trackingDetail.value = response.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTrackingLoading.value = false
            }
        }
    }

    fun clearTracking() {
        _trackingDetail.value = null
    }

    fun cancelPermit(approvalId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<String?> =
                    client.post("${ApiClient.BASE_URL}approval/requests/$approvalId/cancel").body()
                if (response.isSuccess) {
                    _successMessage.value = response.message ?: "Pengajuan izin berhasil dibatalkan"
                    loadPermits()
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Gagal membatalkan pengajuan izin"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Terjadi kesalahan"}"
            }
        }
    }

    fun formatDateDMY(dateStr: String?): String {
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
}
