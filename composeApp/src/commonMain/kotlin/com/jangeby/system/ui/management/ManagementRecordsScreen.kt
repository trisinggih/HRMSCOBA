package com.jangeby.system.ui.management

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.ui.component.DatePickerField
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.theme.Background
import com.jangeby.system.utils.DateTimeUtils
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
internal fun ManagementPicker(label: String, value: String, options: List<ManagementOption>, onChange: (String) -> Unit, allowAll: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    OutlinedCard(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(options.find { it.id == value }?.name ?: if (allowAll) "Semua" else "Pilih $label", style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) }, text = {
        var keyword by remember { mutableStateOf("") }
        Column {
            OutlinedTextField(keyword, { keyword = it }, label = { Text("Cari") }, singleLine = true)
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                if (allowAll) item { TextButton(onClick = { onChange(""); open = false }) { Text("Semua") } }
                items(options.filter { it.name.contains(keyword, true) }) { option ->
                    TextButton(onClick = { onChange(option.id); open = false }) { Text(option.name) }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { open = false }) { Text("Tutup") } })
}

@Composable
internal fun ManagementRecordCard(row: ManagementRow, compact: Boolean = false, onClick: () -> Unit) {
    if (compact) {
        ReportRecordCard(row, onClick)
        return
    }
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!row.photo.isNullOrBlank()) AsyncImage(row.photo, row.title, Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.title, fontWeight = FontWeight.SemiBold)
                if (row.subtitle.isNotBlank()) Text(row.subtitle, style = MaterialTheme.typography.bodySmall)
                if (row.status.isNotBlank()) Text(row.status.replace('_', ' '), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                row.fields.filter { it.label in listOf("Tanggal", "Tanggal Mulai", "Periode", "Gaji Bersih", "Nominal", "Hadir", "Terlambat", "Judul") }.take(3).forEach { Text("${it.label}: ${it.value}", style = MaterialTheme.typography.bodySmall) }
                Text("Lihat rincian", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ReportRecordCard(row: ManagementRow, onClick: () -> Unit) {
    val summary = row.fields.filter { it.label in listOf("Hadir", "Terlambat", "Mangkir", "Gaji Bersih", "Nominal", "Durasi (jam)", "Jumlah Hari", "Tanggal", "Tanggal Mulai", "Sisa Kasbon") }
        .distinctBy { it.label }.take(3)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!row.photo.isNullOrBlank()) AsyncImage(row.photo, row.title, Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Text(row.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Detail", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (row.status.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(row.status.replace('_', ' '), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            if (summary.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.forEach { field ->
                    Column(Modifier.weight(1f).background(Background, RoundedCornerShape(8.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(field.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(field.value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManagementRecordSheet(row: ManagementRow, onDismiss: () -> Unit, pdf: Boolean = false) {
    val scope = rememberCoroutineScope()
    var pdfBusy by remember { mutableStateOf(false) }
    var pdfError by remember { mutableStateOf<String?>(null) }
    val saver = rememberFileSaverLauncher { }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item { Text(row.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(row.subtitle, style = MaterialTheme.typography.bodySmall) }
            if (pdf) item {
                Button(enabled = !pdfBusy, onClick = {
                    pdfBusy = true; pdfError = null
                    scope.launch {
                        try {
                            val response = ApiClient.create().get(ApiClient.BASE_URL + "mobile/management/payslips/${row.id}/pdf")
                            check(response.status.value in 200..299) { "PDF belum dapat diunduh." }
                            val bytes = response.body<ByteArray>()
                            check(bytes.take(5).toByteArray().decodeToString() == "%PDF-") { "Respons server bukan PDF." }
                            saver.launch(bytes = bytes, baseName = "Slip_Gaji_${row.id}", extension = "pdf")
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { pdfError = e.message ?: "Gagal mengunduh PDF." }
                        finally { pdfBusy = false }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (pdfBusy) "Mengunduh..." else "Unduh PDF Slip Gaji") }
                pdfError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            items(row.images) { url -> AsyncImage(url, "Foto Check Point", Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit) }
            items(row.fields) { field ->
                Column { Text(field.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(field.value.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium) }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementRecordsScreen(module: String = "", reports: Boolean = false, onNavigateBack: () -> Unit) {
    var options by remember { mutableStateOf(JsonObject(emptyMap())) }
    var report by remember { mutableStateOf("attendance") }
    var assetDataset by remember { mutableStateOf("assets") }
    var activeAssetDataset by remember { mutableStateOf("assets") }
    var company by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var employee by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(if (reports) DateTimeUtils.todayDateString().take(7) + "-01" else "") }
    var end by remember { mutableStateOf(if (reports) DateTimeUtils.todayDateString() else "") }
    var keyword by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(false) }
    var request by remember { mutableStateOf(0) }
    var applied by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var activeReport by remember { mutableStateOf("attendance") }
    var page by remember { mutableStateOf(1) }
    var data by remember { mutableStateOf(ManagementPage()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<ManagementRow?>(null) }
    var ready by remember { mutableStateOf(false) }
    val title = if (reports) "Report" else moduleTitles[module] ?: "Data Karyawan"
    LaunchedEffect(request, page) {
        loading = true; error = null
        try {
            if (!ready) {
                options = managementGet("options")
                ready = true
                applied = mapOf("start_date" to start, "end_date" to end)
            }
            val json = managementGet(if (reports) "reports/$activeReport" else "records/${if (module == "assets") activeAssetDataset else module}", applied + ("page" to page.toString()))
            data = ApiClient.json.decodeFromJsonElement<ManagementPage>(json)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { data = ManagementPage(); error = e.message ?: "Tidak dapat memuat data." }
        finally { loading = false }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(request, page) { if (reports) listState.scrollToItem(0) }
    val searchAction: () -> Unit = { applied = applied + ("search" to keyword); page = 1; request++ }
    Scaffold(containerColor = Background, topBar = { HrmsTopBar(title, onNavigateBack, actions = { TextButton(onClick = { filters = true }) { Text("Filter", color = androidx.compose.ui.graphics.Color.White) } }) }, bottomBar = {
        if (reports) Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !loading && page > 1, onClick = { page-- }) { Text("Sebelumnya") }
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("${data.page} / ${data.lastPage}", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                TextButton(enabled = !loading && error == null && page < data.lastPage, onClick = { page++ }) { Text("Berikutnya") }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (reports) item {
                ManagementPicker("Jenis Laporan", activeReport, options.options("reports"), { selectedReport ->
                    report = selectedReport
                    status = ""
                    selected = null
                    if (selectedReport == "individual" && employee.isBlank()) {
                        filters = true
                    } else {
                        activeReport = selectedReport
                        applied = applied + mapOf("status" to "", "employee_id" to if (selectedReport == "individual") employee else "")
                        page = 1
                        request++
                    }
                }, allowAll = false)
            }
            item {
                if (!reports) Text("Seluruh Karyawan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (applied["start_date"].isNullOrBlank() && applied["end_date"].isNullOrBlank()) "Semua tanggal" else "${applied["start_date"].orEmpty()} s/d ${applied["end_date"].orEmpty()}", style = MaterialTheme.typography.bodySmall)
            }
            item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reports) {
                    Surface(modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            BasicTextField(keyword, { keyword = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { if (!loading) searchAction() }),
                                decorationBox = { input ->
                                    if (keyword.isEmpty()) Text("Cari nama...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    input()
                                })
                        }
                    }
                    FilledTonalButton(enabled = !loading, onClick = searchAction, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) { Text("Cari", style = MaterialTheme.typography.labelMedium) }
                } else {
                    OutlinedTextField(keyword, { keyword = it }, label = { Text("Cari nama / keterangan") }, modifier = Modifier.weight(1f), singleLine = true)
                    TextButton(enabled = !loading, onClick = searchAction) { Text("Cari") }
                }
            } }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = { request++ }) { Text("Coba Lagi") } } }
            if (!loading && error == null) {
                item { Text("${data.total} data", style = MaterialTheme.typography.labelLarge) }
                if (data.items.isEmpty()) item { Text("Belum ada data sesuai filter.", Modifier.padding(vertical = 32.dp)) }
                items(data.items) { row -> ManagementRecordCard(row, compact = reports) { selected = row } }
                if (!reports) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = page > 1, onClick = { page-- }) { Text("Sebelumnya") }
                    Text("${data.page} / ${data.lastPage}")
                    TextButton(enabled = page < data.lastPage, onClick = { page++ }) { Text("Berikutnya") }
                } }
            }
        }
    }
    if (filters) ModalBottomSheet(onDismissRequest = { filters = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.padding(horizontal = 20.dp).heightIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item { Text("Filter $title", style = MaterialTheme.typography.titleLarge) }
            if (module == "assets") item { ManagementPicker("Jenis Data", assetDataset, listOf(ManagementOption("assets", "Inventaris Karyawan"), ManagementOption("asset-requests", "Pengajuan Aset")), { assetDataset = it; status = "" }, allowAll = false) }
            if (reports) item { ManagementPicker("Jenis Report", report, options.options("reports"), { report = it; status = "" }, allowAll = false) }
            if (reports && report == "individual") item { ManagementPicker("Karyawan", employee, options.options("employees"), { employee = it }, allowAll = false) }
            if (module !in listOf("personal", "checkpoints", "announcements") && !(reports && report in listOf("attendance", "employees", "individual", "violations"))) item {
                ManagementPicker("Status", status, listOf("pending", "approved", "rejected", "cancelled", "todo", "in_progress", "review", "done", "calculated", "paid").map { ManagementOption(it, it.replace('_', ' ')) }, { status = it })
            }
            item { ManagementPicker("Perusahaan", company, options.options("companies"), { company = it; branch = "" }) }
            item { ManagementPicker("Cabang", branch, options.options("branches").filter { company.isBlank() || it.company == company }, { branch = it }) }
            item { ManagementPicker("Departemen", department, options.options("departments"), { department = it }) }
            item { DatePickerField(start, { start = it }, "Dari Tanggal", maxDate = end.ifBlank { null }) }
            item { DatePickerField(end, { end = it }, "Sampai Tanggal", minDate = start.ifBlank { null }) }
            if (!reports) item { TextButton(onClick = { start = ""; end = "" }) { Text("Semua Tanggal") } }
            item { Button(enabled = ready && !(reports && report == "individual" && employee.isBlank()), onClick = {
                applied = mapOf("company_id" to company, "branch_id" to branch, "department_id" to department, "start_date" to start, "end_date" to end, "search" to keyword, "status" to status, "employee_id" to if (reports && report == "individual") employee else "")
                activeReport = report; activeAssetDataset = assetDataset; page = 1; request++; filters = false
            }, modifier = Modifier.fillMaxWidth()) { Text("Terapkan Filter") } }
        }
    }
    selected?.let { row -> ManagementRecordSheet(row, { selected = null }, pdf = module == "payslips") }
}
