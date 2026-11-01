package com.jangeby.system.ui.asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.AssetItem
import com.jangeby.system.data.remote.AssetRequestCreate
import com.jangeby.system.data.remote.AssetRequestItem
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.jangeby.system.data.local.AppSettings

class AssetViewModel(private val appSettings: AppSettings = AppSettings()) : ViewModel() {

    private val _assets = MutableStateFlow<List<AssetItem>>(emptyList())
    val assets: StateFlow<List<AssetItem>> = _assets

    private val _assetRequests = MutableStateFlow<List<AssetRequestItem>>(emptyList())
    val assetRequests: StateFlow<List<AssetRequestItem>> = _assetRequests

    private val _hasSubordinates = MutableStateFlow(appSettings.hasSubordinates)
    val hasSubordinates: StateFlow<Boolean> = _hasSubordinates

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        loadAssets()
        if (_hasSubordinates.value) {
            loadAssetRequests()
        }
    }

    fun loadAssets() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<List<AssetItem>> = client.get("${ApiClient.BASE_URL}my-assets").body()
                if (response.isSuccess) {
                    _assets.value = response.data ?: emptyList()
                    val canReq = response.hasSubordinates ?: response.canRequestAssets
                    if (canReq != null) {
                        _hasSubordinates.value = canReq
                        appSettings.hasSubordinates = canReq
                        if (canReq && _assetRequests.value.isEmpty()) {
                            loadAssetRequests()
                        }
                    }
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat data aset."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat aset: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAssetRequests() {
        if (!_hasSubordinates.value) return
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<List<AssetRequestItem>> = client.get("${ApiClient.BASE_URL}my-asset-requests").body()
                if (response.isSuccess) {
                    _assetRequests.value = response.data ?: emptyList()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat riwayat permintaan aset."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat permintaan aset: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitAssetRequest(
        requestType: String = "kebutuhan_rutin",
        itemName: String? = null,
        itemSize: String? = null,
        forWho: String?,
        purpose: String,
        quantity: Int = 1,
        estimatedPrice: Double? = null,
        specifications: String? = null,
        items: List<com.jangeby.system.data.remote.AssetRequestItemCreate>? = null,
        onSuccess: () -> Unit
    ) {
        if (!_hasSubordinates.value) {
            _errorMessage.value = "Hanya atasan yang memiliki bawahan yang dapat melakukan pengajuan permintaan aset."
            return
        }
        if (items.isNullOrEmpty()) {
            if (itemName.isNullOrBlank()) {
                _errorMessage.value = "Nama barang aset wajib diisi."
                return
            }
            if (requestType == "kebutuhan_perbaikan" && itemSize.isNullOrBlank()) {
                _errorMessage.value = "Keterangan ukuran wajib diisi untuk perbaikan."
                return
            }
        } else {
            val invalidItem = items.firstOrNull { it.itemName.isBlank() }
            if (invalidItem != null) {
                _errorMessage.value = "Semua nama barang harus diisi."
                return
            }
            if (requestType == "kebutuhan_perbaikan") {
                val missingSize = items.firstOrNull { it.itemSize.isNullOrBlank() }
                if (missingSize != null) {
                    _errorMessage.value = "Keterangan ukuran wajib diisi untuk perbaikan (${missingSize.itemName})."
                    return
                }
            }
        }
        if (purpose.isBlank()) {
            _errorMessage.value = "Kebutuhan untuk apa / keperluan wajib diisi."
            return
        }

        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val finalItems = items?.takeIf { it.isNotEmpty() }
                val calculatedQty = if (!finalItems.isNullOrEmpty()) finalItems.sumOf { it.quantity } else quantity.coerceAtLeast(1)
                val calculatedEstPrice = if (!finalItems.isNullOrEmpty()) {
                    val sum = finalItems.sumOf { it.estimatedPrice ?: 0.0 }
                    if (sum > 0) sum else estimatedPrice
                } else estimatedPrice

                val summaryItemName = if (!finalItems.isNullOrEmpty()) {
                    val first = finalItems.first().itemName
                    if (finalItems.size > 1) "$first (+${finalItems.size - 1} item lainnya)" else first
                } else itemName?.trim()

                val requestPayload = com.jangeby.system.data.remote.AssetRequestCreate(
                    requestType = requestType,
                    itemName = summaryItemName,
                    itemSize = if (!finalItems.isNullOrEmpty()) finalItems.first().itemSize else itemSize?.trim()?.ifBlank { null },
                    forWho = forWho?.trim()?.ifBlank { null },
                    purpose = purpose.trim(),
                    quantity = calculatedQty,
                    estimatedPrice = calculatedEstPrice,
                    specifications = specifications?.trim()?.ifBlank { null },
                    items = finalItems
                )

                val response = client.post("${ApiClient.BASE_URL}my-asset-requests") {
                    contentType(ContentType.Application.Json)
                    setBody(requestPayload)
                }

                if (response.status.value in 200..299) {
                    _successMessage.value = "Pengajuan pembelian aset berhasil dikirim!"
                    loadAssetRequests()
                    onSuccess()
                } else {
                    val err = runCatching { response.body<ApiResponse<String?>>() }.getOrNull()
                    _errorMessage.value = err?.message ?: "Gagal mengirim pengajuan aset (${response.status.value})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengirim pengajuan: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
