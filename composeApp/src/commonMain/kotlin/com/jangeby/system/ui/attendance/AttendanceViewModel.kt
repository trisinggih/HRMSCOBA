package com.jangeby.system.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Serializable
data class ClockInRequest(
    val latitude: Double = -6.200000,
    val longitude: Double = 106.816666,
    val accuracy: Double = 10.0,
    @SerialName("device_info")
    val deviceInfo: String = "Mobile App",
    @SerialName("face_photo")
    val facePhoto: String = ""
)

@Serializable
data class ClockOutRequest(
    val latitude: Double = -6.200000,
    val longitude: Double = 106.816666,
    val accuracy: Double = 10.0,
    @SerialName("device_info")
    val deviceInfo: String = "Mobile App",
    @SerialName("face_photo")
    val facePhoto: String = ""
)

sealed class AttendanceState {
    object Idle : AttendanceState()
    object Loading : AttendanceState()
    data class Success(val message: String) : AttendanceState()
    data class Error(val message: String) : AttendanceState()
    data class LatePrompt(val date: String) : AttendanceState()
}

class AttendanceViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _state = MutableStateFlow<AttendanceState>(AttendanceState.Idle)
    val state: StateFlow<AttendanceState> = _state

    private val _calendarItems = MutableStateFlow<List<CalendarItem>>(emptyList())
    val calendarItems: StateFlow<List<CalendarItem>> = _calendarItems

    private val _todayItem = MutableStateFlow<CalendarItem?>(null)
    val todayItem: StateFlow<CalendarItem?> = _todayItem

    private val _todayDateStr = MutableStateFlow("2026-10-05")
    val todayDateStr: StateFlow<String> = _todayDateStr

    private val _isLoadingData = MutableStateFlow(false)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData

    private val _primaryBranch = MutableStateFlow<Branch?>(null)
    val primaryBranch: StateFlow<Branch?> = _primaryBranch

    private val _allBranches = MutableStateFlow<List<BranchLocationData>>(emptyList())
    val allBranches: StateFlow<List<BranchLocationData>> = _allBranches

    private val _canClockInAnywhere = MutableStateFlow(false)
    val canClockInAnywhere: StateFlow<Boolean> = _canClockInAnywhere

    init {
        loadShiftCalendar()
        loadBranchData()
    }

    private suspend fun resolveEmployeeId(): Long {
        if (appSettings.employeeId != 0L) {
            return appSettings.employeeId
        }

        val jsonStr = appSettings.userJson
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val profile = ApiClient.json.decodeFromString<EmployeeProfile>(jsonStr)
                if (profile.id != 0L) {
                    appSettings.employeeId = profile.id
                    return profile.id
                }
            } catch (e: Exception) {}
        }

        try {
            val client = ApiClient.create()
            val response = client.get(ApiClient.BASE_URL + "employee/profile")
            if (response.status.value in 200..299) {
                val apiResponse: ApiResponse<EmployeeProfile> = response.body()
                val profile = apiResponse.data
                if (profile != null && profile.id != 0L) {
                    appSettings.employeeId = profile.id
                    return profile.id
                }
            }
        } catch (e: Exception) {}

        return 1L
    }

    private suspend fun resolveBranchId(): Long {
        if (appSettings.employeeBranchId != -1L) {
            return appSettings.employeeBranchId
        }

        val jsonStr = appSettings.userJson
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val profile = ApiClient.json.decodeFromString<EmployeeProfile>(jsonStr)
                if (profile.branchId != null && profile.branchId != 0L) {
                    appSettings.employeeBranchId = profile.branchId
                    return profile.branchId
                }
            } catch (e: Exception) {}
        }

        try {
            val client = ApiClient.create()
            val response = client.get(ApiClient.BASE_URL + "employee/profile")
            if (response.status.value in 200..299) {
                val apiResponse: ApiResponse<EmployeeProfile> = response.body()
                val profile = apiResponse.data
                if (profile?.branchId != null && profile.branchId != 0L) {
                    appSettings.employeeBranchId = profile.branchId
                    return profile.branchId
                }
            }
        } catch (e: Exception) {}

        return -1L
    }

    fun loadBranchData() {
        viewModelScope.launch {
            val client = ApiClient.create()
            
            try {
                val branchId = resolveBranchId()
                if (branchId != -1L) {
                    val branchRes = client.get(ApiClient.BASE_URL + "branches/$branchId")
                    if (branchRes.status.value in 200..299) {
                        val apiRes: ApiResponse<Branch> = branchRes.body()
                        _primaryBranch.value = apiRes.data
                    }
                }
            } catch (e: Exception) {
                // Primary branch fetch error
            }

            try {
                val permRes = client.get(ApiClient.BASE_URL + "attendance/check-radius-permission")
                if (permRes.status.value in 200..299) {
                    val apiRes: ApiResponse<RadiusPermissionResponse> = permRes.body()
                    _canClockInAnywhere.value = apiRes.data?.canClockInAnywhere ?: false
                }
            } catch (e: Exception) {
                // Radius permission error
            }

            try {
                val allRes = client.get(ApiClient.BASE_URL + "attendance/branch-locations")
                if (allRes.status.value in 200..299) {
                    val apiRes: ApiResponse<List<BranchLocationData>> = allRes.body()
                    val list = apiRes.data ?: emptyList()
                    _allBranches.value = list
                    
                    if (_primaryBranch.value == null && list.isNotEmpty()) {
                        val first = list.first()
                        _primaryBranch.value = Branch(
                            id = first.id,
                            name = first.name,
                            latitude = first.latitude,
                            longitude = first.longitude,
                            radius = first.attendanceRadius
                        )
                    }
                }
            } catch (e: Exception) {
                // Branch locations error
            }
        }
    }

    fun loadShiftCalendar(targetYear: Int? = null, targetMonth: Int? = null) {
        _isLoadingData.value = true
        viewModelScope.launch {
            try {
                val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                val year = targetYear ?: now.year
                val month = targetMonth ?: now.monthNumber
                val todayStr = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"

                val empId = resolveEmployeeId()
                val client = ApiClient.create()
                val url = ApiClient.BASE_URL + "employee-shifts/calendar?employee_id=$empId&year=$year&month=$month"
                val response = client.get(url)

                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<List<CalendarItem>> = response.body()
                    val items = apiResponse.data ?: emptyList()
                    _calendarItems.value = items

                    _todayDateStr.value = todayStr
                    val foundToday = items.find { it.date == todayStr } ?: items.firstOrNull()
                    _todayItem.value = foundToday

                    if (foundToday?.attendance?.clockIn != null && foundToday.attendance.clockOut == null) {
                        com.jangeby.system.hardware.LocationTracker.start()
                    } else {
                        com.jangeby.system.hardware.LocationTracker.stop()
                    }
                } else {
                    _state.value = AttendanceState.Error("Gagal memuat jadwal (${response.status.value})")
                }
            } catch (e: Exception) {
                _state.value = AttendanceState.Error(e.message ?: "Terjadi kesalahan koneksi")
            } finally {
                _isLoadingData.value = false
            }
        }
    }

    fun submitClockIn(lat: Double = -6.200000, lon: Double = 106.816666, isOvertime: Boolean = false, facePhoto: String = "") {
        _state.value = AttendanceState.Loading
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val endpoint = if (isOvertime) "mobile/overtime/clock-in" else "attendance/clock-in"
                val response = client.post(ApiClient.BASE_URL + endpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(ClockInRequest(latitude = lat, longitude = lon, facePhoto = facePhoto))
                }

                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<AttendanceItem?> = response.body()
                    val item = apiResponse.data
                    com.jangeby.system.hardware.LocationTracker.start()
                    if (item?.status == "late") {
                        val lateDate = if (!item.date.isNullOrBlank()) item.date else {
                            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
                        }
                        _state.value = AttendanceState.LatePrompt(lateDate)
                    } else {
                        val msg = apiResponse.message ?: if (isOvertime) "Berhasil Clock In Lembur" else "Berhasil Clock In"
                        _state.value = AttendanceState.Success(msg)
                        loadShiftCalendar()
                    }
                } else {
                    val errMsg = try {
                        val err: ApiResponse<kotlinx.serialization.json.JsonElement?> = response.body()
                        err.message ?: "Gagal Clock In (${response.status.value})"
                    } catch (_: Exception) {
                        "Gagal Clock In (${response.status.value})"
                    }
                    _state.value = AttendanceState.Error(errMsg)
                }
            } catch (e: Exception) {
                _state.value = AttendanceState.Error(e.message ?: "Koneksi gagal")
            }
        }
    }

    fun submitClockOut(lat: Double = -6.200000, lon: Double = 106.816666, isOvertime: Boolean = false, facePhoto: String = "") {
        _state.value = AttendanceState.Loading
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val endpoint = if (isOvertime) "mobile/overtime/clock-out" else "attendance/clock-out"
                val response = client.post(ApiClient.BASE_URL + endpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(ClockOutRequest(latitude = lat, longitude = lon, facePhoto = facePhoto))
                }

                if (response.status.value in 200..299) {
                    com.jangeby.system.hardware.LocationTracker.stop()
                    val apiResponse: ApiResponse<kotlinx.serialization.json.JsonElement?> = response.body()
                    val msg = apiResponse.message ?: if (isOvertime) "Berhasil Clock Out Lembur" else "Berhasil Clock Out"
                    _state.value = AttendanceState.Success(msg)
                    loadShiftCalendar()
                } else {
                    val errMsg = try {
                        val err: ApiResponse<kotlinx.serialization.json.JsonElement?> = response.body()
                        err.message ?: "Gagal Clock Out (${response.status.value})"
                    } catch (_: Exception) {
                        "Gagal Clock Out (${response.status.value})"
                    }
                    _state.value = AttendanceState.Error(errMsg)
                }
            } catch (e: Exception) {
                _state.value = AttendanceState.Error(e.message ?: "Koneksi gagal")
            }
        }
    }

    fun resetState() {
        _state.value = AttendanceState.Idle
    }
    
    fun dismissLatePrompt() {
        _state.value = AttendanceState.Success("Clock-in terlambat (tanpa alasan)")
        loadShiftCalendar()
    }

    fun submitLateReason(date: String, reason: String) {
        _state.value = AttendanceState.Loading
        viewModelScope.launch {
            try {
                val finalDate = if (date.isNotBlank()) date else {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
                }
                val client = ApiClient.create()
                var response = client.post(ApiClient.BASE_URL + "late/request") {
                    contentType(ContentType.Application.Json)
                    setBody(LateRequestData(date = finalDate, reason = reason))
                }
                if (response.status.value == 404) {
                    response = client.post(ApiClient.BASE_URL + "attendance/late") {
                        contentType(ContentType.Application.Json)
                        setBody(LateRequestData(date = finalDate, reason = reason))
                    }
                }
                if (response.status.value in 200..299) {
                    _state.value = AttendanceState.Success("Alasan keterlambatan berhasil dikirim")
                    loadShiftCalendar()
                } else {
                    val errMsg = try {
                        val err: ApiResponse<String?> = response.body()
                        err.message ?: "Gagal mengirim alasan (${response.status.value})"
                    } catch (_: Exception) {
                        "Gagal mengirim alasan (${response.status.value})"
                    }
                    _state.value = AttendanceState.Error(errMsg)
                }
            } catch (e: Exception) {
                _state.value = AttendanceState.Error(e.message ?: "Koneksi gagal")
            }
        }
    }
}

@Serializable
data class BranchLocationData(
    val id: Long,
    val name: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("attendance_radius") val attendanceRadius: Double? = null
)

@Serializable
data class Branch(
    val id: Long,
    val name: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("attendance_radius") val radius: Double? = null
)

@Serializable
data class RadiusPermissionResponse(
    @SerialName("can_clock_in_anywhere") val canClockInAnywhere: Boolean = false
)
