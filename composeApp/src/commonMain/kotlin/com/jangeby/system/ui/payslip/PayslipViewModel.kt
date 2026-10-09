package com.jangeby.system.ui.payslip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.PaginatedResponse
import com.jangeby.system.data.remote.PayslipItem
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class PayslipViewModel : ViewModel() {
    private val _payslips = MutableStateFlow<List<PayslipItem>>(emptyList())
    val payslips: StateFlow<List<PayslipItem>> = _payslips.asStateFlow()

    private val _selectedPayslip = MutableStateFlow<PayslipItem?>(null)
    val selectedPayslip: StateFlow<PayslipItem?> = _selectedPayslip.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPayslips()
    }

    fun loadPayslips() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response = client.get("${ApiClient.BASE_URL}my/payslips")
                if (response.status.value in 200..299) {
                    val apiRes: ApiResponse<PaginatedResponse<PayslipItem>> = response.body()
                    val items = apiRes.data?.data ?: emptyList()
                    _payslips.value = items
                } else {
                    _errorMessage.value = "Gagal memuat slip gaji (${response.status.value})"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Koneksi gagal"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectPayslip(item: PayslipItem) {
        _selectedPayslip.value = item
    }

    fun clearSelectedPayslip() {
        _selectedPayslip.value = null
    }

    fun formatRupiah(amount: Double?): String {
        if (amount == null) return "Rp 0"
        val rounded = amount.toLong()
        val isNegative = rounded < 0
        val absVal = abs(rounded)
        val str = absVal.toString().reversed().chunked(3).joinToString(".").reversed()
        return if (isNegative) "-Rp $str" else "Rp $str"
    }

    fun formatPeriodTitle(item: PayslipItem): String {
        val periodName = item.period?.name
        if (!periodName.isNullOrBlank()) {
            return periodName
        }
        val p = item.period?.period
        if (!p.isNullOrBlank()) {
            val parts = p.split("-")
            if (parts.size >= 2) {
                val year = parts[0]
                val month = when (parts[1]) {
                    "01" -> "Januari"
                    "02" -> "Februari"
                    "03" -> "Maret"
                    "04" -> "April"
                    "05" -> "Mei"
                    "06" -> "Juni"
                    "07" -> "Juli"
                    "08" -> "Agustus"
                    "09" -> "September"
                    "10" -> "Oktober"
                    "11" -> "November"
                    "12" -> "Desember"
                    else -> parts[1]
                }
                return "$month $year"
            }
            return p
        }
        return "Slip Gaji #${item.id}"
    }
}
