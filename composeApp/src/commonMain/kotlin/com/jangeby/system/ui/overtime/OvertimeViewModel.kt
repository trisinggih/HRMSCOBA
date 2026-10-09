package com.jangeby.system.ui.overtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.*
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OvertimeViewModel : ViewModel() {

    private val _overtimes = MutableStateFlow<List<OvertimeRequestItem>>(emptyList())
    val overtimes: StateFlow<List<OvertimeRequestItem>> = _overtimes

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
        loadOvertimes()
    }

    fun loadOvertimes() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<PaginatedResponse<OvertimeRequestItem>> = client.get("${ApiClient.BASE_URL}mobile/overtime/history").body()
                if (response.isSuccess) {
                    _overtimes.value = response.data?.data ?: emptyList()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat riwayat lembur"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat riwayat lembur: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitOvertime(
        date: String,
        startTime: String,
        endTime: String,
        reason: String,
        photo: PlatformFile?
    ) {
        if (date.isBlank()) {
            _errorMessage.value = "Silakan pilih tanggal lembur"
            return
        }
        if (startTime.isBlank()) {
            _errorMessage.value = "Silakan pilih jam mulai lembur"
            return
        }
        if (endTime.isBlank()) {
            _errorMessage.value = "Silakan pilih jam selesai lembur"
            return
        }
        if (reason.isBlank()) {
            _errorMessage.value = "Harap isi alasan pengajuan lembur"
            return
        }

        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val photoBytes = photo?.readBytes()
                val response = client.post("${ApiClient.BASE_URL}mobile/overtime/request") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("date", date)
                                append("requested_start_time", startTime)
                                append("requested_end_time", endTime)
                                append("reason", reason)
                                if (photoBytes != null && photo != null) {
                                    append("attachment", photoBytes, Headers.build {
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                        append(HttpHeaders.ContentDisposition, "filename=\"${photo.name}\"")
                                    })
                                }
                            }
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    val apiResponse = runCatching { response.body<ApiResponse<OvertimeRequestItem>>() }.getOrNull()
                    if (apiResponse != null && !apiResponse.isSuccess) {
                        _errorMessage.value = apiResponse.message ?: "Gagal mengajukan lembur"
                    } else {
                        _successMessage.value = "Pengajuan lembur berhasil diajukan dan masuk proses approval"
                        loadOvertimes()
                    }
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _errorMessage.value = err?.message ?: "Gagal mengajukan lembur (${response.status.value})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengajukan lembur: ${e.message ?: "Terjadi kesalahan"}"
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
                val response: ApiResponse<ApprovalItem> = client.get("${ApiClient.BASE_URL}approval/requests/$approvalId").body()
                if (response.isSuccess) {
                    _trackingDetail.value = response.data
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                _isTrackingLoading.value = false
            }
        }
    }

    fun cancelOvertime(overtimeId: Long, reason: String = "Dibatalkan oleh karyawan") {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val payload = buildJsonObject {
                    put("reason", reason)
                }
                val response = client.post("${ApiClient.BASE_URL}mobile/overtime/$overtimeId/cancel") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }

                if (response.status.value in 200..299) {
                    _successMessage.value = "Pengajuan lembur berhasil dibatalkan"
                    loadOvertimes()
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _errorMessage.value = err?.message ?: "Gagal membatalkan pengajuan lembur"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal membatalkan lembur: ${e.message ?: "Terjadi kesalahan"}"
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun clearTracking() {
        _trackingDetail.value = null
    }

    fun formatDateDMY(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "-"
        return try {
            val date = if (dateStr.contains("T")) {
                val dateTime = kotlinx.datetime.LocalDateTime.parse(dateStr.replace("Z", ""))
                dateTime.date
            } else {
                kotlinx.datetime.LocalDate.parse(dateStr)
            }
            "${date.dayOfMonth.toString().padStart(2, '0')}-${date.monthNumber.toString().padStart(2, '0')}-${date.year}"
        } catch (e: Exception) {
            dateStr
        }
    }
}
