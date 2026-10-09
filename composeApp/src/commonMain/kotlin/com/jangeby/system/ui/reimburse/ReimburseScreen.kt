package com.jangeby.system.ui.reimburse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApprovalItem
import com.jangeby.system.data.remote.ReimburseItem
import com.jangeby.system.ui.component.DatePickerField
import com.jangeby.system.ui.component.EmptyState
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.component.StatusChip
import com.jangeby.system.ui.theme.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReimburseScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReimburseViewModel = koinInject()
) {
    val reimbursements by viewModel.reimbursements.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val trackingDetail by viewModel.trackingDetail.collectAsState()
    val isTrackingLoading by viewModel.isTrackingLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State dialog & sheets
    var showFormSheet by remember { mutableStateOf(false) }
    var itemToCancel by remember { mutableStateOf<ReimburseItem?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<ReimburseItem?>(null) }
    var selectedFilter by remember { mutableStateOf("Semua") }

    val filterOptions = listOf("Semua", "Menunggu", "Disetujui", "Ditolak", "Dibatalkan")

    // Filtered items
    val filteredList = remember(reimbursements, selectedFilter) {
        when (selectedFilter) {
            "Menunggu" -> reimbursements.filter { it.status.equals("pending", ignoreCase = true) }
            "Disetujui" -> reimbursements.filter { it.status.equals("approved", ignoreCase = true) }
            "Ditolak" -> reimbursements.filter { it.status.equals("rejected", ignoreCase = true) }
            "Dibatalkan" -> reimbursements.filter { it.status.equals("cancelled", ignoreCase = true) }
            else -> reimbursements
        }
    }

    // Summary calculation
    val totalApproved = remember(reimbursements) {
        reimbursements
            .filter { it.status.equals("approved", ignoreCase = true) }
            .sumOf { it.amount ?: 0.0 }
    }
    val countApproved = remember(reimbursements) {
        reimbursements.count { it.status.equals("approved", ignoreCase = true) }
    }
    val countPending = remember(reimbursements) {
        reimbursements.count { it.status.equals("pending", ignoreCase = true) }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            showFormSheet = false
        }
    }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Klaim / Reimburse",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadReimbursements() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFormSheet = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajukan Klaim",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
        ) {
            if (isLoading && reimbursements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (!errorMessage.isNullOrEmpty() && reimbursements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Gagal memuat data klaim",
                            color = Danger,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadReimbursements() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Ringkasan Total Klaim (Standard Card bergaya Saldo)
                    item {
                        ReimburseSummaryCard(
                            totalApproved = viewModel.formatRupiah(totalApproved),
                            countApproved = countApproved,
                            countPending = countPending
                        )
                    }

                    // Filter Chips Baris
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filterOptions) { filter ->
                                val isSelected = selectedFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(
                                            text = filter,
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

                    // Header List
                    item {
                        Text(
                            text = "Riwayat Klaim / Reimburse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    // List Pengajuan Klaim
                    if (filteredList.isEmpty()) {
                        item {
                            EmptyState(
                                if (selectedFilter == "Semua") "Belum ada riwayat pengajuan klaim"
                                else "Tidak ada klaim dengan status '$selectedFilter'"
                            )
                        }
                    } else {
                        items(filteredList, key = { it.id }) { item ->
                            ReimburseHistoryCard(
                                item = item,
                                formattedAmount = viewModel.formatRupiah(item.amount),
                                formattedDate = viewModel.formatDateDMY(item.date),
                                onClick = {
                                    selectedItemForDetail = item
                                    item.approval?.id?.let { approvalId ->
                                        viewModel.loadTracking(approvalId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // BottomSheet Form Pengajuan Klaim
        if (showFormSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFormSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                FormReimburseContent(
                    isSubmitting = isSubmitting,
                    onCancel = { showFormSheet = false },
                    onSubmit = { date, type, amount, desc, photo ->
                        viewModel.submitReimbursement(date, type, amount, desc, photo)
                    }
                )
            }
        }

        // BottomSheet Detail & Tracking Approval
        selectedItemForDetail?.let { item ->
            val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            DetailReimburseBottomSheet(
                item = item,
                formattedAmount = viewModel.formatRupiah(item.amount),
                formattedDate = viewModel.formatDateDMY(item.date),
                detail = trackingDetail,
                isTrackingLoading = isTrackingLoading,
                viewModel = viewModel,
                onDismiss = {
                    selectedItemForDetail = null
                    viewModel.clearTracking()
                },
                onCancelClick = {
                    itemToCancel = item
                },
                sheetState = detailSheetState
            )
        }

        // Dialog Konfirmasi Batalkan Klaim
        itemToCancel?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToCancel = null },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White,
                title = {
                    Text(
                        text = "Batalkan Klaim",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                },
                text = {
                    Text(
                        text = "Apakah Anda yakin ingin membatalkan pengajuan klaim ${item.type ?: ""} sebesar ${viewModel.formatRupiah(item.amount)}?",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            item.approval?.id?.let { approvalId ->
                                viewModel.cancelReimbursement(approvalId)
                            }
                            itemToCancel = null
                            selectedItemForDetail = null
                            viewModel.clearTracking()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ya, Batalkan", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { itemToCancel = null },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCCCCCC))
                    ) {
                        Text("Tidak", color = TextSecondary)
                    }
                }
            )
        }
    }
}

/**
 * Kartu Ringkasan Standar Pengajuan Klaim
 */
@Composable
private fun ReimburseSummaryCard(
    totalApproved: String,
    countApproved: Int,
    countPending: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Klaim Disetujui",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = totalApproved,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "$countApproved Disetujui",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menunggu Persetujuan",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "$countPending Pengajuan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Kartu Item Riwayat Klaim - Standar Bersih tanpa Ikon Berlebih
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReimburseHistoryCard(
    item: ReimburseItem,
    formattedAmount: String,
    formattedDate: String,
    onClick: () -> Unit
) {
    val hasAttachment = !item.attachment.isNullOrBlank() || item.approval?.attachmentUrl != null

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
                    Text(
                        text = "${item.type ?: "Klaim"} • $formattedAmount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    if (hasAttachment) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Ada Lampiran",
                            modifier = Modifier.size(14.dp),
                            tint = Primary
                        )
                    }
                }
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(item.status ?: "pending")
        }
    }
}

/**
 * BottomSheet Detail Klaim & Tracking Approval
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailReimburseBottomSheet(
    item: ReimburseItem,
    formattedAmount: String,
    formattedDate: String,
    detail: ApprovalItem?,
    isTrackingLoading: Boolean,
    viewModel: ReimburseViewModel,
    onDismiss: () -> Unit,
    onCancelClick: () -> Unit,
    sheetState: SheetState
) {
    val uriHandler = LocalUriHandler.current
    val effectiveAttachment = item.attachment ?: detail?.attachmentUrl

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
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detail Pengajuan Klaim",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Jenis Klaim: ${item.type ?: "-"}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Nominal: $formattedAmount", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Tanggal: $formattedDate", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Keterangan: ${item.description ?: "-"}", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Status: ${(item.status ?: "pending").uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            if (!effectiveAttachment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val baseUrl = ApiClient.BASE_URL.replace("api/v1/", "storage/")
                        val fullUrl = if (effectiveAttachment.startsWith("http")) {
                            effectiveAttachment
                        } else {
                            "$baseUrl${effectiveAttachment.removePrefix("/")}"
                        }
                        uriHandler.openUri(fullUrl)
                    },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Lihat Bukti", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lihat Bukti Lampiran", fontSize = 13.sp)
                }
            }

            val canCancel = (item.status?.lowercase() == "pending" || item.status?.lowercase() == "in_review") && item.approval != null
            if (canCancel) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                    border = BorderStroke(1.dp, Danger),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batalkan Pengajuan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Tracking Approval", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            if (isTrackingLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                val steps = detail?.workflow?.steps
                if (steps != null && steps.isNotEmpty()) {
                    steps.forEachIndexed { index, step ->
                        val isLast = index == steps.size - 1
                        val stepOrder = step.order ?: (index + 1)
                        val isCurrentStep = stepOrder == (detail.currentStep ?: 1) &&
                                (detail.status == "pending" || detail.status == "in_review")

                        // Filter log decision asli
                        val log = detail.logs?.findLast {
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
}

/**
 * BottomSheet Form Pengajuan Reimburse - Standar Pengajuan
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormReimburseContent(
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onSubmit: (date: String, type: String, amount: Double, desc: String, photo: PlatformFile?) -> Unit
) {
    var selectedDate by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Medical") }
    var rawAmount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<PlatformFile?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var expandedTypeDropdown by remember { mutableStateOf(false) }

    val reimburseTypes = listOf("Medical", "Travel", "Supplies", "Other")

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Pilih Bukti Kwitansi"
    ) { file ->
        file?.let { selectedPhoto = it }
    }

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
                text = "Form Pengajuan Klaim",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (validationError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEB)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = validationError ?: "",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Tanggal
        DatePickerField(
            value = selectedDate,
            onValueChange = {
                selectedDate = it
                validationError = null
            },
            label = "Tanggal Klaim / Transaksi",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Jenis Reimburse Dropdown
        ExposedDropdownMenuBox(
            expanded = expandedTypeDropdown,
            onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown }
        ) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Jenis Klaim", fontSize = 13.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = expandedTypeDropdown,
                onDismissRequest = { expandedTypeDropdown = false }
            ) {
                reimburseTypes.forEach { typeItem ->
                    DropdownMenuItem(
                        text = { Text(typeItem) },
                        onClick = {
                            selectedType = typeItem
                            expandedTypeDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nominal
        OutlinedTextField(
            value = rawAmount,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                rawAmount = filtered
                validationError = null
            },
            label = { Text("Nominal Klaim (Rp)", fontSize = 13.sp) },
            placeholder = { Text("Contoh: 150000", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (rawAmount.isNotEmpty()) {
            val formatted = "Rp " + rawAmount.reversed().chunked(3).joinToString(".").reversed()
            Text(
                text = "Estimasi: $formatted",
                fontSize = 12.sp,
                color = Primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keterangan
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                validationError = null
            },
            label = { Text("Keterangan / Keperluan", fontSize = 13.sp) },
            placeholder = { Text("Jelaskan rincian keperluan klaim...", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth().height(90.dp),
            maxLines = 3,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Upload Kwitansi
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { filePicker.launch() },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedPhoto == null) "Unggah Bukti Kwitansi (Opsional)" else "Terlampir: ${selectedPhoto?.name}",
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            if (selectedPhoto != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { selectedPhoto = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus lampiran", tint = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedDate.isBlank()) {
                    validationError = "Silakan pilih tanggal klaim"
                    return@Button
                }
                val parsedAmount = rawAmount.toDoubleOrNull() ?: 0.0
                if (parsedAmount <= 0) {
                    validationError = "Nominal klaim harus lebih dari 0"
                    return@Button
                }
                validationError = null
                onSubmit(selectedDate, selectedType, parsedAmount, description, selectedPhoto)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            enabled = !isSubmitting && selectedDate.isNotEmpty() && rawAmount.isNotEmpty()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("MENGIRIM...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KIRIM PENGAJUAN KLAIM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
