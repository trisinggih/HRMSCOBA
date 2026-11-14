package com.jangeby.system.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.component.LatenessIcon
import com.jangeby.system.ui.theme.Background
import com.jangeby.system.ui.theme.Primary
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.utils.DateTimeUtils
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data class LatenessAccess(@SerialName("can_view") val canView: Boolean = false, val scope: String = "team")
@Serializable
data class LateEmployeeItem(
    val id: Int,
    @SerialName("employee_name") val name: String? = null,
    @SerialName("employee_number") val number: String? = null,
    @SerialName("photo_url") val photo: String? = null,
    val date: String,
    @SerialName("clock_in") val clockIn: String? = null,
    @SerialName("shift_name") val shiftName: String? = null,
    @SerialName("shift_start") val shiftStart: String? = null,
    @SerialName("late_minutes") val minutes: Int = 0
)
@Serializable
data class LatenessSummary(
    @SerialName("total_records") val records: Int = 0,
    @SerialName("total_employees") val employees: Int = 0,
    @SerialName("total_minutes") val minutes: Int = 0
)
@Serializable
data class LatenessData(
    val scope: String = "team", val items: List<LateEmployeeItem> = emptyList(),
    val summary: LatenessSummary = LatenessSummary(),
    @SerialName("current_page") val page: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1
)
data class LatenessUiState(val loading: Boolean = false, val error: String? = null, val data: LatenessData = LatenessData())
class TeamLatenessViewModel : ViewModel() {
    private val _state = MutableStateFlow(LatenessUiState())
    val state = _state.asStateFlow()
    var month by mutableStateOf(LocalDate.parse(DateTimeUtils.todayDateString()).let { LocalDate(it.year, it.monthNumber, 1) })
        private set
    var search by mutableStateOf("")
    fun changeMonth(amount: Int) {
        if (_state.value.loading) return
        month = month.plus(DatePeriod(months = amount))
        load()
    }
    fun load(page: Int = 1) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        val start = month.toString()
        val end = month.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).toString()
        val keyword = search.trim()
        viewModelScope.launch {
            try {
                val response = ApiClient.create().get(ApiClient.BASE_URL + "attendance/late-team") {
                    parameter("start_date", start); parameter("end_date", end)
                    parameter("search", keyword); parameter("page", page)
                }
                val body: ApiResponse<LatenessData> = response.body()
                if (response.status.value !in 200..299 || !body.success || body.data == null) {
                    _state.value = LatenessUiState(error = body.message ?: "Tidak dapat membuka daftar keterlambatan.")
                } else {
                    _state.value = LatenessUiState(data = body.data)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.value = LatenessUiState(error = "Gagal memuat data. Periksa koneksi dan coba lagi.") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamLatenessScreen(onNavigateBack: () -> Unit, viewModel: TeamLatenessViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    Scaffold(
        containerColor = Background,
        topBar = {
            HrmsTopBar(
                title = "Terlambat",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.load() }, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, "Muat Ulang", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(if (state.data.scope == "all") "Keterlambatan seluruh karyawan" else "Keterlambatan bawahan langsung", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.changeMonth(-1) }, enabled = !state.loading) { Icon(Icons.Default.KeyboardArrowLeft, "Bulan Sebelumnya", tint = Primary) }
                    Text("${listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")[viewModel.month.monthNumber - 1]} ${viewModel.month.year}", fontWeight = FontWeight.Bold, color = Primary)
                    IconButton(onClick = { viewModel.changeMonth(1) }, enabled = !state.loading) { Icon(Icons.Default.KeyboardArrowRight, "Bulan Berikutnya", tint = Primary) }
                }
                OutlinedTextField(value = viewModel.search, onValueChange = { viewModel.search = it }, label = { Text("Nama / NIP karyawan") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.load() }, enabled = !state.loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cari Karyawan")
                }
            }
            item {
                val summary = state.data.summary
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Karyawan" to summary.employees, "Kejadian" to summary.records, "Menit" to summary.minutes).forEach { (label, count) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
                                Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
            if (state.loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            if (state.error != null) item {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load() }, enabled = !state.loading) { Text("Coba lagi") }
            }
            if (!state.loading && state.error == null && state.data.items.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(LatenessIcon, null, modifier = Modifier.size(44.dp), tint = Color(0xFF94A3B8))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum Ada Keterlambatan", fontWeight = FontWeight.Bold)
                    Text("Tidak ada data untuk periode dan pencarian ini.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
            }
            if (!state.loading) items(state.data.items, key = { it.id }) { employee ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Text(employee.name?.take(1)?.uppercase() ?: "K", color = Primary, fontWeight = FontWeight.Bold)
                                if (!employee.photo.isNullOrBlank()) AsyncImage(model = ApiClient.sanitizeImageUrl(employee.photo), contentDescription = "Foto ${employee.name}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(employee.name ?: "Karyawan", fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(employee.number ?: "-", style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(color = Color(0xFFFFF1F2), shape = RoundedCornerShape(8.dp)) {
                                Text("${employee.minutes} menit", modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Text(employee.date, style = MaterialTheme.typography.labelMedium, color = Primary)
                        Text(employee.shiftName ?: "Shift", fontWeight = FontWeight.Medium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Jadwal ${employee.shiftStart ?: "-"} WIB", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            Text("Masuk ${employee.clockIn ?: "-"} WIB", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (state.data.lastPage > 1) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { viewModel.load(state.data.page - 1) }, enabled = !state.loading && state.data.page > 1) { Text("Sebelumnya") }
                    Text("${state.data.page} / ${state.data.lastPage}")
                    TextButton(onClick = { viewModel.load(state.data.page + 1) }, enabled = !state.loading && state.data.page < state.data.lastPage) { Text("Berikutnya") }
                }
            }
        }
    }
}
