package com.jangeby.system.ui.pengajuan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.jangeby.system.utils.DateTimeUtils
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

sealed class PengajuanState {
    object Idle : PengajuanState()
    object Loading : PengajuanState()
    data class Success(val message: String) : PengajuanState()
    data class Error(val message: String) : PengajuanState()
}

class PengajuanViewModel : ViewModel() {

    private val _state = MutableStateFlow<PengajuanState>(PengajuanState.Idle)
    val state: StateFlow<PengajuanState> = _state

    private val _saldoCuti = MutableStateFlow<LeaveBalanceItem?>(null)
    val saldoCuti: StateFlow<LeaveBalanceItem?> = _saldoCuti

    private val _leaveBalances = MutableStateFlow<List<LeaveBalanceItem>>(emptyList())
    val leaveBalances: StateFlow<List<LeaveBalanceItem>> = _leaveBalances

    private val _sakitHistory = MutableStateFlow<List<PengajuanHistoryItem>>(emptyList())
    val sakitHistory: StateFlow<List<PengajuanHistoryItem>> = _sakitHistory

    private val _izinHistory = MutableStateFlow<List<PengajuanHistoryItem>>(emptyList())
    val izinHistory: StateFlow<List<PengajuanHistoryItem>> = _izinHistory

    private val _tukarHariHistory = MutableStateFlow<List<PengajuanHistoryItem>>(emptyList())
    val tukarHariHistory: StateFlow<List<PengajuanHistoryItem>> = _tukarHariHistory

    private val _cutiHistory = MutableStateFlow<List<PengajuanHistoryItem>>(emptyList())
    val cutiHistory: StateFlow<List<PengajuanHistoryItem>> = _cutiHistory

    private val _approvalDetails = MutableStateFlow<ApprovalItem?>(null)
    val approvalDetails: StateFlow<ApprovalItem?> = _approvalDetails

    private val _isSaldoLoading = MutableStateFlow(false)
    val isSaldoLoading: StateFlow<Boolean> = _isSaldoLoading

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadSaldoCuti()
        loadSakitHistory()
        loadIzinHistory()
        loadTukarHariHistory()
        loadCutiHistory()
    }

    fun selectLeaveBalance(item: LeaveBalanceItem) {
        _saldoCuti.value = item
    }

    fun loadSaldoCuti() {
        viewModelScope.launch {
            _isSaldoLoading.value = true
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "leave/balance")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<List<LeaveBalanceItem>> = response.body()
                    val data = apiResponse.data
                    if (apiResponse.isSuccess && !data.isNullOrEmpty()) {
                        _leaveBalances.value = data
                        val currentSelected = _saldoCuti.value
                        _saldoCuti.value = if (currentSelected != null) {
                            data.firstOrNull { it.leaveTypeId == currentSelected.leaveTypeId } ?: data.first()
                        } else {
                            data.firstOrNull { it.leaveTypeCode == "CT" } ?: data.first()
                        }
                    }
                }
            } catch (e: Exception) {
                println("[PengajuanViewModel] Error loadSaldoCuti: ${e.message}")
                e.printStackTrace()
            } finally {
                _isSaldoLoading.value = false
            }
        }
    }

    fun loadSakitHistory() {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "sick-leave/request")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<PaginatedResponse<SickLeaveRequestItem>> = response.body()
                    val items = apiResponse.data?.data ?: emptyList()
                    _sakitHistory.value = items.map {
                        val startFormatted = formatDateIndoFull(it.startDate)
                        val dateText = if (!it.endDate.isNullOrBlank() && it.endDate.take(10) != it.startDate?.take(10)) {
                            val endFormatted = formatDateIndoFull(it.endDate)
                            "Tanggal: $startFormatted s/d $endFormatted"
                        } else {
                            "Tanggal: $startFormatted"
                        }
                        PengajuanHistoryItem(
                            id = it.id,
                            type = "sakit",
                            title = "Izin Sakit",
                            date = dateText,
                            description = it.reason ?: "-",
                            status = it.status ?: "pending",
                            approval = it.approval,
                            attachmentUrl = it.medicalCertificate
                        )
                    }
                }
            } catch (e: Exception) { }
        }
    }

    fun loadIzinHistory() {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "permission/request")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<PaginatedResponse<PermitRequestItem>> = response.body()
                    val items = apiResponse.data?.data ?: emptyList()
                    _izinHistory.value = items.map {
                        val titleText = when (it.type) {
                            "business" -> "Izin Dinas Luar Kota"
                            "out_of_office", "izin_keluar" -> "Izin Keluar"
                            "personal" -> "Izin Pribadi"
                            "family" -> "Urusan Keluarga"
                            "urgent" -> "Mendesak / Urgent"
                            "other" -> "Izin Lainnya"
                            else -> "Izin ${it.type?.replaceFirstChar { char -> char.uppercase() } ?: ""}"
                        }
                        val finalStartDate = it.startDate ?: it.date
                        val finalEndDate = it.endDate ?: finalStartDate
                        val isMultiDay = !finalEndDate.isNullOrBlank() && !finalStartDate.isNullOrBlank() && finalEndDate.take(10) != finalStartDate.take(10)
                        val isExitPermit = it.type in listOf("out_of_office", "izin_keluar")
                        val timeStr = if (!isMultiDay && isExitPermit && it.startTime != null && it.endTime != null) " · ${it.startTime.take(5)} - ${it.endTime.take(5)}" else ""
                        val dateText = if (isMultiDay) {
                            "${com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(finalStartDate)} s/d ${com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(finalEndDate)}"
                        } else {
                            com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(finalStartDate) + timeStr
                        }
                        PengajuanHistoryItem(
                            id = it.id,
                            type = "izin",
                            title = titleText,
                            date = dateText,
                            description = it.reason ?: "-",
                            status = it.status ?: "pending",
                            approval = it.approval,
                            attachmentUrl = it.attachment
                        )
                    }
                }
            } catch (e: Exception) { }
        }
    }

    fun loadTukarHariHistory() {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "workday-swap/request")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<PaginatedResponse<WorkdaySwapRequestItem>> = response.body()
                    val items = apiResponse.data?.data ?: emptyList()
                    _tukarHariHistory.value = items.map {
                        PengajuanHistoryItem(
                            id = it.id,
                            type = "tukar_hari",
                            title = "Tukar Hari Kerja",
                            date = "${formatDateDMY(it.workDate)} → ${formatDateDMY(it.swapToDate)}",
                            description = it.reason ?: "-",
                            status = it.status ?: "pending",
                            approval = it.approval,
                            attachmentUrl = null
                        )
                    }
                }
            } catch (e: Exception) { }
        }
    }

    fun loadCutiHistory() {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "leave/request")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<PaginatedResponse<LeaveRequestItem>> = response.body()
                    val items = apiResponse.data?.data ?: emptyList()
                    _cutiHistory.value = items.map {
                        PengajuanHistoryItem(
                            id = it.id,
                            type = "cuti",
                            title = it.leaveType?.name ?: "Cuti",
                            date = if (it.endDate != null && it.endDate != it.startDate) "${formatDateDMY(it.startDate)} – ${formatDateDMY(it.endDate)}" else formatDateDMY(it.startDate),
                            description = it.reason ?: "-",
                            status = it.status ?: "pending",
                            approval = it.approval,
                            attachmentUrl = it.attachment
                        )
                    }
                }
            } catch (e: Exception) { }
        }
    }

    fun formatDateDMY(dateStr: String?): String {
        return com.jangeby.system.utils.DateTimeUtils.formatDateDMY(dateStr)
    }

    fun formatDateIndoFull(dateStr: String?): String {
        return com.jangeby.system.utils.DateTimeUtils.formatDateIndoFull(dateStr)
    }

    fun submitSakit(startDate: String, endDate: String, reason: String, attachment: PlatformFile?) {
        if (startDate.isBlank() || reason.isBlank()) {
            _state.value = PengajuanState.Error("Tanggal mulai dan alasan sakit wajib diisi")
            return
        }
        if (attachment == null) {
            _state.value = PengajuanState.Error("Lampiran surat keterangan dokter wajib dilampirkan")
            return
        }
        val finalEndDate = if (endDate.isNotBlank()) endDate else startDate
        if (finalEndDate < startDate) {
            _state.value = PengajuanState.Error("Tanggal selesai sakit tidak boleh sebelum tanggal mulai")
            return
        }
        val nowJakarta = DateTimeUtils.nowInJakarta()
        val todayStr = nowJakarta.date.toString()
        val isPast10Wib = (nowJakarta.hour > 10) || (nowJakarta.hour == 10 && (nowJakarta.minute > 0 || nowJakarta.second > 0))
        if (startDate <= todayStr && isPast10Wib) {
            _state.value = PengajuanState.Error("Pengajuan izin sakit maksimal diajukan sebelum pukul 10:00 WIB")
            return
        }
        _state.value = PengajuanState.Loading
        viewModelScope.launch {
            try {
                val fileBytes = attachment.readBytes()
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "sick-leave/request") {
                    setBody(MultiPartFormDataContent(
                        formData {
                            append("start_date", startDate)
                            append("end_date", finalEndDate)
                            append("reason", reason)
                            append("medical_certificate", fileBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/*")
                                append(HttpHeaders.ContentDisposition, "filename=\"${attachment.name}\"")
                            })
                        }
                    ))
                }
                if (response.status.value in 200..299) {
                    _state.value = PengajuanState.Success("Pengajuan izin sakit berhasil dikirim")
                    loadSakitHistory()
                } else {
                    val errorBody = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _state.value = PengajuanState.Error(errorBody?.message ?: "Gagal mengirim pengajuan sakit (${response.status.value})")
                }
            } catch (e: Exception) {
                _state.value = PengajuanState.Error(e.message ?: "Koneksi gagal")
            }
        }
    }

    fun submitIzin(
        type: String = "personal",
        startDate: String,
        toDate: String = "",
        startTime: String = "",
        endTime: String = "",
        reason: String,
        attachment: PlatformFile?
    ) {
        if (startDate.isBlank() || reason.isBlank()) {
            _state.value = PengajuanState.Error("Tanggal izin dan alasan wajib diisi")
            return
        }

        val today = DateTimeUtils.nowInJakarta().date
        val targetStart = runCatching { LocalDate.parse(startDate.take(10)) }.getOrNull()
        val isExitPermit = (type == "out_of_office" || type == "izin_keluar")
        val finalEndDate = if (isExitPermit || toDate.isBlank()) startDate else toDate
        val targetEnd = runCatching { LocalDate.parse(finalEndDate.take(10)) }.getOrNull()

        if (isExitPermit) {
            if (targetStart != today) {
                _state.value = PengajuanState.Error("Pengajuan izin keluar hanya dapat diajukan untuk hari ini")
                return
            }
            if (startTime.isBlank() || endTime.isBlank()) {
                _state.value = PengajuanState.Error("Jam keluar dan jam kembali wajib diisi")
                return
            }
            if (endTime <= startTime) {
                _state.value = PengajuanState.Error("Jam kembali harus setelah jam keluar")
                return
            }
        } else {
            if (targetStart != null && targetStart < today) {
                _state.value = PengajuanState.Error("Tanggal izin tidak boleh sebelum hari ini")
                return
            }
            if (targetStart != null && targetEnd != null && targetEnd < targetStart) {
                _state.value = PengajuanState.Error("Tanggal selesai izin tidak boleh sebelum tanggal mulai")
                return
            }
        }

        val sTime = if (startTime.isNotBlank()) startTime else "08:00"
        val eTime = if (endTime.isNotBlank()) endTime else "17:00"

        _state.value = PengajuanState.Loading
        viewModelScope.launch {
            try {
                val fileBytes = attachment?.readBytes()
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "permission/request") {
                    setBody(MultiPartFormDataContent(
                        formData {
                            append("type", type)
                            append("date", startDate)
                            append("start_date", startDate)
                            append("end_date", finalEndDate)
                            append("to_date", finalEndDate)
                            append("start_time", sTime)
                            append("end_time", eTime)
                            append("reason", reason)
                            if (fileBytes != null && attachment != null) {
                                append("attachment", fileBytes, Headers.build {
                                    append(HttpHeaders.ContentType, "image/*")
                                    append(HttpHeaders.ContentDisposition, "filename=\"${attachment.name}\"")
                                })
                            }
                        }
                    ))
                }
                if (response.status.value in 200..299) {
                    _state.value = PengajuanState.Success("Pengajuan izin berhasil dikirim")
                    loadIzinHistory()
                } else {
                    val errorBody = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _state.value = PengajuanState.Error(errorBody?.message ?: "Gagal mengirim pengajuan izin (${response.status.value})")
                }
            } catch (e: Exception) {
                _state.value = PengajuanState.Error(e.message ?: "Koneksi gagal")
            }
        }
    }

    fun submitTukarHari(workDate: String, swapDate: String, reason: String) {
        if (workDate.isBlank() || swapDate.isBlank() || reason.isBlank()) {
            _state.value = PengajuanState.Error("Semua field tukar hari wajib diisi")
            return
        }
        if (workDate == swapDate) {
            _state.value = PengajuanState.Error("Tanggal pengganti harus berbeda dari tanggal kerja asli")
            return
        }
        val today = DateTimeUtils.nowInJakarta().date
        val tomorrow = today.plus(DatePeriod(days = 1))
        val targetWork = runCatching { LocalDate.parse(workDate.take(10)) }.getOrNull()
        val targetSwap = runCatching { LocalDate.parse(swapDate.take(10)) }.getOrNull()
        if ((targetWork != null && targetWork < tomorrow) || (targetSwap != null && targetSwap < tomorrow)) {
            _state.value = PengajuanState.Error("Pengajuan tukar hari wajib dilakukan minimal H-1 (mulai dari besok)")
            return
        }
        _state.value = PengajuanState.Loading
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "workday-swap/request") {
                    contentType(ContentType.Application.Json)
                    setBody(WorkdaySwapRequestCreate(workDate = workDate, swapToDate = swapDate, reason = reason))
                }
                if (response.status.value in 200..299) {
                    _state.value = PengajuanState.Success("Pengajuan tukar hari berhasil dikirim")
                    loadTukarHariHistory()
                } else {
                    val errorBody = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _state.value = PengajuanState.Error(errorBody?.message ?: "Gagal mengirim tukar hari (${response.status.value})")
                }
            } catch (e: Exception) {
                _state.value = PengajuanState.Error(e.message ?: "Koneksi gagal")
            }
        }
    }

    fun submitCuti(leaveTypeId: Long, startDate: String, endDate: String, reason: String, attachment: PlatformFile?) {
        if (startDate.isBlank() || reason.isBlank()) {
            _state.value = PengajuanState.Error("Tanggal mulai dan alasan cuti wajib diisi")
            return
        }
        val finalEndDate = if (endDate.isNotBlank()) endDate else startDate
        if (finalEndDate < startDate) {
            _state.value = PengajuanState.Error("Tanggal selesai cuti tidak boleh sebelum tanggal mulai")
            return
        }
        val today = DateTimeUtils.nowInJakarta().date
        val tomorrow = today.plus(DatePeriod(days = 1))
        val targetStart = runCatching { LocalDate.parse(startDate.take(10)) }.getOrNull()
        if (targetStart != null && targetStart < tomorrow) {
            _state.value = PengajuanState.Error("Pengajuan cuti wajib diajukan minimal H-1 (mulai dari besok)")
            return
        }
        _state.value = PengajuanState.Loading
        viewModelScope.launch {
            try {
                val fileBytes = attachment?.readBytes()
                val client = ApiClient.create()
                val response = client.post(ApiClient.BASE_URL + "leave/request") {
                    setBody(MultiPartFormDataContent(
                        formData {
                            append("leave_type_id", leaveTypeId.toString())
                            append("start_date", startDate)
                            append("end_date", finalEndDate)
                            append("reason", reason)
                            if (fileBytes != null && attachment != null) {
                                append("attachment", fileBytes, Headers.build {
                                    append(HttpHeaders.ContentType, "image/*")
                                    append(HttpHeaders.ContentDisposition, "filename=\"${attachment.name}\"")
                                })
                            }
                        }
                    ))
                }
                if (response.status.value in 200..299) {
                    _state.value = PengajuanState.Success("Pengajuan cuti berhasil dikirim")
                    loadCutiHistory()
                    loadSaldoCuti()
                } else {
                    val errorBody = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _state.value = PengajuanState.Error(errorBody?.message ?: "Gagal mengirim pengajuan cuti (${response.status.value})")
                }
            } catch (e: Exception) {
                _state.value = PengajuanState.Error(e.message ?: "Koneksi gagal")
            }
        }
    }

    fun resetState() {
        _state.value = PengajuanState.Idle
    }

    fun fetchApprovalDetails(approvalId: Long) {
        _approvalDetails.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "approval/requests/$approvalId")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<ApprovalItem> = response.body()
                    _approvalDetails.value = apiResponse.data
                }
            } catch (e: Exception) { }
        }
    }

    fun clearApprovalDetails() {
        _approvalDetails.value = null
    }
}
