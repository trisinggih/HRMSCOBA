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
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.jangeby.system.utils.DateTimeUtils

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
        startDate: String,
        endDate: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        reason: String,
        attachment: PlatformFile?,
        onSuccess: () -> Unit
    ) {
        _isSubmitting.value = true
        _errorMessage.value = null

        val finalEndDate = if (!endDate.isNullOrBlank()) endDate else startDate
        val today = DateTimeUtils.nowInJakarta().date
        val targetDate = runCatching { LocalDate.parse(startDate.take(10)) }.getOrNull()
        val isExitPermit = type.lowercase() in listOf("out_of_office", "izin_keluar")

        val isEarlyLeave = type == "early_leave"
        if (isEarlyLeave) {
            if (targetDate != today || finalEndDate.take(10) != today.toString()) {
                _errorMessage.value = "Izin pulang awal hanya berlaku untuk hari ini"
                _isSubmitting.value = false
                return
            }
            if (endTime == null || !Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(endTime)) {
                _errorMessage.value = "Pilih jam akhir yang valid untuk izin pulang awal"
                _isSubmitting.value = false
                return
            }
        } else if (isExitPermit) {
            if (targetDate != null && targetDate != today) {
                _errorMessage.value = "Pengajuan izin keluar hanya dapat diajukan untuk hari ini"
                _isSubmitting.value = false
                return
            }
        } else {
            if (targetDate != null && targetDate < today) {
                _errorMessage.value = "Pengajuan izin tidak boleh sebelum hari ini"
                _isSubmitting.value = false
                return
            }
        }

        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val attachmentBytes = attachment?.readBytes()

                val response = client.post("${ApiClient.BASE_URL}permission/request") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("type", type)
                                append("date", startDate)
                                append("start_date", startDate)
                                append("end_date", finalEndDate)
                                if (!isEarlyLeave && !startTime.isNullOrBlank()) append("start_time", startTime)
                                if (!endTime.isNullOrBlank()) append("end_time", endTime)
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
        return com.jangeby.system.utils.DateTimeUtils.formatDateDMY(dateStr)
    }

    fun formatDateOnlyDMY(dateStr: String?): String {
        return com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(dateStr)
    }

    fun formatPermitDateRange(item: PermitRequestItem): String {
        val start = item.startDate ?: item.date
        val end = item.endDate ?: start
        val isMultiDay = !end.isNullOrBlank() && !start.isNullOrBlank() && end.take(10) != start.take(10)
        val isExitPermit = item.type?.lowercase() in listOf("out_of_office", "izin_keluar")

        return if (isMultiDay) {
            "${formatDateOnlyDMY(start)} s/d ${formatDateOnlyDMY(end)}"
        } else {
            val singleDate = formatDateOnlyDMY(start)
            if (item.type == "early_leave") {
                "$singleDate | Jam akhir: ${item.endTime?.take(5) ?: "-"}"
            } else if (isExitPermit && (!item.startTime.isNullOrBlank() || !item.endTime.isNullOrBlank())) {
                "$singleDate · ${item.startTime?.take(5) ?: "-"} - ${item.endTime?.take(5) ?: "-"}"
            } else {
                singleDate
            }
        }
    }
}
