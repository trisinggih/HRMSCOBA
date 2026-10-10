package com.jangeby.system.ui.payslip

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

    Scaffold(
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
                    IconButton(onClick = { viewModel.loadPayslips() }) {
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
                                                imageVector = Icons.Default.List,
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
    onClose: () -> Unit
) {
    // Separate earnings vs deductions from details
    val rawDetails = payslip.details ?: emptyList()
    val earnings = rawDetails.filter {
        val t = it.type?.lowercase() ?: ""
        t == "earning" || t == "allowance" || t == "bonus" || t == "overtime" || (it.amount ?: 0.0) > 0 && !t.contains("deduct") && !t.contains("potong") && !t.contains("tax") && !t.contains("bpjs")
    }
    val deductions = rawDetails.filter {
        val t = it.type?.lowercase() ?: ""
        t == "deduction" || t == "tax" || t == "bpjs" || (it.amount ?: 0.0) < 0 || t.contains("potong") || t.contains("kasbon")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // Header detail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Primary, PrimaryVariant)))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onClose, contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Default.KeyboardArrowLeft, "Kembali", tint = Color.White)
                            Text("Kembali", color = Color.White, fontSize = 13.sp)
                        }
                        StatusChip(payslip.status ?: "approved")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.formatPeriodTitle(payslip),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!payslip.period?.startDate.isNullOrBlank() && !payslip.period?.endDate.isNullOrBlank()) {
                        Text(
                            text = "${payslip.period?.startDate} - ${payslip.period?.endDate}",
                            color = Color.White.copy(0.85f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Take Home Pay (Gaji Bersih)", color = Color.White.copy(0.75f), fontSize = 12.sp)
                    Text(
                        text = viewModel.formatRupiah(payslip.netSalary),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Attendance summary card if available
        if (payslip.attendance != null) {
            val att = payslip.attendance
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ringkasan Kehadiran",
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            AttendanceStatItem("Hadir", "${att.hadir ?: 0}", Color(0xFF16A34A))
                            AttendanceStatItem("Cuti/Izin", "${att.cuti ?: 0}", Color(0xFF0284C7))
                            AttendanceStatItem("Libur", "${att.libur ?: 0}", Color(0xFF6B7280))
                            AttendanceStatItem("Alpha", "${att.alpha ?: 0}", Color(0xFFDC2626))
                        }
                    }
                }
            }
        }

        // Pendapatan (Earnings)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pendapatan", fontWeight = FontWeight.Bold, color = Primary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (earnings.isNotEmpty()) {
                        earnings.forEach { d ->
                            InfoRow(d.label ?: "Komponen", viewModel.formatRupiah(d.amount ?: 0.0))
                        }
                    } else {
                        // Fallback fields from main run
                        val gross = payslip.grossSalary ?: 0.0
                        val allowance = payslip.totalAllowance ?: 0.0
                        val overtime = payslip.totalOvertime ?: 0.0
                        val basicEstimated = (gross - allowance - overtime).coerceAtLeast(0.0)
                        InfoRow("Gaji Pokok", viewModel.formatRupiah(basicEstimated))
                        if (allowance > 0) InfoRow("Total Tunjangan", viewModel.formatRupiah(allowance))
                        if (overtime > 0) InfoRow("Uang Lembur", viewModel.formatRupiah(overtime))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE5E7EB))
                    InfoRow("Total Pendapatan (Bruto)", viewModel.formatRupiah(payslip.grossSalary))
                }
            }
        }

        // Potongan (Deductions)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Potongan", fontWeight = FontWeight.Bold, color = Danger, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (deductions.isNotEmpty()) {
                        deductions.forEach { d ->
                            val amt = kotlin.math.abs(d.amount ?: 0.0)
                            InfoRow(d.label ?: "Potongan", "- ${viewModel.formatRupiah(amt)}")
                        }
                    } else {
                        val bpjs = payslip.totalBpjs ?: 0.0
                        val tax = payslip.totalTax ?: 0.0
                        val other = ((payslip.totalDeduction ?: 0.0) - bpjs - tax).coerceAtLeast(0.0)
                        if (bpjs > 0) InfoRow("BPJS", "- ${viewModel.formatRupiah(bpjs)}")
                        if (tax > 0) InfoRow("PPh 21", "- ${viewModel.formatRupiah(tax)}")
                        if (other > 0) InfoRow("Potongan Lainnya", "- ${viewModel.formatRupiah(other)}")
                        if (bpjs == 0.0 && tax == 0.0 && other == 0.0) {
                            InfoRow("Tidak ada potongan", "Rp 0")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE5E7EB))
                    InfoRow("Total Potongan", "- ${viewModel.formatRupiah(payslip.totalDeduction)}")
                }
            }
        }

        // Gaji Bersih Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Take Home Pay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Gaji Bersih Diterima", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                    Text(
                        text = viewModel.formatRupiah(payslip.netSalary),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(text = label, fontSize = 11.sp, color = Color(0xFF6B7280))
    }
}
