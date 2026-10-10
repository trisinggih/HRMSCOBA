package com.jangeby.system.ui.pengajuan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
    val tabs = listOf("Sakit", "Izin", "Tukar Hari", "Cuti")

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

    // Memuat ulang saldo cuti & riwayat cuti dari database saat tab Cuti (selectedTab == 3) dibuka
    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) {
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

            val currentType = when (selectedTab) {
                0 -> "sakit"
                1 -> "izin"
                2 -> "tukar_hari"
                else -> "cuti"
            }

            val activeList = when (selectedTab) {
                0 -> sakitList
                1 -> izinList
                2 -> tukarHariList
                else -> cutiList
            }

            val displayList = activeList

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // TAMPILKAN SALDO CUTI HANYA JIKA TAB CUTI (selectedTab == 3)
                if (selectedTab == 3) {
                    item {
                        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
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
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                PengajuanFormContent(
                    tabIndex = selectedTab,
                    tabName = tabs[selectedTab],
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
                .padding(bottom = 30.dp)
        ) {
            Text("Detail Pengajuan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            val uriHandler = LocalUriHandler.current
            Text("Judul: ${item.title}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Tanggal: ${item.date}", fontSize = 13.sp)
            Text("Alasan: ${item.description}", fontSize = 13.sp)
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
                    val stepOrder = step.order ?: (index + 1)
                    val isCurrentStep = stepOrder == (detail?.currentStep ?: 1) && 
                        (detail?.status == "pending" || detail?.status == "in_review")

                    // Find actual decision log, ignoring system transitions like escalate or submit
                    val log = detail?.logs?.findLast { 
                        (it.stepId == step.id || (step.order != null && it.stepId == step.order.toLong())) &&
                        it.action?.lowercase() !in listOf("escalate", "submit")
                    }
                    
                    val actionName = log?.action?.uppercase() ?: "PENDING"
                    val actionColor = when (log?.action?.lowercase()) {
                        "approve", "approved" -> Color(0xFF4CAF50)
                        "reject", "rejected" -> Color.Red
                        "cancel", "cancelled" -> Color.Gray
                        else -> if (isCurrentStep) Color(0xFFFFA000) else Color.LightGray
                    }
                    val expectedName = step.expectedApproverName ?: step.approverName ?: step.approverType ?: "Approver"
                    val actionLabel = if (log != null) {
                        "$actionName - ${log.approverName ?: expectedName}"
                    } else {
                        "PENDING - $expectedName"
                    }

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
                                    .background(actionColor)
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
                            Text(
                                text = actionLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            if (!log?.createdAt.isNullOrBlank()) {
                                Text(
                                    text = viewModel.formatDateDMY(log?.createdAt),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (!log?.note.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Catatan: ${log?.note}",
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
    val displayYear = year ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
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
    tabIndex: Int,
    tabName: String,
    viewModel: PengajuanViewModel,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
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

        when (tabIndex) {
            0 -> SakitFormContent(isLoading = isLoading) { start, end, reason, attachment -> viewModel.submitSakit(start, end, reason, attachment) }
            1 -> IzinFormContent(isLoading = isLoading) { type, date, start, end, reason, attachment -> viewModel.submitIzin(type, date, start, end, reason, attachment) }
            2 -> TukarHariFormContent(isLoading = isLoading) { workDate, swapDate, reason -> viewModel.submitTukarHari(workDate, swapDate, reason) }
            3 -> CutiFormContent(viewModel = viewModel, isLoading = isLoading) { leaveTypeId, start, end, reason, attachment -> viewModel.submitCuti(leaveTypeId, start, end, reason, attachment) }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kirimkan pengajuan sebelum jam 10:00 WIB", fontSize = 11.sp, color = Color(0xFF5B21B6))
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DatePickerField(
            value = startDate,
            onValueChange = { 
                startDate = it
                if (endDate.isBlank()) endDate = it
            },
            label = "Tanggal Mulai",
            modifier = Modifier.weight(1f)
        )
        DatePickerField(
            value = endDate,
            onValueChange = { endDate = it },
            label = "Tanggal Selesai",
            modifier = Modifier.weight(1f)
        )
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

    Button(
        onClick = { onSubmit(startDate, if (endDate.isNotBlank()) endDate else startDate, reason, attachment) },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = !isLoading && startDate.isNotEmpty() && reason.isNotEmpty() && attachment != null,
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
            Text("KIRIM PENGAJUAN SAKIT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IzinFormContent(
    isLoading: Boolean = false,
    onSubmit: (String, String, String, String, String, io.github.vinceglb.filekit.core.PlatformFile?) -> Unit
) {
    val permissionTypes = remember {
        listOf(
            "Izin Pribadi" to "personal",
            "Izin Dinas" to "business",
            "Urusan Keluarga" to "family",
            "Mendesak / Urgent" to "urgent",
            "Lainnya" to "other"
        )
    }
    var selectedTypePair by remember { mutableStateOf(permissionTypes.first()) }
    var expandedType by remember { mutableStateOf(false) }

    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var reason by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<io.github.vinceglb.filekit.core.PlatformFile?>(null) }

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
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    DatePickerField(
        value = date,
        onValueChange = { date = it },
        label = "Tanggal Izin",
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TimePickerField(
            value = startTime,
            onValueChange = { startTime = it },
            label = "Jam Mulai",
            modifier = Modifier.weight(1f)
        )
        TimePickerField(
            value = endTime,
            onValueChange = { endTime = it },
            label = "Jam Selesai",
            modifier = Modifier.weight(1f)
        )
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

    Button(
        onClick = { onSubmit(selectedTypePair.second, date, startTime, endTime, reason, attachment) },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = !isLoading && date.isNotEmpty() && reason.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty(),
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

    val isSameDate = workDate.isNotEmpty() && swapDate.isNotEmpty() && workDate == swapDate

    DatePickerField(
        value = workDate,
        onValueChange = { workDate = it },
        label = "Tanggal Kerja Asli",
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    DatePickerField(
        value = swapDate,
        onValueChange = { swapDate = it },
        label = "Ditukar Ke Tanggal",
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
        enabled = !isLoading && workDate.isNotEmpty() && swapDate.isNotEmpty() && reason.isNotEmpty() && !isSameDate,
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
                    val currentYear = selectedTypeItem.year ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
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

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DatePickerField(
            value = startDate,
            onValueChange = { 
                startDate = it
                if (endDate.isBlank()) endDate = it
            },
            label = "Tanggal Mulai",
            modifier = Modifier.weight(1f)
        )
        DatePickerField(
            value = endDate,
            onValueChange = { endDate = it },
            label = "Tanggal Selesai",
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

    Button(
        onClick = {
            onSubmit(selectedTypeItem.leaveTypeId, startDate, if (endDate.isNotBlank()) endDate else startDate, reason, attachment)
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = !isLoading && startDate.isNotEmpty() && reason.isNotEmpty(),
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
                Text(item.date, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.description, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(item.status)
        }
    }
}

