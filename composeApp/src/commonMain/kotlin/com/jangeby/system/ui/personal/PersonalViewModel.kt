package com.jangeby.system.ui.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jangeby.system.data.remote.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PersonalViewModel : ViewModel() {

    private val _profile = MutableStateFlow<EmployeeProfile?>(null)
    val profile: StateFlow<EmployeeProfile?> = _profile

    private val _families = MutableStateFlow<List<FamilyModel>>(emptyList())
    val families: StateFlow<List<FamilyModel>> = _families

    private val _educations = MutableStateFlow<List<EducationModel>>(emptyList())
    val educations: StateFlow<List<EducationModel>> = _educations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        loadAll()
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun loadAll() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                loadProfileInternal()
                loadFamilyInternal()
                loadEducationInternal()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadProfileInternal() {
        try {
            val client = ApiClient.create()
            val response: ApiResponse<EmployeeProfile> =
                client.get("${ApiClient.BASE_URL}employee/profile").body()
            if (response.isSuccess && response.data != null) {
                _profile.value = response.data
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadFamilyInternal() {
        try {
            val client = ApiClient.create()
            val response: ApiResponse<List<FamilyModel>> =
                client.get("${ApiClient.BASE_URL}employee/family").body()
            if (response.isSuccess) {
                _families.value = response.data ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadEducationInternal() {
        try {
            val client = ApiClient.create()
            val response: ApiResponse<List<EducationModel>> =
                client.get("${ApiClient.BASE_URL}employee/education").body()
            if (response.isSuccess) {
                _educations.value = response.data ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadFamily() {
        viewModelScope.launch {
            _isLoading.value = true
            loadFamilyInternal()
            _isLoading.value = false
        }
    }

    fun loadEducation() {
        viewModelScope.launch {
            _isLoading.value = true
            loadEducationInternal()
            _isLoading.value = false
        }
    }

    fun storeFamily(req: FamilyRequest, onSuccess: () -> Unit) {
        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<FamilyModel> = client.post("${ApiClient.BASE_URL}employee/family") {
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }.body()

                if (response.isSuccess) {
                    _successMessage.value = response.message ?: "Data keluarga berhasil disimpan"
                    loadFamilyInternal()
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Gagal menyimpan data keluarga"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun updateFamily(id: Int, req: FamilyRequest, onSuccess: () -> Unit) {
        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<FamilyModel> = client.put("${ApiClient.BASE_URL}employee/family/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }.body()

                if (response.isSuccess) {
                    _successMessage.value = response.message ?: "Data keluarga berhasil diperbarui"
                    loadFamilyInternal()
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memperbarui data keluarga"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deleteFamily(id: Int) {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<String?> = client.delete("${ApiClient.BASE_URL}employee/family/$id").body()
                if (response.isSuccess) {
                    _successMessage.value = response.message ?: "Data keluarga berhasil dihapus"
                    loadFamilyInternal()
                } else {
                    _errorMessage.value = response.message ?: "Gagal menghapus data keluarga"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Terjadi kesalahan"}"
            }
        }
    }

    fun storeEducation(req: EducationRequest, onSuccess: () -> Unit) {
        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<EducationModel> = client.post("${ApiClient.BASE_URL}employee/education") {
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }.body()

                if (response.isSuccess) {
                    _successMessage.value = response.message ?: "Data pendidikan berhasil disimpan"
                    loadEducationInternal()
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Gagal menyimpan data pendidikan"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun updateEducation(id: Int, req: EducationRequest, onSuccess: () -> Unit) {
        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<EducationModel> = client.put("${ApiClient.BASE_URL}employee/education/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }.body()

                if (response.isSuccess) {
                    _successMessage.value = response.message ?: "Data pendidikan berhasil diperbarui"
                    loadEducationInternal()
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memperbarui data pendidikan"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deleteEducation(id: Int) {
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<String?> = client.delete("${ApiClient.BASE_URL}employee/education/$id").body()
                if (response.isSuccess) {
                    _successMessage.value = response.message ?: "Data pendidikan berhasil dihapus"
                    loadEducationInternal()
                } else {
                    _errorMessage.value = response.message ?: "Gagal menghapus data pendidikan"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Terjadi kesalahan"}"
            }
        }
    }
}
