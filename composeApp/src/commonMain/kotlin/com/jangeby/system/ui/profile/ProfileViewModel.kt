package com.jangeby.system.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.ChangePasswordRequest
import com.jangeby.system.data.remote.EmployeeProfile
import com.jangeby.system.data.remote.EmployeeSignatureResponse
import com.jangeby.system.data.remote.SaveSignatureRequest
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

class ProfileViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _profile = MutableStateFlow<EmployeeProfile?>(null)
    val profile: StateFlow<EmployeeProfile?> = _profile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    private val _currentSignatureUrl = MutableStateFlow<String?>(null)
    val currentSignatureUrl: StateFlow<String?> = _currentSignatureUrl

    private val _isSignatureSaving = MutableStateFlow(false)
    val isSignatureSaving: StateFlow<Boolean> = _isSignatureSaving

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        appSettings.userJson?.let {
            runCatching {
                _profile.value = ApiClient.json.decodeFromString<EmployeeProfile>(it)
            }
        }
        loadProfile()
        loadSignature()
    }

    fun loadProfile() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "employee/profile")
                if (response.status.value in 200..299) {
                    val res: ApiResponse<EmployeeProfile> = response.body()
                    if (res.success && res.data != null) {
                        val sanitizedPhoto = ApiClient.sanitizeImageUrl(res.data.photoUrl)
                        val updatedProfile = res.data.copy(photoUrl = sanitizedPhoto)
                        _profile.value = updatedProfile
                        // Save cached data to AppSettings
                        res.data.name?.let { appSettings.userName = it }
                        res.data.position?.let { appSettings.userPosition = it }
                        res.data.branchId?.let { appSettings.employeeBranchId = it }
                        res.data.signatureUrl?.let { _currentSignatureUrl.value = it }
                        sanitizedPhoto?.let { appSettings.userPhotoUrl = it }
                        appSettings.hasSubordinates = res.data.hasSubordinates
                        appSettings.subordinatesCount = res.data.subordinatesCount
                        appSettings.userJson = ApiClient.json.encodeToString(updatedProfile)
                    }
                }
            } catch (e: Exception) {
                // Keep local cached fallback if network error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(
        name: String,
        phone: String,
        address: String,
        birthDate: String,
        photoFile: PlatformFile?,
        photoBytes: ByteArray? = null
    ) {
        _isUpdating.value = true
        viewModelScope.launch {
            try {
                val bytes = photoBytes ?: photoFile?.readBytes()
                val client = ApiClient.create()

                val originalName = photoFile?.name ?: "profile.jpg"
                val safeName = if (originalName.endsWith(".png", ignoreCase = true)) "profile.png"
                               else if (originalName.endsWith(".webp", ignoreCase = true)) "profile.webp"
                               else "profile.jpg"
                val contentType = if (safeName.endsWith(".png", ignoreCase = true)) "image/png"
                                  else if (safeName.endsWith(".webp", ignoreCase = true)) "image/webp"
                                  else "image/jpeg"

                val response = client.post(ApiClient.BASE_URL + "employee/profile/update") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("name", name)
                                append("phone", phone)
                                append("address", address)
                                append("birth_date", birthDate)
                                if (bytes != null) {
                                    append("photo", bytes, Headers.build {
                                        append(HttpHeaders.ContentType, contentType)
                                        append(HttpHeaders.ContentDisposition, "filename=\"$safeName\"")
                                    })
                                }
                            }
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    _successMessage.value = "Profil berhasil diperbarui"
                    appSettings.userName = name
                    val res = runCatching { response.body<ApiResponse<EmployeeProfile>>() }.getOrNull()
                    if (res?.data != null) {
                        val sanitized = ApiClient.sanitizeImageUrl(res.data.photoUrl)
                        val updatedProfile = res.data.copy(photoUrl = sanitized)
                        _profile.value = updatedProfile
                        sanitized?.let { appSettings.userPhotoUrl = it }
                        appSettings.userJson = ApiClient.json.encodeToString(updatedProfile)
                    }
                    loadProfile()
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _errorMessage.value = err?.message ?: "Gagal memperbarui profil (${response.status.value})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memperbarui profil: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun changePassword(currentPass: String, newPass: String, confirmPass: String, onResult: (Boolean, String?) -> Unit) {
        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            onResult(false, "Semua kolom password harus diisi")
            return
        }
        if (newPass != confirmPass) {
            onResult(false, "Konfirmasi password baru tidak cocok")
            return
        }

        _isUpdating.value = true
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "employee-auth/change-password") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ChangePasswordRequest(
                            currentPassword = currentPass,
                            newPassword = newPass,
                            newPasswordConfirmation = confirmPass
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    val res = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
                    if (res != null && !res.isSuccess) {
                        onResult(false, res.message ?: "Gagal mengubah password")
                    } else {
                        onResult(true, "Password berhasil diubah")
                    }
                } else {
                    val err = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
                    onResult(false, err?.message ?: "Gagal mengubah password (${response.status.value})")
                }
            } catch (e: Exception) {
                onResult(false, "Gagal mengubah password: ${e.message ?: "Terjadi kesalahan"}")
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                client.post(ApiClient.BASE_URL + "employee-auth/logout")
            } catch (e: Exception) {
                // Ignore network error on logout
            } finally {
                appSettings.clear()
                onComplete()
            }
        }
    }

    fun loadSignature() {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "employee/signature")
                if (response.status.value in 200..299) {
                    val res: ApiResponse<EmployeeSignatureResponse?> = response.body()
                    if (res.success && res.data != null) {
                        _currentSignatureUrl.value = res.data.signatureUrl
                    }
                }
            } catch (e: Exception) {
                // Ignore network error on signature fetch
            }
        }
    }

    fun saveSignature(base64Image: String, strokesJson: String?, onResult: (Boolean, String?) -> Unit) {
        _isSignatureSaving.value = true
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "employee/signature") {
                    contentType(ContentType.Application.Json)
                    setBody(SaveSignatureRequest(signatureBase64 = base64Image, strokesData = strokesJson))
                }
                if (response.status.value in 200..299) {
                    val res: ApiResponse<EmployeeSignatureResponse> = response.body()
                    if (res.success && res.data != null) {
                        _currentSignatureUrl.value = res.data.signatureUrl
                        _successMessage.value = "Tanda tangan berhasil disimpan"
                        onResult(true, "Tanda tangan berhasil disimpan")
                    } else {
                        onResult(false, res.message?.ifBlank { "Gagal menyimpan tanda tangan" } ?: "Gagal menyimpan tanda tangan")
                    }
                } else {
                    val err = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
                    onResult(false, err?.message ?: "Gagal menyimpan tanda tangan (${response.status.value})")
                }
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan: ${e.message ?: "Koneksi gagal"}")
            } finally {
                _isSignatureSaving.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
