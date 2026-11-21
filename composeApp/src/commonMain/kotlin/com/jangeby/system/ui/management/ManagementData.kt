package com.jangeby.system.ui.management

import com.jangeby.system.data.remote.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class ManagementAccess(val dashboard: Boolean = false, val reports: Boolean = false, @SerialName("all_employee_data") val allEmployeeData: Boolean = false)
@Serializable
data class ManagementField(val label: String, val value: String)
@Serializable
data class ManagementRow(val id: String, val title: String, val subtitle: String = "", val status: String = "", val photo: String? = null, val fields: List<ManagementField> = emptyList(), val images: List<String> = emptyList())
@Serializable
data class ManagementPage(val items: List<ManagementRow> = emptyList(), val total: Int = 0, val page: Int = 1, @SerialName("last_page") val lastPage: Int = 1)

data class ManagementOption(val id: String, val name: String, val company: String = "")
internal fun JsonElement?.obj(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
internal fun JsonElement?.arr(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())
internal fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull ?: ""
internal fun JsonElement?.number(): Float = (this as? JsonPrimitive)?.floatOrNull ?: 0f
internal suspend fun managementGet(path: String, params: Map<String, String> = emptyMap()): JsonObject {
    val response = ApiClient.create().get(ApiClient.BASE_URL + "mobile/management/" + path) {
        params.filterValues { it.isNotBlank() }.forEach { (key, value) -> parameter(key, value) }
    }
    if (response.status.value >= 500) error("Server belum dapat memuat data. Silakan coba lagi.")
    val root = response.body<JsonObject>()
    if (response.status.value == 401) error("Sesi telah berakhir. Silakan masuk kembali.")
    if (response.status.value == 403) error("Anda tidak memiliki akses menu ini.")
    if (response.status.value !in 200..299) error(root["message"].text().ifBlank { "Data belum dapat dimuat (${response.status.value})." })
    return root["data"].obj()
}
internal fun JsonObject.options(key: String): List<ManagementOption> = this[key].arr().map { ManagementOption(it.obj()["id"].text(), it.obj()["name"].text(), it.obj()["company_id"].text()) }
internal val moduleTitles = mapOf("pengajuan" to "Pengajuan", "tasks" to "Tugas", "payslips" to "Slip Gaji", "assets" to "Aset", "loans" to "Kasbon", "overtime" to "Lembur", "checkpoints" to "Check Point", "announcements" to "Pengumuman", "personal" to "Personal")
