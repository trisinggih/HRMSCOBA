package com.jangeby.system.ui.asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.AssetItem
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AssetViewModel : ViewModel() {

    private val _assets = MutableStateFlow<List<AssetItem>>(emptyList())
    val assets: StateFlow<List<AssetItem>> = _assets

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadAssets()
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
}
