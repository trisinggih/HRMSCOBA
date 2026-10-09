package com.jangeby.system.ui.checkpoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.CheckPointItem
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

class CheckPointViewModel : ViewModel() {

    private val _checkPoints = MutableStateFlow<List<CheckPointItem>>(emptyList())
    val checkPoints: StateFlow<List<CheckPointItem>> = _checkPoints

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        loadCheckPoints()
    }

    fun loadCheckPoints() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<List<CheckPointItem>> = client.get("${ApiClient.BASE_URL}attendance/check-points").body()
                if (response.isSuccess) {
                    _checkPoints.value = response.data ?: emptyList()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat data check point"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data check point: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitCheckPoint(
        clientName: String,
        purpose: String,
        latitude: Double,
        longitude: Double,
        notes: String,
        photos: List<PlatformFile>
    ) {
        if (clientName.isBlank()) {
            _errorMessage.value = "Harap isi nama klien/tempat"
            return
        }
        if (purpose.isBlank()) {
            _errorMessage.value = "Harap isi tujuan kunjungan"
            return
        }
        if (photos.isEmpty()) {
            _errorMessage.value = "Mohon ambil minimal 1 foto bukti kunjungan"
            return
        }

        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                
                // Read bytes beforehand to avoid suspending function inside formData block
                val photoBytesList = photos.map { it.name to it.readBytes() }

                val response = client.post("${ApiClient.BASE_URL}attendance/check-points") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("client_name", clientName)
                                append("purpose", purpose)
                                append("latitude", latitude.toString())
                                append("longitude", longitude.toString())
                                append("notes", notes)
                                
                                photoBytesList.forEach { (name, bytes) ->
                                    append("photos[]", bytes, Headers.build {
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                        append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                                    })
                                }
                            }
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    _successMessage.value = "Check point berhasil disubmit"
                    loadCheckPoints()
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _errorMessage.value = err?.message ?: "Gagal mensubmit check point (${response.status.value})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mensubmit check point: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
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
                        return "${dateParts[2]}-$monthStr-${dateParts[0]} ${timeParts[0]}:${timeParts[1]}"
                    }
                }
                return "${dateParts[2]}-$monthStr-${dateParts[0]}"
            }
        } catch (e: Exception) { }
        return dateStr ?: "-"
    }
}
