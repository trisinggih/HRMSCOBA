package com.jangeby.system.ui.attendance

import androidx.compose.foundation.layout.*
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
                val response = ApiClient.create().get(ApiClient.BASE_URL + "employee/attendance/late-team") {
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
    Scaffold(topBar = {
        TopAppBar(title = { Text("Terlambat") }, navigationIcon = {
            IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
        }, actions = {
            IconButton(onClick = { viewModel.load() }, enabled = !state.loading) { Icon(Icons.Default.Refresh, "Muat ulang") }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(if (state.data.scope == "all") "Keterlambatan seluruh karyawan" else "Keterlambatan bawahan langsung", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { viewModel.changeMonth(-1) }, enabled = !state.loading) { Text("Sebelumnya") }
                    Text(viewModel.month.toString().take(7), fontWeight = FontWeight.Bold)
                    TextButton(onClick = { viewModel.changeMonth(1) }, enabled = !state.loading) { Text("Berikutnya") }
                }
                OutlinedTextField(value = viewModel.search, onValueChange = { viewModel.search = it }, label = { Text("Nama / NIP karyawan") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                TextButton(onClick = { viewModel.load() }, enabled = !state.loading) { Text("Cari") }
            }
            item {
                val summary = state.data.summary
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${summary.employees} karyawan")
                        Text("${summary.records} kejadian")
                        Text("${summary.minutes} menit")
                    }
                }
            }
            if (state.loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            if (state.error != null) item {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load() }, enabled = !state.loading) { Text("Coba lagi") }
            }
            if (!state.loading && state.error == null && state.data.items.isEmpty()) item {
                Text("Belum ada keterlambatan pada bulan ini.", modifier = Modifier.padding(vertical = 24.dp))
            }
            if (!state.loading) items(state.data.items, key = { it.id }) { employee ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!employee.photo.isNullOrBlank()) AsyncImage(model = employee.photo, contentDescription = "Foto ${employee.name}", modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f)) {
                                Text(employee.name ?: "Karyawan", fontWeight = FontWeight.Bold)
                                Text(employee.number ?: "-", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${employee.minutes} menit", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        Text(employee.date)
                        Text("${employee.shiftName ?: "Shift"} ? Jadwal ${employee.shiftStart ?: "-"} WIB")
                        Text("Masuk ${employee.clockIn ?: "-"} WIB")
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
