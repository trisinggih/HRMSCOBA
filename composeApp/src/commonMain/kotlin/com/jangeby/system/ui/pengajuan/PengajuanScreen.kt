package com.jangeby.system.ui.pengajuan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.jangeby.system.utils.DateTimeUtils

import androidx.compose.foundation.lazy.LazyRow
import com.jangeby.system.data.remote.ApprovalItem
import org.koin.compose.viewmodel.koinViewModel
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType

data class PengajuanHistoryItem(
    val id: Long,
    val type: String, // "sakit", "izin", "tukar_hari", "cuti"
    val title: String,
    val date: String,
    val description: String,
    val status: String,
    val approval: ApprovalItem? = null,
    val attachmentUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengajuanScreen(
    onNavigateBack: () -> Unit,
    viewModel: PengajuanViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    // Tab "Tukar Hari" sementara di-hide sesuai instruksi
    val tabs = listOf("Sakit", "Izin", "Cuti")

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedApprovalItem by remember { mutableStateOf<PengajuanHistoryItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    val state by viewModel.state.collectAsState()
    val saldoCutiItem by viewModel.saldoCuti.collectAsState()
    val leaveBalances by viewModel.leaveBalances.collectAsState()
    val isSaldoLoading by viewModel.isSaldoLoading.collectAsState()
    val sakitList by viewModel.sakitHistory.collectAsState()
    val izinList by viewModel.izinHistory.collectAsState()
    val tukarHariList by viewModel.tukarHariHistory.collectAsState()
    val cutiList by viewModel.cutiHistory.collectAsState()

    // Memuat seluruh data pengajuan & saldo cuti saat layar dibuka
    LaunchedEffect(Unit) {
        viewModel.loadAllData()
    }

    // Memuat ulang saldo cuti & riwayat cuti dari database saat tab Cuti dibuka
    LaunchedEffect(selectedTab) {
        if (tabs.getOrNull(selectedTab) == "Cuti") {
            viewModel.loadSaldoCuti()
            viewModel.loadCutiHistory()
        }
    }

    // Handle ViewModel State changes
    LaunchedEffect(state) {
        when (val currentState = state) {
            is PengajuanState.Success -> {
                showBottomSheet = false
                viewModel.loadAllData()
                snackbarHostState.showSnackbar(currentState.message)
                viewModel.resetState()
            }
            is PengajuanState.Error -> {
                if (!showBottomSheet) {
                    snackbarHostState.showSnackbar(currentState.message)
                    viewModel.resetState()
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            HrmsTopBar(title = "Pengajuan", onNavigateBack = onNavigateBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Pengajuan",
                    tint = Color.White
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            val currentTabName = tabs.getOrNull(selectedTab) ?: "Sakit"

            val currentType = when (currentTabName) {
                "Sakit" -> "sakit"
                "Izin" -> "izin"
                "Tukar Hari" -> "tukar_hari"
                else -> "cuti"
            }

            val activeList = when (currentTabName) {
                "Sakit" -> sakitList
                "Izin" -> izinList
                "Tukar Hari" -> tukarHariList
                else -> cutiList
            }

            val displayList = activeList

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // TAMPILKAN SALDO CUTI HANYA JIKA TAB CUTI
                if (currentTabName == "Cuti") {
                    item {
                        val currentYear = DateTimeUtils.nowInJakarta().year
                        SaldoCutiCard(
                            typeName = saldoCutiItem?.leaveTypeName ?: "Cuti Tahunan",
                            year = saldoCutiItem?.year ?: currentYear,
                            total = saldoCutiItem?.quota?.toInt() ?: 12,
                            used = saldoCutiItem?.used?.toInt() ?: 0,
                            remaining = saldoCutiItem?.remaining?.toInt() ?: (saldoCutiItem?.quota?.toInt() ?: 12),
                            isLoading = isSaldoLoading,
                            onRefresh = { viewModel.loadSaldoCuti() }
                        )
                    }

                    if (leaveBalances.size > 1) {
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(leaveBalances) { balance ->
                                    val isSelected = saldoCutiItem?.leaveTypeId == balance.leaveTypeId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectLeaveBalance(balance) },
                                        label = {
                                            Text(
                                                text = "${balance.leaveTypeName ?: "Cuti"} (${balance.remaining.toInt()})",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary.copy(alpha = 0.15f),
                                            selectedLabelColor = Primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Riwayat ${tabs[selectedTab]}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (displayList.isEmpty()) {
                    item {
                        EmptyState("Belum ada riwayat pengajuan ${tabs[selectedTab].lowercase()}")
                    }
                } else {
                    items(displayList) { item ->
                        PengajuanHistoryCard(item, onClick = {
                            selectedApprovalItem = item
                            item.approval?.id?.let { approvalId ->
                                viewModel.fetchApprovalDetails(approvalId)
                            }
                        })
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    viewModel.resetState()
                },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                PengajuanFormContent(
                    tabName = tabs.getOrNull(selectedTab) ?: "Sakit",
                    viewModel = viewModel,
                    onCancel = { showBottomSheet = false }
                )
            }
        }

        if (selectedApprovalItem != null) {
            val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ApprovalDetailBottomSheet(
                item = selectedApprovalItem!!,
                onDismiss = { selectedApprovalItem = null },
                viewModel = viewModel,
                sheetState = detailSheetState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalDetailBottomSheet(
    item: PengajuanHistoryItem,
    onDismiss: () -> Unit,
    viewModel: PengajuanViewModel,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
        ) {
            Text("Detail Pengajuan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            val uriHandler = LocalUriHandler.current
            Text("Judul: ${item.title}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(if (item.date.startsWith("Tanggal:", ignoreCase = true)) item.date else "Tanggal: ${item.date}", fontSize = 13.sp)
            Text(if (item.description.startsWith("Keterangan:", ignoreCase = true) || item.description.startsWith("Alasan:", ignoreCase = true)) item.description else "Keterangan: ${item.description}", fontSize = 13.sp)
            Text("Status: ${item.status.uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (item.attachmentUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { 
                        val fullUrl = if (item.attachmentUrl.startsWith("http")) item.attachmentUrl else "${com.jangeby.system.data.remote.ApiClient.BASE_URL.replace("/api/v1/", "/storage/")}${item.attachmentUrl.removePrefix("/")}"
                        uriHandler.openUri(fullUrl) 
                    },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Lihat Lampiran", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lihat Lampiran", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            val detail by viewModel.approvalDetails.collectAsState()
            
            Text("Tracking Approval", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            val steps = detail?.workflow?.steps
            if (steps != null && steps.isNotEmpty()) {
                steps.forEachIndexed { index, step ->
                    val isLast = index == steps.size - 1
                    val statusInfo = com.jangeby.system.utils.ApprovalUtils.resolveStepStatus(detail, step, index)

                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(statusInfo.statusColor)
                            )
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(Color.LightGray)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.padding(bottom = 24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tahap ${step.order ?: (index + 1)} • ${step.approverName ?: step.expectedApproverName ?: step.approverType ?: "Approver"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusInfo.statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = statusInfo.statusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusInfo.statusColor
                                    )
                                }
                            }
                            Text(
                                text = statusInfo.actorLine,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            if (!statusInfo.dateText.isNullOrBlank()) {
                                Text(
                                    text = viewModel.formatDateDMY(statusInfo.dateText),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (!statusInfo.note.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Catatan: ${statusInfo.note}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                if (detail == null) {
                    Text("Memuat data approval...", fontSize = 13.sp, color = TextSecondary)
                } else {
                    Text("Belum ada alur approval.", fontSize = 13.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun SaldoCutiCard(
    typeName: String = "Cuti Tahunan",
    year: Int? = null,
    total: Int = 12,
    used: Int = 0,
    remaining: Int = 12,
    isLoading: Boolean = false,
    onRefresh: (() -> Unit)? = null
) {
    val displayYear = year ?: DateTimeUtils.nowInJakarta().year
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Saldo $typeName",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            "Tahun $displayYear",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (onRefresh != null) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Saldo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            "$remaining Hari Tersisa",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Hak Cuti", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$total Hari", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Cuti Terpakai", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$used Hari", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sisa Cuti", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$remaining Hari", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PengajuanFormContent(
    tabName: String,
    viewModel: PengajuanViewModel,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 36.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Form Pengajuan $tabName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state is PengajuanState.Error) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEB)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = (state as PengajuanState.Error).message,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        val isLoading = state is PengajuanState.Loading

        when (tabName) {
            "Sakit" -> SakitFormContent(isLoading = isLoading) { start, end, reason, attachment -> viewModel.submitSakit(start, end, reason, attachment) }
            "Izin" -> IzinFormContent(isLoading = isLoading) { type, start, end, sTime, eTime, reason, attachment -> viewModel.submitIzin(type, start, end, sTime, eTime, reason, attachment) }
            "Tukar Hari" -> TukarHariFormContent(isLoading = isLoading) { workDate, swapDate, reason -> viewModel.submitTukarHari(workDate, swapDate, reason) }
            else -> CutiFormContent(viewModel = viewModel, isLoading = isLoading) { leaveTypeId, start, end, reason, attachment -> viewModel.submitCuti(leaveTypeId, start, end, reason, attachment) }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SakitFormContent(
    isLoading: Boolean = false,
    onSubmit: (String, String, String, io.github.vinceglb.filekit.core.PlatformFile?) -> Unit
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<io.github.vinceglb.filekit.core.PlatformFile?>(null) }

    val nowJakarta = DateTimeUtils.nowInJakarta()
    val todayStr = nowJakarta.date.toString()
    val isAfter10Am = (nowJakarta.hour > 10) || (nowJakarta.hour == 10 && (nowJakarta.minute > 0 || nowJakarta.second > 0))
    val isDateBlocked = isAfter10Am && startDate.isNotBlank() && startDate <= todayStr

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAfter10Am) Color(0xFFFEE2E2) else Color(0xFFF3E8FF)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isAfter10Am) Icons.Default.Warning else Icons.Default.Info,
                null,
                tint = if (isAfter10Am) Color(0xFFDC2626) else Color(0xFF7C3AED),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    if (isAfter10Am) "Batas Waktu Pengajuan Berakhir" else "Ketentuan Pengajuan Izin Sakit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAfter10Am) Color(0xFF991B1B) else Color(0xFF5B21B6)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (isAfter10Am) "Pengajuan izin sakit untuk hari ini maksimal diajukan sebelum pukul 10:00 WIB."
                    else "Kirimkan pengajuan sakit sebelum jam 10:00 WIB.",
                    fontSize = 11.sp,
                    color = if (isAfter10Am) Color(0xFFB91C1C) else Color(0xFF6D28D9),
                    lineHeight = 15.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DatePickerField(
            value = startDate,
            onValueChange = { 
                startDate = it
                if (endDate.isBlank() || endDate < it) endDate = it
            },
            label = "Tanggal Mulai",
            minDate = todayStr,
            modifier = Modifier.weight(1f)
        )
        DatePickerField(
            value = endDate,
            onValueChange = { 
                if (startDate.isNotBlank() && it < startDate) {
                    endDate = startDate
                } else {
                    endDate = it
                }
            },
            label = "Tanggal Selesai",
            minDate = if (startDate.isNotBlank()) startDate else todayStr,
            modifier = Modifier.weight(1f)
        )
    }

    if (isDateBlocked) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Tidak dapat mengajukan sakit hari ini (lewat pukul 10:00 WIB)",
                color = Color(0xFFDC2626),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = reason,
        onValueChange = { reason = it },
        label = { Text("Keterangan / Gejala Sakit", fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().height(90.dp),
        maxLines = 3,
        shape = RoundedCornerShape(10.dp)
    )
    
    Spacer(modifier = Modifier.height(12.dp))

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Pilih Bukti Lampiran"
    ) { file ->
        file?.let { attachment = it }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { filePicker.launch() }, 
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (attachment == null) "Unggah Surat Dokter (Wajib)" else "Terlampir: ${attachment?.name}",
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        if (attachment != null) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { attachment = null }) {
                Icon(Icons.Default.Close, contentDescription = "Hapus lampiran", tint = Color.Red)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val isInvalidEndDate = startDate.isNotBlank() && endDate.isNotBlank() && endDate < startDate
    val isSubmitEnabled = !isLoading && !isDateBlocked && !isInvalidEndDate && startDate.isNotEmpty() && reason.isNotEmpty() && attachment != null

    Button(
        onClick = { onSubmit(startDate, if (endDate.isNotBlank()) endDate else startDate, reason, attachment) },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = isSubmitEnabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDateBlocked) Color(0xFFDC2626) else Primary,
            disabledContainerColor = if (isDateBlocked) Color(0xFFFECACA) else Color.LightGray
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MENGIRIM...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else if (isDateBlocked) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp), tint = Color(0xFF991B1B))
            Spacer(modifier = Modifier.width(8.dp))
            Text("LEWAT BATAS JAM 10:00 WIB", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF991B1B))
        } else {
            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("KIRIM PENGAJUAN SAKIT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IzinFormContent(
    isLoading: Boolean = false,
    onSubmit: (String, String, String, String, String, String, io.github.vinceglb.filekit.core.PlatformFile?) -> Unit
) {
    val permissionTypes = remember {
        listOf(
            "Izin Pribadi" to "personal",
            "Izin Perjalanan Dinas" to "business",
            "Izin Keluar" to "out_of_office",
            "Izin Pulang Awal" to "early_leave"
        )
    }
    var selectedTypePair by remember { mutableStateOf(permissionTypes.first()) }
    var expandedType by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var reason by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<io.github.vinceglb.filekit.core.PlatformFile?>(null) }

    val todayDate = DateTimeUtils.nowInJakarta().date
    val todayStr = todayDate.toString()

    val isExitPermit = selectedTypePair.second == "out_of_office"
    val isBusinessTrip = selectedTypePair.second == "business"
    val isEarlyLeave = selectedTypePair.second == "early_leave"

    LaunchedEffect(selectedTypePair) {
        if (isExitPermit || isEarlyLeave) {
            startDate = todayStr
            endDate = todayStr
            startTime = if (isEarlyLeave) "" else "08:00"
            endTime = if (isEarlyLeave) "" else "17:00"
        } else {
            if (startDate < todayStr) {
                startDate = todayStr
            }
            if (endDate < startDate) {
                endDate = startDate
            }
        }
    }

    // Advisory Note Card
    Surface(
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp).padding(top = 1.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Ketentuan Pengajuan Izin",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E40AF)
                )
                Text(
                    text = when {
                        isEarlyLeave -> "Izin pulang awal berlaku untuk hari ini. Pilih jam akhir / jam pulang yang diajukan."
                        isExitPermit -> "• Khusus Izin Keluar hanya dapat diajukan untuk hari ini.\n• Wajib menentukan jam keluar dan jam kembali."
                        isBusinessTrip -> "• Izin Perjalanan Dinas dapat diajukan mulai hari ini.\n• Dapat diajukan untuk satu hari atau beberapa hari (rentang tanggal).\n• Memungkinkan presensi bebas lokasi setelah disetujui."
                        else -> "• Izin Pribadi dapat diajukan mulai hari ini.\n• Dapat diajukan untuk satu hari atau beberapa hari (rentang tanggal)."
                    },
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF1E3A8A)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    ExposedDropdownMenuBox(
        expanded = expandedType,
        onExpandedChange = { expandedType = !expandedType }
    ) {
        OutlinedTextField(
            value = selectedTypePair.first,
            onValueChange = {},
            readOnly = true,
            label = { Text("Jenis Izin", fontSize = 13.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
            modifier = Modifier.fillMaxWidth().menuAnchor().height(56.dp),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expandedType,
            onDismissRequest = { expandedType = false }
        ) {
            permissionTypes.forEach { pair ->
                DropdownMenuItem(
                    text = { Text(pair.first) },
                    onClick = {
                        selectedTypePair = pair
                        expandedType = false
                        if (pair.second in listOf("out_of_office", "early_leave")) {
                            startDate = todayStr
                            endDate = todayStr
                        } else {
                            if (startDate.isNotBlank() && startDate < todayStr) {
                                startDate = todayStr
                            }
                            if (endDate.isNotBlank() && endDate < startDate) {
                                endDate = startDate
                            }
                        }
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (isEarlyLeave) {
        OutlinedTextField(
            value = DateTimeUtils.formatDateOnlyDMY(todayStr),
            onValueChange = {},
            readOnly = true,
            label = { Text("Tanggal Hari Ini") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TimePickerField(value = endTime, onValueChange = { endTime = it }, label = "Jam Akhir / Jam Pulang")
    } else if (isExitPermit) {
        // Tanggal Izin Keluar (1 hari - hari ini)
        DatePickerField(
            value = startDate,
            onValueChange = { 
                startDate = it
                endDate = it
            },
            label = "Tanggal Izin Keluar (Hanya Hari Ini)",
            minDate = todayStr,
            maxDate = todayStr,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Jam Keluar & Jam Kembali
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimePickerField(
                value = startTime,
                onValueChange = { startTime = it },
                label = "Jam Keluar",
                modifier = Modifier.weight(1f)
            )
            TimePickerField(
                value = endTime,
                onValueChange = { endTime = it },
                label = "Jam Kembali",
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        // Range Tanggal (Start Date minimal hari ini dan To Date)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DatePickerField(
                value = startDate,
                onValueChange = { 
                    startDate = it
                    if (endDate.isBlank() || endDate < it) {
                        endDate = it
                    }
                },
                label = "Tanggal Mulai",
                minDate = todayStr,
                modifier = Modifier.weight(1f)
            )
            DatePickerField(
                value = endDate,
                onValueChange = { 
                    if (startDate.isNotBlank() && it < startDate) {
                        endDate = startDate
                    } else {
                        endDate = it
                    }
                },
                label = "Tanggal Selesai",
                minDate = if (startDate.isNotBlank()) startDate else todayStr,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = reason,
        onValueChange = { reason = it },
        label = { Text("Alasan Izin", fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().height(90.dp),
        maxLines = 3,
        shape = RoundedCornerShape(10.dp)
    )
    
    Spacer(modifier = Modifier.height(12.dp))

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Pilih Bukti Lampiran"
    ) { file ->
        file?.let { attachment = it }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { filePicker.launch() },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (attachment == null) "Unggah Bukti Pendukung (Opsional)" else "Terlampir: ${attachment?.name}",
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        if (attachment != null) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { attachment = null }) {
                Icon(Icons.Default.Close, contentDescription = "Hapus lampiran", tint = Color.Red)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val isDateValid = if (isEarlyLeave) {
        endTime.isNotBlank()
    } else if (isExitPermit) {
        startDate == todayStr && startTime.isNotBlank() && endTime.isNotBlank() && endTime > startTime
    } else {
        startDate.isNotBlank() && endDate.isNotBlank() && startDate >= todayStr && endDate >= startDate
    }

    Button(
        onClick = { 
            val finalStart = if (isEarlyLeave) DateTimeUtils.nowInJakarta().date.toString() else startDate
            val finalEnd = if (isEarlyLeave || isExitPermit || endDate.isBlank()) finalStart else endDate
            val finalStartTime = if (isEarlyLeave) "" else if (isExitPermit) startTime else "08:00"
            val finalEndTime = if (isEarlyLeave || isExitPermit) endTime else "17:00"
            onSubmit(selectedTypePair.second, finalStart, finalEnd, finalStartTime, finalEndTime, reason, attachment) 
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = !isLoading && isDateValid && reason.isNotBlank(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MENGIRIM...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else {
            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("KIRIM PENGAJUAN IZIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun TukarHariFormContent(
    isLoading: Boolean = false,
    onSubmit: (String, String, String) -> Unit
) {
    var workDate by remember { mutableStateOf("") }
    var swapDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    val todayDate = DateTimeUtils.nowInJakarta().date
    val tomorrowStr = todayDate.plus(DatePeriod(days = 1)).toString()

    val isSameDate = workDate.isNotEmpty() && swapDate.isNotEmpty() && workDate == swapDate

    // Advisory Note Card
    Surface(
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp).padding(top = 1.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Ketentuan Tukar Hari Kerja",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E40AF)
                )
                Text(
                    text = "• Pengajuan tukar hari wajib dilakukan minimal H-1.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF1E3A8A)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    DatePickerField(
        value = workDate,
        onValueChange = { workDate = it },
        label = "Tanggal Kerja Asli (Minimal Besok)",
        minDate = tomorrowStr,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    DatePickerField(
        value = swapDate,
        onValueChange = { swapDate = it },
        label = "Ditukar Ke Tanggal (Minimal Besok)",
        minDate = tomorrowStr,
        modifier = Modifier.fillMaxWidth()
    )

    if (isSameDate) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tanggal tujuan tukar harus berbeda dengan tanggal kerja asli",
            color = Color.Red,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = reason,
        onValueChange = { reason = it },
        label = { Text("Alasan Tukar Hari", fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().height(90.dp),
        maxLines = 3,
        shape = RoundedCornerShape(10.dp)
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { onSubmit(workDate, swapDate, reason) },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = !isLoading && workDate.isNotEmpty() && swapDate.isNotEmpty() && workDate >= tomorrowStr && swapDate >= tomorrowStr && reason.isNotEmpty() && !isSameDate,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MENGIRIM...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else {
            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("KIRIM PENGAJUAN TUKAR HARI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutiFormContent(
    viewModel: PengajuanViewModel,
    isLoading: Boolean = false,
    onSubmit: (Long, String, String, String, io.github.vinceglb.filekit.core.PlatformFile?) -> Unit
) {
    val leaveBalances by viewModel.leaveBalances.collectAsState()

    LaunchedEffect(Unit) {
        if (leaveBalances.isEmpty()) {
            viewModel.loadSaldoCuti()
        }
    }

    val fallbackBalances = remember {
        listOf(
            com.jangeby.system.data.remote.LeaveBalanceItem(
                leaveTypeId = 1L,
                leaveTypeName = "Cuti Tahunan",
                leaveTypeCode = "CT",
                quota = 12.0,
                used = 0.0,
                remaining = 12.0
            ),
            com.jangeby.system.data.remote.LeaveBalanceItem(
                leaveTypeId = 2L,
                leaveTypeName = "Cuti Sakit",
                leaveTypeCode = "CS",
                quota = 0.0,
                used = 0.0,
                remaining = 0.0
            ),
            com.jangeby.system.data.remote.LeaveBalanceItem(
                leaveTypeId = 3L,
                leaveTypeName = "Cuti Melahirkan",
                leaveTypeCode = "CM",
                quota = 90.0,
                used = 0.0,
                remaining = 90.0
            ),
            com.jangeby.system.data.remote.LeaveBalanceItem(
                leaveTypeId = 4L,
                leaveTypeName = "Cuti Alasan Penting",
                leaveTypeCode = "CAP",
                quota = 45.0,
                used = 0.0,
                remaining = 45.0
            )
        )
    }

    val activeList = if (leaveBalances.isNotEmpty()) leaveBalances else fallbackBalances
    var selectedTypeItem by remember(activeList) {
        mutableStateOf(activeList.firstOrNull { it.leaveTypeCode == "CT" } ?: activeList.first())
    }
    var expandedType by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<io.github.vinceglb.filekit.core.PlatformFile?>(null) }

    val todayDate = DateTimeUtils.nowInJakarta().date
    val tomorrowStr = todayDate.plus(DatePeriod(days = 1)).toString()

    // Card Informasi Saldo Cuti dari Database
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Informasi Saldo Cuti",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.15f)
                ) {
                    val currentYear = selectedTypeItem.year ?: DateTimeUtils.nowInJakarta().year
                    Text(
                        "Tahun $currentYear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Primary.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hak Kuota", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${selectedTypeItem.quota.toInt()} Hari", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(1.dp)
                        .background(Primary.copy(alpha = 0.2f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Terpakai", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${selectedTypeItem.used.toInt()} Hari", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(1.dp)
                        .background(Primary.copy(alpha = 0.2f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sisa Saldo", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${selectedTypeItem.remaining.toInt()} Hari",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTypeItem.remaining > 0) Primary else Color.Red
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    ExposedDropdownMenuBox(
        expanded = expandedType,
        onExpandedChange = { expandedType = !expandedType }
    ) {
        OutlinedTextField(
            value = "${selectedTypeItem.leaveTypeName ?: "Cuti"} (Sisa: ${selectedTypeItem.remaining.toInt()} hari)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Jenis Cuti") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
            modifier = Modifier.fillMaxWidth().menuAnchor().height(56.dp),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expandedType,
            onDismissRequest = { expandedType = false }
        ) {
            activeList.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.leaveTypeName ?: "Cuti", fontWeight = FontWeight.Medium)
                            Text("Sisa: ${item.remaining.toInt()} hari", color = Primary, fontSize = 12.sp)
                        }
                    },
                    onClick = {
                        selectedTypeItem = item
                        expandedType = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Advisory Note Card
    Surface(
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp).padding(top = 1.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Ketentuan Pengajuan Cuti",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E40AF)
                )
                Text(
                    text = "• Pengajuan cuti wajib diajukan minimal H-1 (mulai dari besok).\n• Tanggal selesai tidak boleh sebelum tanggal mulai.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF1E3A8A)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DatePickerField(
            value = startDate,
            onValueChange = { 
                startDate = it
                if (endDate.isBlank() || endDate < it) endDate = it
            },
            label = "Tanggal Mulai",
            minDate = tomorrowStr,
            modifier = Modifier.weight(1f)
        )
        DatePickerField(
            value = endDate,
            onValueChange = { 
                if (startDate.isNotBlank() && it < startDate) {
                    endDate = startDate
                } else {
                    endDate = it
                }
            },
            label = "Tanggal Selesai",
            minDate = if (startDate.isNotBlank()) startDate else tomorrowStr,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = reason,
        onValueChange = { reason = it },
        label = { Text("Alasan Cuti", fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().height(90.dp),
        maxLines = 3,
        shape = RoundedCornerShape(10.dp)
    )
    
    Spacer(modifier = Modifier.height(12.dp))

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Pilih Bukti Lampiran"
    ) { file ->
        file?.let { attachment = it }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { filePicker.launch() }, 
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (attachment == null) "Unggah Bukti Pendukung (Opsional)" else "Terlampir: ${attachment?.name}",
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        if (attachment != null) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { attachment = null }) {
                Icon(Icons.Default.Close, contentDescription = "Hapus lampiran", tint = Color.Red)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val isDatesValid = startDate.isNotEmpty() && startDate >= tomorrowStr && (endDate.isEmpty() || endDate >= startDate)
    val isSubmitEnabled = !isLoading && isDatesValid && reason.isNotEmpty()

    Button(
        onClick = {
            onSubmit(selectedTypeItem.leaveTypeId, startDate, if (endDate.isNotBlank()) endDate else startDate, reason, attachment)
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = isSubmitEnabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MENGIRIM...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else {
            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("KIRIM PENGAJUAN CUTI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengajuanHistoryCard(item: PengajuanHistoryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    if (item.attachmentUrl != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Info, contentDescription = "Ada Lampiran", modifier = Modifier.size(14.dp), tint = Primary)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val dateDisplay = if (item.date.startsWith("Tanggal:", ignoreCase = true)) item.date else "Tanggal: ${item.date}"
                Text(dateDisplay, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                val descDisplay = if (item.description.startsWith("Keterangan:", ignoreCase = true) || item.description.startsWith("Alasan:", ignoreCase = true)) item.description else "Keterangan: ${item.description}"
                Text(descDisplay, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(item.status)
        }
    }
}

