package com.jangeby.system.ui.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.ui.component.DatePickerField
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.theme.Background
import com.jangeby.system.utils.DateTimeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

private val chartColors = listOf(Color(0xFF169B87), Color(0xFFF4B942), Color(0xFF5887D8))
private data class DashboardDetail(val title: String, val path: String, val params: Map<String, String>)

@Composable
private fun ManagementChart(title: String, categories: List<String>, series: List<Pair<String, List<Float>>>, onPoint: (Int, Int) -> Unit) {
    val max = series.flatMap { it.second }.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            series.forEachIndexed { index, item -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).background(chartColors[index % chartColors.size], RoundedCornerShape(2.dp)))
                Text(item.first, style = MaterialTheme.typography.labelMedium)
            } }
            Text("Ketuk batang untuk melihat data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (categories.isEmpty()) Text("Belum ada data grafik.")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.forEachIndexed { categoryIndex, category -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.height(155.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        series.forEachIndexed { seriesIndex, item ->
                            val value = item.second.getOrElse(categoryIndex) { 0f }
                            Column(Modifier.width(28.dp).height(155.dp).clickable { onPoint(categoryIndex, seriesIndex) }, verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value.toInt().toString(), style = MaterialTheme.typography.labelSmall)
                                Box(Modifier.width(22.dp).height((value / max * 125).coerceAtLeast(3f).dp).background(chartColors[seriesIndex % chartColors.size], RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                            }
                        }
                    }
                    Text(category, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementDashboardScreen(onNavigateBack: () -> Unit) {
    var data by remember { mutableStateOf(JsonObject(emptyMap())) }
    var options by remember { mutableStateOf(JsonObject(emptyMap())) }
    var company by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(DateTimeUtils.todayDateString()) }
    var applied by remember { mutableStateOf(mapOf("date" to DateTimeUtils.todayDateString())) }
    var reload by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filters by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<DashboardDetail?>(null) }
    var detailPage by remember { mutableStateOf(1) }
    var detailData by remember { mutableStateOf(ManagementPage()) }
    var detailBusy by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<ManagementRow?>(null) }
    var detailRetry by remember { mutableStateOf(0) }
    fun open(title: String, path: String, params: Map<String, String> = emptyMap()) {
        detailData = ManagementPage(); detailPage = 1; detail = DashboardDetail(title, path, applied + params)
    }
    LaunchedEffect(reload) {
        loading = true; error = null
        try {
            if (options.isEmpty()) options = managementGet("options")
            data = managementGet("dashboard/indicators", applied)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { data = JsonObject(emptyMap()); error = e.message ?: "Dashboard belum dapat dimuat." }
        finally { loading = false }
    }
    LaunchedEffect(detail, detailPage, detailRetry) {
        val current = detail ?: return@LaunchedEffect
        detailBusy = true; detailError = null; detailData = ManagementPage()
        try { detailData = ApiClient.json.decodeFromJsonElement(managementGet(current.path, current.params + ("page" to detailPage.toString()))) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { detailError = e.message ?: "Rincian belum dapat dimuat." }
        finally { detailBusy = false }
    }
    Scaffold(containerColor = Background, topBar = { HrmsTopBar("Dashboard", onNavigateBack, actions = { TextButton(onClick = { filters = true }) { Text("Filter", color = Color.White) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                DatePickerField(selectedDate, { date ->
                    if (date.isNotBlank() && date != selectedDate) {
                        selectedDate = date
                        applied = applied + ("date" to date)
                        detail = null; selected = null; data = JsonObject(emptyMap()); reload++
                    }
                }, "Tanggal Dashboard", maxDate = DateTimeUtils.todayDateString())
            }
            item { Text("Ringkasan HRMS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = { reload++ }) { Text("Coba Lagi") } } }
            items(data["indicators"].arr().chunked(2)) { cards ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { cards.forEach { element ->
                    val card = element.obj()
                    OutlinedCard(onClick = {
                        if (card["key"].text() == "pending_approval") open("Pending Approval", "dashboard/transactions", mapOf("status" to "pending"))
                        else open(card["label"].text(), "dashboard/employees", mapOf("category" to card["filter_category"].text().ifBlank { if (card["key"].text() == "absent_today") "not_present_all" else "all" }))
                    }, modifier = Modifier.weight(1f)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(card["label"].text().replace("Hari Ini", "" ).trim(), style = MaterialTheme.typography.labelMedium)
                        Text(card["value"].text(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(card["trendText"].text(), style = MaterialTheme.typography.bodySmall)
                    } }
                } }
            }
            val statistics = data["statistics"].obj()
            val attendance = statistics["attendance"].obj()
            if (attendance.isNotEmpty()) item {
                ManagementChart("Kehadiran 7 Hari Terakhir", attendance["categories"].arr().map { it.text().take(3) }, attendance["series"].arr().map { it.obj()["name"].text() to it.obj()["data"].arr().map { n -> n.number() } }) { index, series ->
                    open("Kehadiran ${attendance["dates"].arr().getOrNull(index).text()}", "dashboard/attendance", mapOf("date" to attendance["dates"].arr().getOrNull(index).text(), "category" to when(series) { 0 -> "present_all"; 1 -> "late"; else -> "leave" }))
                }
            }
            val departments = statistics["departments"].obj()
            if (departments.isNotEmpty()) item { OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Distribusi Departemen", fontWeight = FontWeight.Bold)
                Text("Ketuk departemen untuk melihat karyawan", style = MaterialTheme.typography.bodySmall)
                val max = departments["series"].arr().maxOfOrNull { it.number() }?.coerceAtLeast(1f) ?: 1f
                departments["labels"].arr().forEachIndexed { index, label ->
                    val value = departments["series"].arr().getOrNull(index).number()
                    Column(Modifier.fillMaxWidth().clickable { open(label.text(), "dashboard/department", mapOf("department" to label.text())) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${label.text()} (${value.toInt()})", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(progress = { value / max }, modifier = Modifier.fillMaxWidth().height(10.dp), color = chartColors[0])
                    }
                }
            } } }
            val trends = statistics["trends"].obj()
            if (trends.isNotEmpty()) item {
                ManagementChart("Karyawan Masuk & Keluar", trends["categories"].arr().map { it.text() }, trends["series"].arr().map { it.obj()["name"].text() to it.obj()["data"].arr().map { n -> n.number() } }) { index, series ->
                    open("${if (series == 0) "Masuk" else "Keluar"} - ${trends["categories"].arr().getOrNull(index).text()}", "dashboard/trends", mapOf("month" to (index + 1).toString(), "year" to selectedDate.take(4), "type" to if (series == 0) "joins" else "departures"))
                }
            }
            val transactions = statistics["transaction_trends"].obj()
            if (transactions.isNotEmpty()) item {
                ManagementChart("Tren Pengajuan", transactions["categories"].arr().map { it.text() }, transactions["series"].arr().map { it.obj()["name"].text() to it.obj()["data"].arr().map { n -> n.number() } }) { index, series ->
                    open("Pengajuan ${transactions["categories"].arr().getOrNull(index).text()}", "dashboard/transactions", mapOf("month" to (index + 1).toString(), "year" to transactions["year"].text(), "type" to when(series) { 0 -> "leave_permission"; 1 -> "overtime"; else -> "loan" }))
                }
            }
            val payroll = data["payroll_summary"].obj()
            if (payroll.isNotEmpty()) item { OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ringkasan Payroll Terakhir", fontWeight = FontWeight.Bold)
                Text(payroll["title"].text()); Text(payroll["status"].text())
                Text("Rp ${(payroll["total_take_home"].text().toDoubleOrNull()?.toLong() ?: 0L).toString().reversed().chunked(3).joinToString(".").reversed()}", style = MaterialTheme.typography.titleLarge)
            } } }
            if (statistics["kpi"] != null && statistics["kpi"] !is JsonNull) item { Text("Rata-rata Skor KPI: ${statistics["kpi"].text()}", fontWeight = FontWeight.SemiBold) }
            if (data["recent_transactions"].arr().isNotEmpty()) item { Text("Pengajuan Terakhir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(data["recent_transactions"].arr()) { element -> val r = element.obj(); OutlinedCard(onClick = { open("Pengajuan Terakhir", "dashboard/transactions") }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(r["employee_name"].text(), fontWeight = FontWeight.SemiBold); Text(r["type"].text()); Text("${r["date"].text()} - ${r["status"].text()}", style = MaterialTheme.typography.bodySmall) } } }
            if (data["top_performers"].arr().isNotEmpty()) item { Text("Kinerja KPI Teratas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(data["top_performers"].arr()) { element -> val r = element.obj(); OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(r["name"].text(), fontWeight = FontWeight.SemiBold); Text("${r["position"].text()} - ${r["department"].text()}"); Text("Skor ${r["score"].text()}") } } }
        }
    }
    if (filters) ModalBottomSheet(onDismissRequest = { filters = false }) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Filter Dashboard", style = MaterialTheme.typography.titleLarge)
            ManagementPicker("Perusahaan", company, options.options("companies"), { company = it; branch = "" })
            ManagementPicker("Cabang", branch, options.options("branches").filter { company.isBlank() || it.company == company }, { branch = it })
            Button(onClick = { applied = mapOf("company_id" to company, "branch_id" to branch, "date" to selectedDate); filters = false; reload++ }, modifier = Modifier.fillMaxWidth()) { Text("Terapkan Filter") }
        }
    }
    detail?.let { current -> if (selected == null) ModalBottomSheet(onDismissRequest = { detail = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.padding(horizontal = 16.dp).heightIn(max = 680.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(current.title, style = MaterialTheme.typography.titleLarge); Text("${detailData.total} data", style = MaterialTheme.typography.bodySmall) }
            if (detailBusy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            detailError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = { detailRetry++ }) { Text("Coba Lagi") } } }
            if (!detailBusy && detailError == null && detailData.items.isEmpty()) item { Text("Tidak ada data pada pilihan ini.") }
            items(detailData.items) { row -> ManagementRecordCard(row) { selected = row } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(enabled = detailPage > 1 && !detailBusy, onClick = { detailPage-- }) { Text("Sebelumnya") }; TextButton(enabled = detailPage < detailData.lastPage && !detailBusy, onClick = { detailPage++ }) { Text("Berikutnya") } } }
        }
    } }
    selected?.let { ManagementRecordSheet(it, { selected = null }) }
}
