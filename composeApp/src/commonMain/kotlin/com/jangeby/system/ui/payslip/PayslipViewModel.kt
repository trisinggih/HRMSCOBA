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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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

    private val _pdfLoading = MutableStateFlow(false)
    val pdfLoading: StateFlow<Boolean> = _pdfLoading.asStateFlow()

    fun downloadPdf(id: Long, onReady: (ByteArray) -> Unit, onError: (String) -> Unit) {
        if (_pdfLoading.value) return
        _pdfLoading.value = true
        viewModelScope.launch {
            try {
                // Employee endpoint uses the web payslip template and checks ownership.
                val response = ApiClient.create().get("${ApiClient.BASE_URL}my/payslips/$id/pdf")
                if (response.status.value !in 200..299) {
                    onError("Gagal mengunduh PDF (${response.status.value}). Silakan coba lagi.")
                    return@launch
                }
                val bytes: ByteArray = response.body()
                if (bytes.size < 5 || bytes.copyOfRange(0, 5).decodeToString() != "%PDF-") {
                    onError("Respons server bukan PDF. Silakan coba lagi.")
                    return@launch
                }
                onReady(bytes)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onError("PDF belum dapat diunduh. Periksa koneksi dan coba lagi.")
            } finally {
                _pdfLoading.value = false
            }
        }
    }

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

    private var detailJob: Job? = null
    private val _detailLoading = MutableStateFlow(false)
    val detailLoading = _detailLoading.asStateFlow()
    private val _detailError = MutableStateFlow<String?>(null)
    val detailError = _detailError.asStateFlow()

    fun selectPayslip(item: PayslipItem) {
        detailJob?.cancel()
        _selectedPayslip.value = item
        _detailError.value = null
        _detailLoading.value = true
        detailJob = viewModelScope.launch {
            try {
                val response = ApiClient.create().get("${ApiClient.BASE_URL}my/payslips/${item.id}")
                val result: ApiResponse<PayslipItem> = response.body()
                if (response.status.value in 200..299 && result.data != null) {
                    if (_selectedPayslip.value?.id == item.id) {
                        _selectedPayslip.value = result.data
                        _payslips.value = _payslips.value.map { if (it.id == item.id) result.data else it }
                    }
                } else _detailError.value = result.message ?: "Gagal memuat detail slip gaji."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _detailError.value = "Gagal memuat detail. Periksa koneksi dan coba lagi." }
            finally { _detailLoading.value = false }
        }
    }

    fun clearSelectedPayslip() {
        detailJob?.cancel()
        _detailLoading.value = false
        _detailError.value = null
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
