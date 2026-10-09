package com.jangeby.system.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.LeaveRequestCreate
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LeaveState {
    object Idle : LeaveState()
    object Loading : LeaveState()
    object Success : LeaveState()
    data class Error(val message: String) : LeaveState()
}

class LeaveViewModel : ViewModel() {

    private val _state = MutableStateFlow<LeaveState>(LeaveState.Idle)
    val state: StateFlow<LeaveState> = _state

    fun submitLeave(leaveTypeId: Long, startDate: String, endDate: String, reason: String) {
        if (startDate.isBlank() || endDate.isBlank() || reason.isBlank()) {
            _state.value = LeaveState.Error("Semua field harus diisi")
            return
        }

        _state.value = LeaveState.Loading
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "leave/request") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        LeaveRequestCreate(
                            leaveTypeId = leaveTypeId,
                            startDate = startDate,
                            endDate = endDate,
                            reason = reason
                        )
                    )
                }

                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<String?> = response.body()
                    if (apiResponse.success) {
                        _state.value = LeaveState.Success
                    } else {
                        _state.value = LeaveState.Error(apiResponse.message ?: "Gagal mengajukan cuti")
                    }
                } else {
                    _state.value = LeaveState.Error("HTTP Error: ${response.status.value}")
                }
            } catch (e: Exception) {
                _state.value = LeaveState.Error(e.message ?: "Terjadi kesalahan koneksi")
            }
        }
    }
    
    fun resetState() {
        _state.value = LeaveState.Idle
    }
}
