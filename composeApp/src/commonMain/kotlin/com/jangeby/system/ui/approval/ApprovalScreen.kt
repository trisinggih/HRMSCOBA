package com.jangeby.system.ui.approval

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jangeby.system.data.remote.ApprovalItem
import com.jangeby.system.data.remote.ApprovalLog
import com.jangeby.system.data.remote.ApprovalStep
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ApprovalViewModel = koinInject()
) {
    val approvals by viewModel.approvals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val badgeCount by viewModel.badgeCount.collectAsState()
    val trackingItem by viewModel.trackingItem.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    // Dialog state for Approve
    var itemToApprove by remember { mutableStateOf<ApprovalItem?>(null) }
    var approveNote by remember { mutableStateOf("") }
    var approveTenor by remember { mutableStateOf("") }

    // Dialog state for Reject
    var itemToReject by remember { mutableStateOf<ApprovalItem?>(null) }
    var rejectNote by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Persetujuan",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (approvals.isNotEmpty() || badgeCount > 0) {
                        val count = if (approvals.isNotEmpty()) approvals.size else badgeCount
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (count > 99) "99+" else count.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
        ) {
            // Body Content
            if (isLoading && approvals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (!errorMessage.isNullOrEmpty() && approvals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Terjadi kesalahan",
                            color = Danger,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadApprovals() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(approvals, key = { it.id }) { item ->
                        ApprovalCardItem(
                            item = item,
                            onApproveClick = {
                                itemToApprove = item
                                approveNote = ""
                                approveTenor = ""
                            },
                            onRejectClick = {
                                itemToReject = item
                                rejectNote = ""
                            },
                            onTrackingClick = {
                                viewModel.loadTracking(item.id)
                            },
                            onOpenAttachment = { url ->
                                uriHandler.openUri(url)
                            }
                        )
                    }

                    if (approvals.isEmpty()) {
                        item {
                            EmptyState(message = "Tidak ada pengajuan yang membutuhkan persetujuan")
                        }
                    }
                }
            }
        }
    }

    // Modal BottomSheet for Tracking Workflow
    if (trackingItem != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearTracking() },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            TrackingBottomSheetContent(
                item = trackingItem!!,
                onClose = { viewModel.clearTracking() }
            )
        }
    }

    // Dialog for Approve
    if (itemToApprove != null) {
        val currentApproveItem = itemToApprove!!
        val isKasbon = currentApproveItem.module?.contains("Loan", ignoreCase = true) == true ||
                currentApproveItem.module?.contains("Kasbon", ignoreCase = true) == true

        AlertDialog(
            onDismissRequest = {
                if (!isActionLoading) itemToApprove = null
            },
            title = {
                Text("Persetujuan", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Masukkan catatan" + if (isKasbon) " dan tenor cicilan." else ".",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = approveNote,
                        onValueChange = { approveNote = it },
                        label = { Text("Catatan (Opsional)") },
                        placeholder = { Text("Tambahkan catatan bila perlu...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )
                    if (isKasbon) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = approveTenor,
                            onValueChange = { approveTenor = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tenor Cicilan (Bulan)") },
                            placeholder = { Text("Berapa kali cicilan (bulan)?") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tenorInt = approveTenor.toIntOrNull()
                        viewModel.approveRequest(
                            id = currentApproveItem.id,
                            note = approveNote,
                            tenor = tenorInt
                        ) { success, msg ->
                            itemToApprove = null
                            scope.launch {
                                snackbarHostState.showSnackbar(msg ?: if (success) "Pengajuan disetujui" else "Gagal")
                            }
                        }
                    },
                    enabled = !isActionLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Setujui", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToApprove = null },
                    enabled = !isActionLoading
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog for Reject
    if (itemToReject != null) {
        val currentRejectItem = itemToReject!!

        AlertDialog(
            onDismissRequest = {
                if (!isActionLoading) itemToReject = null
            },
            title = {
                Text("Tolak Pengajuan", fontWeight = FontWeight.Bold, color = Danger)
            },
            text = {
                Column {
                    Text(
                        text = "Tambahkan alasan kenapa pengajuan ini ditolak.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectNote,
                        onValueChange = { rejectNote = it },
                        label = { Text("Alasan Penolakan (Opsional)") },
                        placeholder = { Text("Tuliskan alasan penolakan...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectRequest(
                            id = currentRejectItem.id,
                            note = rejectNote
                        ) { success, msg ->
                            itemToReject = null
                            scope.launch {
                                snackbarHostState.showSnackbar(msg ?: if (success) "Pengajuan ditolak" else "Gagal")
                            }
                        }
                    },
                    enabled = !isActionLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Tolak", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToReject = null },
                    enabled = !isActionLoading
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ApprovalCardItem(
    item: ApprovalItem,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onOpenAttachment: (String) -> Unit
) {
    val photoUrl = item.requester?.photo?.let {
        if (it.startsWith("http")) it else "https://jangeby.rjpteknologi.com/storage/$it"
    }
    val dateDisplay = formatDisplayDate(item.createdAt)
    val parsedDesc = item.parsedDescription

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // HEADER: Profile Pic, Name, Date, Options Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto Pemohon",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.requester?.name ?: "Karyawan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = dateDisplay,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = onTrackingClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Detail Tracking",
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFF1F5F9)
            )

            // BODY: Module & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modul: ${item.moduleLabel}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                StatusChip(status = item.status ?: "pending")
            }

            // Description / Payload info
            if (parsedDesc.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = parsedDesc,
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
            }

            // Attachment view
            if (!item.attachmentUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAttachment(item.attachmentUrl) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Lampiran",
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lihat Lampiran Berkas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Primary
                        )
                    }
                }
            }

            // Action Buttons for Pending items
            val isPending = item.status?.equals("pending", ignoreCase = true) == true ||
                    item.status?.equals("in_review", ignoreCase = true) == true

            if (isPending) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRejectClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("Tolak", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onApproveClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("Setujui", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingBottomSheetContent(
    item: ApprovalItem,
    onClose: () -> Unit
) {
    val steps = item.workflow?.steps ?: emptyList()
    val logs = item.logs ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lacak Persetujuan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Modul: ${item.moduleLabel} · Pemohon: ${item.requester?.name ?: "-"}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (steps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada alur workflow yang ditemukan.", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(steps.size) { index ->
                    val step = steps[index]
                    val isFirst = index == 0
                    val isLast = index == steps.size - 1
                    val log = logs.findLast { 
                        (it.stepId == step.id || (step.order != null && it.stepId == step.order.toLong())) &&
                        it.action?.lowercase() !in listOf("escalate", "submit")
                    }

                    TrackingStepTimelineItem(
                        step = step,
                        log = log,
                        isFirst = isFirst,
                        isLast = isLast
                    )
                }
            }
        }
    }
}

@Composable
fun TrackingStepTimelineItem(
    step: ApprovalStep,
    log: ApprovalLog?,
    isFirst: Boolean,
    isLast: Boolean
) {
    val expectedName = step.approverName ?: step.expectedApproverName ?: step.approverType ?: "Approver"
    val statusStr = log?.action ?: "PENDING"

    val statusColor = when (statusStr.lowercase()) {
        "approve", "approved" -> Color(0xFF16A34A)
        "reject", "rejected" -> Color(0xFFDC2626)
        "cancel", "cancelled" -> Color(0xFF64748B)
        else -> Color(0xFFF59E0B)
    }

    val actionByText = if (!log?.approverName.isNullOrEmpty() && log?.approverName?.lowercase() != "system") {
        "Aktor: ${log.approverName}"
    } else {
        "Menunggu: $expectedName"
    }

    val dateFormatted = formatDisplayDate(log?.createdAt)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Vertical Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Top connecting line
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .background(Color(0xFFE2E8F0))
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            // Bottom connecting line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(Color(0xFFE2E8F0))
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Step Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tahap ${step.order ?: 1}: $expectedName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusStr.uppercase(),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = actionByText,
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                if (log?.createdAt != null) {
                    Text(
                        text = "Tanggal: $dateFormatted",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (!log?.note.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Catatan: ${log.note}",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatDisplayDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "-"
    return try {
        if (dateStr.contains("T")) {
            val parts = dateStr.split("T")
            val datePart = parts[0]
            val timePart = parts.getOrNull(1)?.take(5) ?: ""
            "$datePart $timePart"
        } else {
            dateStr.take(16)
        }
    } catch (e: Exception) {
        dateStr
    }
}
