package com.jangeby.system.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.LoginRequest
import com.jangeby.system.data.remote.LoginData
import com.jangeby.system.data.remote.EmployeeProfile
import com.jangeby.system.data.remote.CompanyLogoData
import com.jangeby.system.hardware.DeviceInfoProvider
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val profile: EmployeeProfile?) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    private val _companyLogoUrl = MutableStateFlow<String?>(appSettings.companyLogoUrl)
    val companyLogoUrl: StateFlow<String?> = _companyLogoUrl

    private val _companyName = MutableStateFlow<String?>(appSettings.companyName ?: "JANGEBY")
    val companyName: StateFlow<String?> = _companyName

    init {
        fetchCompanyBranding()
    }

    fun fetchCompanyBranding() {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "company/logo")
                if (response.status.value in 200..299) {
                    val apiResponse: com.jangeby.system.data.remote.ApiResponse<CompanyLogoData> = response.body()
                    if (apiResponse.success && apiResponse.data != null) {
                        apiResponse.data.logo?.let { logo ->
                            val finalUrl = if (logo.startsWith("http://")) logo.replaceFirst("http://", "https://") else logo
                            _companyLogoUrl.value = finalUrl
                            appSettings.companyLogoUrl = finalUrl
                        }
                        if (!apiResponse.data.name.isNullOrBlank()) {
                            _companyName.value = apiResponse.data.name
                            appSettings.companyName = apiResponse.data.name
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore, keep default
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.value = LoginState.Error("Email dan password tidak boleh kosong")
            return
        }

        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val deviceId = DeviceInfoProvider.getDeviceId()
                val deviceName = DeviceInfoProvider.getDeviceName()

                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "employee-auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        LoginRequest(
                            email = email,
                            password = pass,
                            deviceName = deviceName,
                            deviceId = deviceId
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    val apiResponse: com.jangeby.system.data.remote.ApiResponse<LoginData> = response.body()
                    if (apiResponse.success && apiResponse.data != null) {
                        // Save token & user
                        appSettings.token = apiResponse.data.token
                        apiResponse.data.employee?.let { emp ->
                            appSettings.employeeId = emp.id
                            appSettings.employeeBranchId = emp.branchId ?: -1L
                            appSettings.userJson = ApiClient.json.encodeToString(emp)
                            appSettings.userName = emp.name ?: "Karyawan"
                            appSettings.companyName = emp.company ?: "HRMS"
                            appSettings.userPosition = emp.position ?: ""
                        }
                        
                        _state.value = LoginState.Success(apiResponse.data.employee)
                    } else {
                        _state.value = LoginState.Error(apiResponse.message ?: "Login gagal")
                    }
                } else {
                    val err = runCatching { response.body<com.jangeby.system.data.remote.ApiResponse<Unit>>() }.getOrNull()
                    val errorMsg = err?.message ?: "Login gagal (${response.status.value})"
                    _state.value = LoginState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(e.message ?: "Terjadi kesalahan koneksi")
            }
        }
    }
}
