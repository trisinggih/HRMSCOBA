package com.jangeby.system.ui.permit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.data.remote.ApprovalItem
import com.jangeby.system.data.remote.PermitRequestItem
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermitScreen(
    onNavigateBack: () -> Unit,
    viewModel: PermitViewModel = koinInject()
) {
    val uriHandler = LocalUriHandler.current

    val permits by viewModel.permits.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val trackingDetail by viewModel.trackingDetail.collectAsState()
    val isTrackingLoading by viewModel.isTrackingLoading.collectAsState()

    var showFormSheet by remember { mutableStateOf(false) }
    var selectedItemForTracking by remember { mutableStateOf<PermitRequestItem?>(null) }
    var itemToCancel by remember { mutableStateOf<PermitRequestItem?>(null) }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Permit User",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadPermits() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = Color.White
                        )
                    }
                }
            )
        },
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
                    contentDescription = "Ajukan Izin",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
        ) {
            if (isLoading && permits.isEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary
                )
            }

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Danger,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = successMessage!!,
                            color = Color(0xFF16A34A),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Pengajuan Izin (${permits.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                }

                items(permits) { item ->
                    PermitCard(
                        item = item,
                        viewModel = viewModel,
                        onTrackingClick = {
                            selectedItemForTracking = item
                            item.approval?.id?.let { approvalId ->
                                viewModel.loadTracking(approvalId)
                            }
                        },
                        onCancelClick = {
                            itemToCancel = item
                        },
                        onAttachmentClick = { url ->
                            uriHandler.openUri(url)
                        }
                    )
                }

                if (!isLoading && permits.isEmpty() && errorMessage == null) {
                    item {
                        EmptyState("Belum ada riwayat pengajuan izin")
                    }
                }
            }
        }
    }

    // BottomSheet Form Pengajuan
    if (showFormSheet) {
        FormPermitBottomSheet(
            isSubmitting = isSubmitting,
            onDismiss = { showFormSheet = false },
            onSubmit = { type, date, startTime, endTime, reason, attachment ->
                viewModel.submitPermit(
                    type = type,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    reason = reason,
                    attachment = attachment,
                    onSuccess = { showFormSheet = false }
                )
            }
        )
    }

    // BottomSheet Tracking Approval
    if (selectedItemForTracking != null) {
        TrackingPermitBottomSheet(
            item = selectedItemForTracking!!,
            trackingDetail = trackingDetail,
            isTrackingLoading = isTrackingLoading,
            viewModel = viewModel,
            onDismiss = {
                selectedItemForTracking = null
                viewModel.clearTracking()
            }
        )
    }

    // Dialog Konfirmasi Batalkan
    if (itemToCancel != null) {
        AlertDialog(
            onDismissRequest = { itemToCancel = null },
            title = { Text("Batalkan Permit", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin membatalkan pengajuan izin ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        val approvalId = itemToCancel?.approval?.id
                        itemToCancel = null
                        if (approvalId != null) {
                            viewModel.cancelPermit(approvalId) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Ya, Batalkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToCancel = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun PermitCard(
    item: PermitRequestItem,
    viewModel: PermitViewModel,
    onTrackingClick: () -> Unit,
    onCancelClick: () -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    val typeTitle = when (item.type?.lowercase()) {
        "personal" -> "Keperluan Pribadi"
        "business" -> "Dinas Kantor"
        else -> item.type?.replaceFirstChar { it.uppercase() } ?: "Izin"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = typeTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "${viewModel.formatDateDMY(item.date)} · ${item.startTime?.take(5) ?: "-"} - ${item.endTime?.take(5) ?: "-"}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                StatusChip(item.status ?: "pending")
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Alasan: ${item.reason ?: "-"}",
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            if (!item.attachment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEFF6FF))
                        .clickable { onAttachmentClick(item.attachment) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Lampiran",
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lihat Lampiran",
                        fontSize = 11.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.status?.lowercase() == "pending" && item.approval != null) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                        border = BorderStroke(1.dp, Danger),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Batalkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = onTrackingClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tracking", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPermitBottomSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (type: String, date: String, startTime: String, endTime: String, reason: String, attachment: PlatformFile?) -> Unit
) {
    var selectedTypeIndex by remember { mutableStateOf(0) }
    val typeOptions = listOf("Keperluan Pribadi" to "personal", "Dinas Kantor" to "business")

    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<PlatformFile?>(null) }

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single
    ) { file ->
        selectedFile = file
    }

    val isPersonal = selectedTypeIndex == 0
    val startLabel = if (isPersonal) "Jam Keluar (HH:mm) *" else "Jam Mulai (HH:mm) *"
    val endLabel = if (isPersonal) "Jam Kembali (HH:mm) *" else "Jam Selesai (HH:mm) *"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ajukan Permit / Izin",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text("Jenis Izin:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                typeOptions.forEachIndexed { index, (label, _) ->
                    FilterChip(
                        selected = selectedTypeIndex == index,
                        onClick = { selectedTypeIndex = index },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Tanggal (YYYY-MM-DD) *") },
                placeholder = { Text("Contoh: 2026-10-08") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.DateRange, null, tint = Primary) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text(startLabel) },
                    placeholder = { Text("09:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text(endLabel) },
                    placeholder = { Text("12:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Alasan / Keperluan *") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
            )

            // Attachment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { filePicker.launch() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (selectedFile != null) "Ganti Foto" else "Lampiran Bukti (Opsional)", fontSize = 12.sp)
                }

                if (selectedFile != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedFile!!.name, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        IconButton(onClick = { selectedFile = null }) {
                            Icon(Icons.Default.Close, null, tint = Danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val typeCode = typeOptions[selectedTypeIndex].second
                    onSubmit(typeCode, date.trim(), startTime.trim(), endTime.trim(), reason.trim(), selectedFile)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isSubmitting && date.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank() && reason.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("KIRIM PENGAJUAN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingPermitBottomSheet(
    item: PermitRequestItem,
    trackingDetail: ApprovalItem?,
    isTrackingLoading: Boolean,
    viewModel: PermitViewModel,
    onDismiss: () -> Unit
) {
    val typeTitle = when (item.type?.lowercase()) {
        "personal" -> "Keperluan Pribadi"
        "business" -> "Dinas Kantor"
        else -> item.type?.replaceFirstChar { it.uppercase() } ?: "Izin"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Tracking Permit / Izin",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Jenis: $typeTitle", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            Text("Tanggal: ${viewModel.formatDateDMY(item.date)}", fontSize = 13.sp, color = Color(0xFF757575))
            Text("Waktu: ${item.startTime?.take(5) ?: "-"} s/d ${item.endTime?.take(5) ?: "-"}", fontSize = 13.sp, color = Color(0xFF757575))
            Text("Alasan: ${item.reason ?: "-"}", fontSize = 13.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(16.dp))

            if (isTrackingLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (trackingDetail != null) {
                val steps = trackingDetail.workflow?.steps ?: emptyList()
                val logs = trackingDetail.logs ?: emptyList()

                Text("Alur Persetujuan:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                if (steps.isEmpty()) {
                    Text("Tidak ada alur langkah approval.", fontSize = 13.sp, color = Color(0xFF757575))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.forEachIndexed { idx, step ->
                            val approverName = step.expectedApproverName ?: step.approverName ?: step.approverType ?: "Tahap ${step.order ?: (idx + 1)}"
                            val isDone = (trackingDetail.currentStep ?: 1) > (step.order ?: 0) || trackingDetail.status == "approved"
                            val isCurrent = (trackingDetail.currentStep ?: 1) == (step.order ?: 0) && 
                                (trackingDetail.status == "pending" || trackingDetail.status == "in_review")

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isDone -> Color(0xFF4CAF50)
                                                isCurrent -> Color(0xFFFF9800)
                                                else -> Color(0xFFBDBDBD)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text(
                                            text = "${step.order ?: (idx + 1)}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = approverName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) Color.Black else Color(0xFF424242)
                                    )
                                    val statusText = when {
                                        isDone -> "Disetujui"
                                        isCurrent -> "Menunggu Persetujuan"
                                        else -> "Menunggu giliran"
                                    }
                                    Text(text = statusText, fontSize = 11.sp, color = Color(0xFF757575))
                                }
                            }
                        }
                    }
                }

                if (logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Catatan / Log:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        logs.forEach { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    val actor = log.approverName ?: "Sistem"
                                    Text(
                                        text = "$actor · ${log.action?.uppercase() ?: ""}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                    if (!log.note.isNullOrBlank()) {
                                        Text(text = log.note, fontSize = 11.sp, color = Color(0xFF616161))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Data tracking tidak tersedia untuk permohonan ini.",
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )
            }
        }
    }
}
