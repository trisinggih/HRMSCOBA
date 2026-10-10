package com.jangeby.system.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.ChangePasswordRequest
import com.jangeby.system.data.remote.EmployeeProfile
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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        loadProfile()
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
                        _profile.value = res.data
                        // Save cached data to AppSettings
                        res.data.name?.let { appSettings.userName = it }
                        res.data.position?.let { appSettings.userPosition = it }
                        res.data.branchId?.let { appSettings.employeeBranchId = it }
                        appSettings.userJson = ApiClient.json.encodeToString(res.data)
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
        photoFile: PlatformFile?
    ) {
        _isUpdating.value = true
        viewModelScope.launch {
            try {
                val photoBytes = photoFile?.readBytes()
                val client = ApiClient.create()

                val response = client.post(ApiClient.BASE_URL + "employee/profile/update") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("name", name)
                                append("phone", phone)
                                append("address", address)
                                append("birth_date", birthDate)
                                if (photoBytes != null && photoFile != null) {
                                    append("photo", photoBytes, Headers.build {
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                        append(HttpHeaders.ContentDisposition, "filename=\"${photoFile.name}\"")
                                    })
                                }
                            }
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    _successMessage.value = "Profil berhasil diperbarui"
                    appSettings.userName = name
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

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
