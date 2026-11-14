package com.jangeby.system.ui.kpi

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
import kotlinx.coroutines.CancellationException

class KpiViewModel : ViewModel() {

    private val _assessments = MutableStateFlow<List<KpiAssessmentItem>>(emptyList())
    val assessments: StateFlow<List<KpiAssessmentItem>> = _assessments

    private val _selectedAssessment = MutableStateFlow<KpiAssessmentItem?>(null)
    val selectedAssessment: StateFlow<KpiAssessmentItem?> = _selectedAssessment

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        loadMyKpi()
    }

    fun loadMyKpi() {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val response: ApiResponse<PaginatedResponse<KpiAssessmentItem>> = 
                    client.get("${ApiClient.BASE_URL}employee/kpi/my-assessments").body()
                if (response.isSuccess) {
                    val list = response.data?.data ?: emptyList()
                    _assessments.value = list
                    _selectedAssessment.value = list.firstOrNull { it.id == _selectedAssessment.value?.id }
                        ?: list.firstOrNull()
                } else {
                    _errorMessage.value = response.message ?: "Gagal memuat data KPI"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat KPI: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectAssessment(item: KpiAssessmentItem) {
        _selectedAssessment.value = item
    }

    fun submitSelfAssessment(
        assessmentId: Long,
        scores: List<SelfAssessmentScorePayload>,
        selfNotes: String
    ) {
        _isSubmitting.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            try {
                val client = ApiClient.create()
                val payload = SubmitSelfAssessmentRequest(scores = scores, selfNotes = selfNotes)
                val response: ApiResponse<KpiAssessmentItem> = client.post("${ApiClient.BASE_URL}employee/kpi/my-assessments/$assessmentId/self-assess") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }.body()

                if (response.isSuccess) {
                    _successMessage.value = "Self-assessment KPI berhasil dikirim ke atasan!"
                    if (response.data != null) {
                        _selectedAssessment.value = response.data
                    }
                    loadMyKpi()
                } else {
                    _errorMessage.value = response.message ?: "Gagal mengirim self-assessment"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengirim self-assessment: ${e.message ?: "Terjadi kesalahan"}"
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
