package com.jangeby.system.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.*
import com.jangeby.system.hardware.DeviceInfoProvider
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val email: String, val name: String) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

sealed class VerifyOtpState {
    object Idle : VerifyOtpState()
    object Loading : VerifyOtpState()
    data class Success(val profile: EmployeeProfile?) : VerifyOtpState()
    data class Error(val message: String) : VerifyOtpState()
}

class RegisterViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    private val _verifyState = MutableStateFlow<VerifyOtpState>(VerifyOtpState.Idle)
    val verifyState: StateFlow<VerifyOtpState> = _verifyState

    private val _companyLogoUrl = MutableStateFlow<String?>(ApiClient.sanitizeImageUrl(appSettings.companyLogoUrl))
    val companyLogoUrl: StateFlow<String?> = _companyLogoUrl

    private val _companyName = MutableStateFlow<String?>(appSettings.companyName ?: "JANGEBY")
    val companyName: StateFlow<String?> = _companyName

    private val _resendSeconds = MutableStateFlow(0)
    val resendSeconds: StateFlow<Int> = _resendSeconds

    private val _resendMessage = MutableStateFlow<String?>(null)
    val resendMessage: StateFlow<String?> = _resendMessage

    private var countdownJob: Job? = null

    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }

    fun resetVerifyState() {
        _verifyState.value = VerifyOtpState.Idle
    }

    fun register(
        name: String,
        email: String,
        phone: String,
        pass: String,
        confirmPass: String
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) {
            _registerState.value = RegisterState.Error("Nama lengkap wajib diisi")
            return
        }
        if (trimmedEmail.isBlank()) {
            _registerState.value = RegisterState.Error("Alamat email wajib diisi")
            return
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            _registerState.value = RegisterState.Error("Format email tidak valid")
            return
        }
        if (pass.isBlank()) {
            _registerState.value = RegisterState.Error("Password wajib diisi")
            return
        }
        if (pass.length < 6) {
            _registerState.value = RegisterState.Error("Password minimal harus 6 karakter")
            return
        }
        if (pass != confirmPass) {
            _registerState.value = RegisterState.Error("Konfirmasi password tidak cocok")
            return
        }

        _registerState.value = RegisterState.Loading
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "employee-auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        RegisterRequest(
                            name = trimmedName,
                            email = trimmedEmail,
                            phone = if (trimmedPhone.isNotBlank()) trimmedPhone else null,
                            password = pass,
                            passwordConfirmation = confirmPass
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<RegisterResponseData> = response.body()
                    if (apiResponse.success) {
                        _registerState.value = RegisterState.Success(trimmedEmail, trimmedName)
                        startCountdown(60)
                    } else {
                        _registerState.value = RegisterState.Error(apiResponse.message ?: "Registrasi gagal")
                    }
                } else {
                    val err = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
                    val errorMsg = err?.message ?: "Registrasi gagal (${response.status.value})"
                    _registerState.value = RegisterState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Terjadi kesalahan koneksi")
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedOtp = otp.trim()

        if (trimmedOtp.isBlank()) {
            _verifyState.value = VerifyOtpState.Error("Silakan masukkan kode OTP")
            return
        }
        if (trimmedOtp.length != 6) {
            _verifyState.value = VerifyOtpState.Error("Kode OTP harus terdiri dari 6 digit angka")
            return
        }

        _verifyState.value = VerifyOtpState.Loading
        viewModelScope.launch {
            try {
                val deviceId = DeviceInfoProvider.getDeviceId()
                val deviceName = DeviceInfoProvider.getDeviceName()

                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "employee-auth/register/verify-otp") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        VerifyOtpRequest(
                            email = trimmedEmail,
                            otp = trimmedOtp,
                            deviceName = deviceName,
                            deviceId = deviceId
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<LoginData> = response.body()
                    if (apiResponse.success && apiResponse.data != null) {
                        // Simpan kredensial token dan profil pengguna ke AppSettings
                        appSettings.token = apiResponse.data.token
                        apiResponse.data.employee?.let { emp ->
                            appSettings.employeeId = emp.id
                            appSettings.employeeBranchId = emp.branchId ?: -1L
                            appSettings.userJson = ApiClient.json.encodeToString(emp)
                            appSettings.userName = emp.name ?: "Karyawan"
                            appSettings.companyName = emp.company ?: "HRMS"
                            appSettings.userPosition = emp.position ?: ""
                            appSettings.hasSubordinates = emp.hasSubordinates
                            appSettings.subordinatesCount = emp.subordinatesCount
                            emp.photoUrl?.let { appSettings.userPhotoUrl = it }
                        }

                        _verifyState.value = VerifyOtpState.Success(apiResponse.data.employee)
                    } else {
                        _verifyState.value = VerifyOtpState.Error(apiResponse.message ?: "Verifikasi OTP gagal")
                    }
                } else {
                    val err = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
                    val errorMsg = err?.message ?: "Verifikasi OTP gagal (${response.status.value})"
                    _verifyState.value = VerifyOtpState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _verifyState.value = VerifyOtpState.Error(e.message ?: "Terjadi kesalahan koneksi")
            }
        }
    }

    fun resendOtp(email: String) {
        if (_resendSeconds.value > 0) return

        val trimmedEmail = email.trim().lowercase()
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "employee-auth/register/resend-otp") {
                    contentType(ContentType.Application.Json)
                    setBody(ResendOtpRequest(email = trimmedEmail))
                }

                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<Unit> = response.body()
                    _resendMessage.value = apiResponse.message ?: "Kode OTP baru telah dikirimkan ke email Anda"
                    startCountdown(60)
                } else {
                    val err = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
                    _resendMessage.value = err?.message ?: "Gagal mengirim ulang kode OTP"
                }
            } catch (e: Exception) {
                _resendMessage.value = e.message ?: "Gagal terhubung ke server"
            }
        }
    }

    fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        _resendSeconds.value = seconds
        countdownJob = viewModelScope.launch {
            var count = seconds
            while (count > 0) {
                delay(1000)
                count--
                _resendSeconds.value = count
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
