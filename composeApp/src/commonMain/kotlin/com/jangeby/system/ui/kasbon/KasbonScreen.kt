package com.jangeby.system.ui.kasbon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.jangeby.system.data.remote.LoanRequestItem
import com.jangeby.system.ui.component.EmptyState
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.component.StatusChip
import com.jangeby.system.ui.theme.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasbonScreen(
    onNavigateBack: () -> Unit,
    viewModel: KasbonViewModel = koinViewModel()
) {
    val loans by viewModel.loans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val trackingDetail by viewModel.trackingDetail.collectAsState()
    val isTrackingLoading by viewModel.isTrackingLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State modals & dialogs
    var showFormSheet by remember { mutableStateOf(false) }
    var itemToCancel by remember { mutableStateOf<LoanRequestItem?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<LoanRequestItem?>(null) }

    // Feedback messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            showFormSheet = false
            viewModel.loadLoans()
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Pengajuan Kasbon",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadLoans() }) {
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
                    contentDescription = "Ajukan Kasbon",
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
            if (isLoading && loans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (!errorMessage.isNullOrEmpty() && loans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Gagal memuat data kasbon",
                            color = Danger,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadLoans() },
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
                    item {
                        Text(
                            text = "Riwayat Kasbon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    if (loans.isEmpty()) {
                        item {
                            EmptyState("Belum ada riwayat pengajuan kasbon")
                        }
                    } else {
                        items(loans, key = { it.id }) { item ->
                            KasbonHistoryCard(
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

        // BottomSheet Form Pengajuan Kasbon
        if (showFormSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFormSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                FormKasbonContent(
                    isSubmitting = isSubmitting,
                    onCancel = { showFormSheet = false },
                    onSubmit = { amount, reason, photo ->
                        viewModel.submitLoan(amount, reason, photo)
                    }
                )
            }
        }

        // BottomSheet Detail Pengajuan & Tracking Approval Kasbon
        selectedItemForDetail?.let { item ->
            val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            DetailKasbonBottomSheet(
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

        // Dialog Konfirmasi Batalkan Pengajuan Kasbon
        itemToCancel?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToCancel = null },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White,
                title = {
                    Text(
                        text = "Batalkan Kasbon",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                text = {
                    Text(
                        text = "Apakah Anda yakin ingin membatalkan pengajuan kasbon sebesar ${viewModel.formatRupiah(item.amount)}?",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val approvalId = item.approval?.id
                            if (approvalId != null) {
                                viewModel.cancelLoan(approvalId)
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
 * Kartu Riwayat Pengajuan Kasbon - Seragam dengan PengajuanHistoryCard di PengajuanScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasbonHistoryCard(
    item: LoanRequestItem,
    formattedAmount: String,
    formattedDate: String,
    onClick: () -> Unit
) {
    val hasAttachment = !item.attachmentUrl.isNullOrBlank() || item.approval?.attachmentUrl != null

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
                        text = formattedAmount,
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
                if (!item.reason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.reason,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(item.status)
        }
    }
}

/**
 * BottomSheet Detail Kasbon & Tracking Approval - Standar ApprovalDetailBottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailKasbonBottomSheet(
    item: LoanRequestItem,
    formattedAmount: String,
    formattedDate: String,
    detail: ApprovalItem?,
    isTrackingLoading: Boolean,
    viewModel: KasbonViewModel,
    onDismiss: () -> Unit,
    onCancelClick: () -> Unit,
    sheetState: SheetState
) {
    val uriHandler = LocalUriHandler.current
    val effectiveAttachmentUrl = item.attachmentUrl ?: detail?.attachmentUrl

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
                    text = "Detail Pengajuan Kasbon",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Nominal: $formattedAmount", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Tanggal: $formattedDate", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Alasan: ${item.reason ?: "-"}", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Status: ${item.status.uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            if (!effectiveAttachmentUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val fullUrl = if (effectiveAttachmentUrl.startsWith("http")) {
                            effectiveAttachmentUrl
                        } else {
                            "${ApiClient.BASE_URL.replace("/api/v1/", "/storage/")}${effectiveAttachmentUrl.removePrefix("/")}"
                        }
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

            // Batalkan pengajuan jika status masih pending / in_review
            val canCancel = item.status.lowercase() in listOf("pending", "in_review") && item.approval != null
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

                        // Find actual decision log, ignoring system transitions like escalate or submit
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
 * BottomSheet Form Pengajuan Kasbon - Standar PengajuanFormContent
 */
@Composable
fun FormKasbonContent(
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onSubmit: (Double, String, PlatformFile?) -> Unit
) {
    var rawAmount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<PlatformFile?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Pilih Bukti Lampiran"
    ) { file ->
        file?.let { attachment = it }
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
                text = "Form Pengajuan Kasbon",
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

        OutlinedTextField(
            value = rawAmount,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                rawAmount = filtered
                validationError = null
            },
            label = { Text("Nominal Pinjaman (Rp)", fontSize = 13.sp) },
            placeholder = { Text("Contoh: 1000000", fontSize = 13.sp) },
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

        OutlinedTextField(
            value = reason,
            onValueChange = {
                reason = it
                validationError = null
            },
            label = { Text("Alasan Peminjaman", fontSize = 13.sp) },
            placeholder = { Text("Jelaskan keperluan kasbon...", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth().height(90.dp),
            maxLines = 3,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { filePicker.launch() },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (attachment == null) "Unggah Bukti Pendukung (Opsional)" else "Terlampir: ${attachment?.name}",
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
                val amountNum = rawAmount.toDoubleOrNull() ?: 0.0
                if (amountNum <= 0) {
                    validationError = "Harap masukkan nominal pinjaman yang valid"
                } else if (reason.trim().isEmpty()) {
                    validationError = "Harap isi alasan peminjaman"
                } else {
                    onSubmit(amountNum, reason.trim(), attachment)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isSubmitting && rawAmount.isNotEmpty() && reason.trim().isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MENGIRIM...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("KIRIM PENGAJUAN KASBON", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
