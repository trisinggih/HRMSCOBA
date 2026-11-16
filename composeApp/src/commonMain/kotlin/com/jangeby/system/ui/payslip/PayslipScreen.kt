package com.jangeby.system.ui.payslip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.data.remote.PayslipDetailItem
import com.jangeby.system.data.remote.PayslipItem
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipScreen(
    onNavigateBack: () -> Unit,
    viewModel: PayslipViewModel = koinViewModel()
) {
    val payslipList by viewModel.payslips.collectAsState()
    val selectedPayslip by viewModel.selectedPayslip.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val pdfLoading by viewModel.pdfLoading.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pdfSaver = rememberFileSaverLauncher { saved ->
        if (saved != null) scope.launch { snackbar.showSnackbar("PDF slip gaji berhasil disimpan") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            HrmsTopBar(
                title = "Slip Gaji",
                onNavigateBack = {
                    if (selectedPayslip != null) {
                        viewModel.clearSelectedPayslip()
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    selectedPayslip?.let { slip ->
                        TextButton(
                            enabled = !pdfLoading,
                            onClick = {
                                viewModel.downloadPdf(
                                    slip.id,
                                    onReady = { bytes ->
                                        pdfSaver.launch(bytes = bytes, baseName = "Slip_Gaji_${slip.id}", extension = "pdf")
                                    },
                                    onError = { message -> scope.launch { snackbar.showSnackbar(message) } }
                                )
                            }
                        ) {
                            if (pdfLoading) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Unduh PDF", color = Color.White)
                            }
                        }
                    }
                    IconButton(onClick = { selectedPayslip?.let { viewModel.selectPayslip(it) } ?: viewModel.loadPayslips() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (selectedPayslip != null) {
            // Detail view
            PayslipDetailView(
                payslip = selectedPayslip!!,
                modifier = Modifier.padding(padding),
                viewModel = viewModel,
                onClose = { viewModel.clearSelectedPayslip() }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(padding)
            ) {
                if (isLoading && payslipList.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                } else if (errorMessage != null && payslipList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Terjadi kesalahan",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPayslips() }) {
                            Text("Coba Lagi")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // Summary Card (Gaji Terakhir Diterima)
                        if (payslipList.isNotEmpty()) {
                            val latest = payslipList.first()
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Brush.horizontalGradient(listOf(Primary, PrimaryVariant)))
                                            .padding(20.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Gaji Terakhir Diterima",
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = viewModel.formatRupiah(latest.netSalary),
                                                color = Color.White,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = viewModel.formatPeriodTitle(latest),
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Riwayat Slip Gaji",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(payslipList) { payslip ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { viewModel.selectPayslip(payslip) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BgPayslip),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = PayslipIcon,
                                                contentDescription = null,
                                                tint = IconPayslip,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = viewModel.formatPeriodTitle(payslip),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = viewModel.formatRupiah(payslip.netSalary),
                                                fontWeight = FontWeight.Bold,
                                                color = Primary,
                                                fontSize = 15.sp
                                            )
                                            if (!payslip.period?.paymentDate.isNullOrBlank()) {
                                                Text(
                                                    text = "Dibayar: ${payslip.period?.paymentDate}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF6B7280)
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusChip(payslip.status ?: "approved")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = Color(0xFF9CA3AF)
                                        )
                                    }
                                }
                            }
                        }

                        if (!isLoading && payslipList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyState("Belum ada slip gaji yang selesai / disetujui")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PayslipDetailView(
    payslip: PayslipItem,
    viewModel: PayslipViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loading by viewModel.detailLoading.collectAsState()
    val error by viewModel.detailError.collectAsState()
    var showDaily by remember(payslip.id) { mutableStateOf(false) }
    val details = payslip.details.orEmpty()
    val earnings = details.filterNot(PayslipPresentation::isDeduction)
    val deductions = details.filter(PayslipPresentation::isDeduction).filter { kotlin.math.abs(it.amount ?: 0.0) > 0 }
    val attendance = PayslipPresentation.attendance(payslip)
    val employee = payslip.employee
    val company = payslip.company
    val overtime = PayslipPresentation.overtimeHours(payslip)
    val daily = payslip.attendanceLogs.orEmpty()
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Primary), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(company.text("name") ?: "Slip Gaji", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    company.text("branch_name")?.let { Text(it, color = Color.White.copy(alpha = 0.85f)) }
                    Text(viewModel.formatPeriodTitle(payslip), color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${payslip.period?.startDate ?: "-"} s/d ${payslip.period?.endDate ?: "-"}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    payslip.period?.paymentDate?.let { Text("Tanggal pembayaran: $it", color = Color.White, fontSize = 12.sp) }
                    Text("Gaji Bersih Diterima", color = Color.White, fontSize = 12.sp)
                    Text(viewModel.formatRupiah(payslip.netSalary), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    StatusChip(payslip.status ?: "-")
                }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error != null) item {
            Text(error.orEmpty(), color = Danger)
            TextButton(onClick = { viewModel.selectPayslip(payslip) }) { Text("Muat ulang detail") }
        }
        if (employee != null) item {
            PayslipSection("Informasi Karyawan") {
                PayslipValue("Nama", employee.text("name") ?: "-")
                PayslipValue("NIP", employee.text("employee_code") ?: "-")
                PayslipValue("Cabang", company.obj("branch").text("name") ?: employee.obj("branch").text("name") ?: "-")
                PayslipValue("Departemen", employee.obj("department").text("name") ?: "-")
                PayslipValue("Jabatan", employee.obj("position").text("name") ?: "-")
                PayslipValue("Status", when (val status = employee.text("employment_status")) {
                    "permanent" -> "Tetap"; "contract" -> "Kontrak"; "probation" -> "Percobaan"; "internship", "magang" -> "Magang"; else -> status ?: "-"
                })
                PayslipValue("Bergabung", employee.text("join_date") ?: "-")
            }
        }
        if (payslip.prorate?.isProrated == true) item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))) {
                Text("Penyesuaian masa kerja sejak ${payslip.prorate.joinDate ?: "tanggal bergabung"}.", modifier = Modifier.padding(14.dp), color = Color(0xFF92400E))
            }
        }
        if (attendance != null) item {
            PayslipSection("Kehadiran Periode") {
                PayslipValue("Total hari periode", "${PayslipPresentation.count(attendance.totalPeriodDays)} hari")
                PayslipValue("Hari kerja", "${PayslipPresentation.count(attendance.workDays)} hari")
                val counts = listOf(
                    "Masuk" to attendance.hadir,
                    "Libur" to attendance.libur,
                    "Cuti" to PayslipPresentation.leaveDays(attendance),
                    "Izin" to attendance.permit,
                    "Sakit" to attendance.sick,
                    "Alpa" to attendance.alpha
                )
                counts.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { (label, count) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(PayslipPresentation.count(count), fontWeight = FontWeight.Bold, color = if (label == "Alpa") Danger else Primary, fontSize = 20.sp)
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }
                if ((attendance.holiday ?: 0.0) > 0) Text("Masuk termasuk ${PayslipPresentation.count(attendance.holiday)} hari libur perusahaan.", fontSize = 12.sp, color = Color.Gray)
                val lateMinutes = attendance.lateMinutes ?: payslip.breakdown.obj("daily_rules").number("late_minutes")
                lateMinutes?.let { PayslipValue("Durasi keterlambatan", "${PayslipPresentation.count(it)} menit") }
                overtime?.let { PayslipValue("Jumlah lembur", "${PayslipPresentation.count(it)} jam") }
            }
        }
        if (!payslip.deductions.isNullOrEmpty()) item {
            PayslipSection("Informasi Potongan Lain-lain") {
                payslip.deductions.forEach { deduction ->
                    PayslipValue(deduction.text("title") ?: "Potongan lain-lain", viewModel.formatRupiah(deduction.number("amount")))
                    deduction.text("deduction_date")?.let { Text("Tanggal: ${it.take(10)}", fontSize = 12.sp, color = Color.Gray) }
                }
            }
        }
        item {
            PayslipSection("Penerimaan") {
                if (earnings.isEmpty()) Text("Rincian komponen belum tersedia.", color = Color.Gray)
                earnings.forEach { component ->
                    PayslipValue(component.label ?: "Pendapatan", viewModel.formatRupiah(component.amount))
                    if (component.label.orEmpty().contains("lembur", true) || component.label.orEmpty().contains("overtime", true)) {
                        overtime?.let { Text("Durasi: ${PayslipPresentation.count(it)} jam", fontSize = 12.sp, color = Color.Gray) }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                PayslipValue("Total Penerimaan", viewModel.formatRupiah(payslip.grossSalary), true)
            }
        }
        item {
            PayslipSection("Potongan") {
                deductions.forEach { component ->
                    PayslipValue(component.label ?: "Potongan", viewModel.formatRupiah(kotlin.math.abs(component.amount ?: 0.0)))
                    if (component.label.orEmpty().startsWith("Keterlambatan")) {
                        PayslipPresentation.lateFineEvents(payslip).forEach { event ->
                            Text(
                                "${event.text("date") ?: "-"} / ${PayslipPresentation.count(event.number("minutes"))} menit / ${viewModel.formatRupiah(event.number("amount"))}",
                                fontSize = 12.sp, color = Color.Gray
                            )
                        }
                    }
                }
                if (deductions.isEmpty()) {
                    if ((payslip.totalDeduction ?: 0.0) == 0.0) Text("Tidak ada potongan", color = Color.Gray)
                    else Text("Rincian komponen belum tersedia.", color = Color.Gray)
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                PayslipValue("Total Potongan", viewModel.formatRupiah(payslip.totalDeduction), true)
            }
        }
        item {
            PayslipSection("Ringkasan Gaji") {
                PayslipValue("Total Penerimaan", viewModel.formatRupiah(payslip.grossSalary))
                PayslipValue("Total Potongan", viewModel.formatRupiah(payslip.totalDeduction))
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                PayslipValue("Gaji Bersih Diterima", viewModel.formatRupiah(payslip.netSalary), true)
            }
        }
        if (daily.isNotEmpty()) item {
            OutlinedButton(onClick = { showDaily = !showDaily }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showDaily) "Sembunyikan Rincian Absensi" else "Rincian Absensi (${daily.size} hari)")
            }
        }
        if (showDaily) items(daily, key = { "attendance-${it.text("date")}" }) { day ->
            PayslipSection("${day.text("formatted_date") ?: day.text("date")} ? ${PayslipPresentation.status(day.text("status"))}") {
                PayslipValue("Shift", day.text("shift") ?: "-")
                PayslipValue("Masuk", day.text("clock_in") ?: "-")
                PayslipValue("Pulang", day.text("clock_out") ?: "-")
                if ((day.number("late_minutes") ?: 0.0) > 0) PayslipValue("Terlambat", "${PayslipPresentation.count(day.number("late_minutes"))} menit")
                if ((day.number("overtime_minutes") ?: 0.0) > 0) PayslipValue("Lembur", "${PayslipPresentation.count(day.number("overtime_minutes")?.div(60))} jam")
            }
        }
        item { TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Kembali ke Riwayat Slip") } }
    }
}

@Composable
private fun PayslipSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            content()
        }
    }
}

@Composable
private fun PayslipValue(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
