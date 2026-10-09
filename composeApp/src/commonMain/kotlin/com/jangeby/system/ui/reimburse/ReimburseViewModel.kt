package com.jangeby.system.ui.reimburse

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
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReimburseViewModel : ViewModel() {

    private val _reimbursements = MutableStateFlow<List<ReimburseItem>>(emptyList())
    val reimbursements: StateFlow<List<ReimburseItem>> = _reimbursements

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
        loadReimbursements()
    }

    fun loadReimbursements() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<PaginatedResponse<ReimburseItem>> = client.get("${ApiClient.BASE_URL}my/reimbursements").body()
                if (response.isSuccess) {
                    _reimbursements.value = response.data?.data ?: emptyList()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat data reimbursement"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data reimbursement: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitReimbursement(
        date: String,
        type: String,
        amount: Double,
        description: String,
        photo: PlatformFile?
    ) {
        if (date.isBlank()) {
            _errorMessage.value = "Harap pilih tanggal reimburse"
            return
        }
        if (type.isBlank()) {
            _errorMessage.value = "Harap pilih jenis klaim"
            return
        }
        if (amount <= 0) {
            _errorMessage.value = "Nominal harus lebih dari 0"
            return
        }

        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val photoBytes = photo?.readBytes()
                val response = client.post("${ApiClient.BASE_URL}my/reimbursements") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("date", date)
                                append("type", type)
                                append("amount", amount.toLong().toString())
                                if (description.isNotBlank()) {
                                    append("description", description)
                                }
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
                    _successMessage.value = "Reimbursement berhasil diajukan"
                    loadReimbursements()
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _errorMessage.value = err?.message ?: "Gagal mengajukan reimbursement (${response.status.value})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengajukan reimbursement: ${e.message ?: "Terjadi kesalahan"}"
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

    fun cancelReimbursement(approvalId: Long) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<ApprovalItem?> = client.post("${ApiClient.BASE_URL}approval/requests/$approvalId/cancel").body()
                if (response.isSuccess) {
                    _successMessage.value = "Pengajuan reimbursement berhasil dibatalkan"
                    loadReimbursements()
                } else {
                    _errorMessage.value = response.message ?: "Gagal membatalkan reimbursement"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal membatalkan reimbursement: ${e.message ?: "Terjadi kesalahan"}"
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

    fun formatRupiah(amount: Double?): String {
        if (amount == null) return "Rp 0"
        val rounded = amount.toLong()
        val str = rounded.toString().reversed().chunked(3).joinToString(".").reversed()
        return "Rp $str"
    }

    fun formatDateDMY(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "-"
        try {
            val parts = dateStr.split("T", " ")
            val dateParts = parts[0].split("-")
            if (dateParts.size == 3) {
                val months = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
                val monthIdx = dateParts[1].toIntOrNull() ?: 1
                val monthStr = if (monthIdx in 1..12) months[monthIdx] else dateParts[1]
                if (parts.size > 1 && parts[1].isNotBlank()) {
                    val timeParts = parts[1].split(":")
                    if (timeParts.size >= 2) {
                        return "${dateParts[2]}-$monthStr-${dateParts[0]} ${timeParts[0]}:${timeParts[1]}"
                    }
                }
                return "${dateParts[2]}-$monthStr-${dateParts[0]}"
            }
        } catch (e: Exception) { }
        return dateStr
    }
}
