package com.jangeby.system.ui.overtime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApprovalItem
import com.jangeby.system.data.remote.OvertimeRequestItem
import com.jangeby.system.ui.component.DatePickerField
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.component.TimePickerField
import com.jangeby.system.ui.theme.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeScreen(
    onNavigateBack: () -> Unit,
    viewModel: OvertimeViewModel = koinViewModel()
) {
    val overtimes by viewModel.overtimes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val trackingDetail by viewModel.trackingDetail.collectAsState()
    val isTrackingLoading by viewModel.isTrackingLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    // Dialog & sheet states
    var showFormSheet by remember { mutableStateOf(false) }
    var itemToCancel by remember { mutableStateOf<OvertimeRequestItem?>(null) }
    var selectedItemForTracking by remember { mutableStateOf<OvertimeRequestItem?>(null) }
    var selectedFilter by remember { mutableStateOf("Semua") }

    val filterOptions = listOf("Semua", "Menunggu", "Disetujui", "Ditolak", "Dibatalkan")

    // Filter list
    val filteredList = remember(overtimes, selectedFilter) {
        when (selectedFilter) {
            "Menunggu" -> overtimes.filter { it.status.equals("pending", ignoreCase = true) }
            "Disetujui" -> overtimes.filter { it.status.equals("approved", ignoreCase = true) }
            "Ditolak" -> overtimes.filter { it.status.equals("rejected", ignoreCase = true) }
            "Dibatalkan" -> overtimes.filter { it.status.equals("cancelled", ignoreCase = true) }
            else -> overtimes
        }
    }

    val countApproved = remember(overtimes) {
        overtimes.count { it.status.equals("approved", ignoreCase = true) }
    }
    val countPending = remember(overtimes) {
        overtimes.count { it.status.equals("pending", ignoreCase = true) }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            showFormSheet = false
            viewModel.loadOvertimes()
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Pengajuan Lembur",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadOvertimes() }) {
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
                    contentDescription = "Ajukan Lembur",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FA))
                .padding(padding)
        ) {
            if (isLoading && overtimes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // Summary Banner Card
                    item {
                        OvertimeSummaryBanner(
                            totalCount = overtimes.size,
                            countApproved = countApproved,
                            countPending = countPending
                        )
                    }

                    // Filter Chips Row
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                        selectedContainerColor = Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White,
                                        labelColor = TextPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) Primary else Color(0xFFE2E8F0),
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }

                    // Content List or Empty View
                    if (filteredList.isEmpty()) {
                        item {
                            EmptyOvertimeView(filter = selectedFilter)
                        }
                    } else {
                        items(filteredList, key = { it.id }) { item ->
                            OvertimeItemCard(
                                item = item,
                                formattedDate = viewModel.formatDateDMY(item.date),
                                onTrackingClick = {
                                    selectedItemForTracking = item
                                    item.approval?.id?.let { approvalId ->
                                        viewModel.loadTracking(approvalId)
                                    }
                                },
                                onCancelClick = {
                                    itemToCancel = item
                                },
                                onAttachmentClick = { attachmentPath ->
                                    val baseUrl = ApiClient.BASE_URL.replace("api/v1/", "storage/")
                                    val fullUrl = if (attachmentPath.startsWith("http")) {
                                        attachmentPath
                                    } else {
                                        "$baseUrl$attachmentPath"
                                    }
                                    uriHandler.openUri(fullUrl)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Form Pengajuan Lembur
    if (showFormSheet) {
        FormOvertimeBottomSheet(
            isSubmitting = isSubmitting,
            apiErrorMessage = errorMessage,
            onClearError = { viewModel.clearMessages() },
            onDismiss = { showFormSheet = false },
            onSubmit = { date, startTime, endTime, reason, photo ->
                viewModel.submitOvertime(date, startTime, endTime, reason, photo)
            }
        )
    }

    // Dialog Konfirmasi Batalkan Lembur
    itemToCancel?.let { item ->
        var cancelReason by remember { mutableStateOf("Dibatalkan oleh karyawan") }
        AlertDialog(
            onDismissRequest = { itemToCancel = null },
            title = {
                Text(
                    text = "Batalkan Pengajuan Lembur",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda yakin ingin membatalkan pengajuan lembur tanggal ${item.date}?",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Alasan Pembatalan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOvertime(item.id, cancelReason)
                        itemToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Ya, Batalkan", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToCancel = null }) {
                    Text("Kembali")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // BottomSheet Tracking Approval Lembur
    selectedItemForTracking?.let { item ->
        OvertimeTrackingBottomSheet(
            item = item,
            formattedDate = viewModel.formatDateDMY(item.date),
            trackingDetail = trackingDetail,
            isLoading = isTrackingLoading,
            onDismiss = {
                selectedItemForTracking = null
                viewModel.clearTracking()
            }
        )
    }
}

/**
 * Summary Banner di bagian atas list
 */
@Composable
private fun OvertimeSummaryBanner(
    totalCount: Int,
    countApproved: Int,
    countPending: Int
) {
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
                .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0D9488))))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Pengajuan Lembur",
                        color = Color.White.copy(0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalCount Pengajuan",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "$countApproved Disetujui",
                            color = Color(0xFFA7F3D0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(0.5f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$countPending Menunggu",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/**
 * Card Item Riwayat Lembur
 */
@Composable
private fun OvertimeItemCard(
    item: OvertimeRequestItem,
    formattedDate: String,
    onTrackingClick: () -> Unit,
    onCancelClick: () -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    val statusLower = item.status.lowercase()

    val (badgeBg, badgeTextColor, statusLabel) = when (statusLower) {
        "approved" -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Disetujui"
        )
        "rejected" -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            "Ditolak"
        )
        "cancelled" -> Triple(
            Color(0xFFECEFF1),
            Color(0xFF546E7A),
            "Dibatalkan"
        )
        else -> Triple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            "Menunggu"
        )
    }

    val startTimeFormatted = item.requestedStartTime?.take(5) ?: "-"
    val endTimeFormatted = item.requestedEndTime?.take(5) ?: "-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Tanggal & Badge Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFCCFBF1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = formattedDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Jam: $startTimeFormatted – $endTimeFormatted",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = statusLabel,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Jam Pelaksanaan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Waktu Lembur",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "$startTimeFormatted s/d $endTimeFormatted",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
            }

            // Alasan Lembur
            if (!item.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "Alasan",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = item.reason,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Lampiran Dokumen Bukti
            if (!item.attachment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onAttachmentClick(item.attachment) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Lampiran",
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lihat Dokumen / Bukti Lembur",
                        color = Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action Buttons: Lihat Tracking & Batalkan
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.approval != null) {
                    TextButton(onClick = onTrackingClick) {
                        Text(
                            text = "Lihat Tracking",
                            fontSize = 12.sp,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (statusLower == "pending") {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                        border = BorderStroke(1.dp, Danger),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Batalkan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * BottomSheet Form Pengajuan Lembur sesuai legacy FormOvertimeActivity.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormOvertimeBottomSheet(
    isSubmitting: Boolean,
    apiErrorMessage: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (date: String, startTime: String, endTime: String, reason: String, photo: PlatformFile?) -> Unit
) {
    var selectedDate by remember { mutableStateOf("") }
    val startTime = "20:30" // Fixed start time
    var endTime by remember { mutableStateOf("22:00") }
    var reason by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<PlatformFile?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Pilih Surat Tugas / Lampiran Lembur"
    ) { file ->
        file?.let { selectedPhoto = it }
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
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Ajukan Lembur",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pengajuan lembur akan dikirimkan ke atasan untuk proses approval",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tanggal Lembur
            Text(
                text = "Tanggal Lembur",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            DatePickerField(
                value = selectedDate,
                onValueChange = { selectedDate = it },
                label = "Pilih Tanggal Lembur",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Waktu Mulai & Selesai
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jam Mulai",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color(0xFFCBD5E1),
                            disabledContainerColor = Color(0xFFF1F5F9)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jam Selesai",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TimePickerField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = "Jam Selesai",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alasan Lembur
            Text(
                text = "Alasan / Rincian Pekerjaan Lembur",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Jelaskan tugas lembur yang dikerjakan...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Lampiran Surat Tugas / Foto (Opsional)
            Text(
                text = "Dokumen / Surat Tugas (Opsional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { filePicker.launch() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (selectedPhoto != null) Primary else Color(0xFFCBD5E1))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (selectedPhoto != null) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = if (selectedPhoto != null) Primary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = selectedPhoto?.name ?: "Pilih Lampiran / Bukti",
                        fontSize = 13.sp,
                        color = if (selectedPhoto != null) Primary else Color.Gray
                    )
                }
            }

            if (validationError != null || apiErrorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = validationError ?: apiErrorMessage ?: "",
                            color = Danger,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (apiErrorMessage != null) {
                            IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Danger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (selectedDate.isBlank()) {
                        validationError = "Silakan pilih tanggal lembur"
                        return@Button
                    }
                    if (reason.isBlank()) {
                        validationError = "Silakan isi alasan pengajuan lembur"
                        return@Button
                    }
                    validationError = null
                    onSubmit(selectedDate, startTime, endTime, reason, selectedPhoto)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "KIRIM PENGAJUAN LEMBUR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * BottomSheet Tracking Approval Lembur
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OvertimeTrackingBottomSheet(
    item: OvertimeRequestItem,
    formattedDate: String,
    trackingDetail: ApprovalItem?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Tracking Persetujuan Lembur",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tanggal $formattedDate • ${item.requestedStartTime?.take(5) ?: "-"} s/d ${item.requestedEndTime?.take(5) ?: "-"}",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (trackingDetail != null) {
                val steps = trackingDetail.workflow?.steps ?: emptyList()
                val logs = trackingDetail.logs ?: emptyList()

                if (steps.isEmpty()) {
                    Text(
                        text = "Belum ada alur approval yang tercatat.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    steps.forEachIndexed { index, step ->
                        val isLast = index == steps.size - 1
                        val log = logs.findLast { 
                            (it.stepId == step.id || (step.order != null && it.stepId == step.order.toLong())) &&
                            it.action?.lowercase() !in listOf("escalate", "submit")
                        }

                        val action = log?.action?.lowercase()
                        val (stepColor, statusText) = when (action) {
                            "approved", "approve" -> Color(0xFF4CAF50) to "Disetujui"
                            "rejected", "reject" -> Danger to "Ditolak"
                            else -> Color(0xFFFFA000) to "Menunggu"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(28.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(stepColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (action) {
                                            "approved", "approve" -> Icons.Default.Check
                                            "rejected", "reject" -> Icons.Default.Close
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = stepColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                if (!isLast) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(48.dp)
                                            .background(Color(0xFFE2E8F0))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.expectedApproverName ?: step.approverName ?: step.approverType ?: "Tahap ${step.order ?: (index + 1)}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Status: $statusText",
                                    fontSize = 12.sp,
                                    color = stepColor,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!log?.note.isNullOrBlank()) {
                                    Text(
                                        text = "Catatan: ${log?.note}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                if (!log?.createdAt.isNullOrBlank()) {
                                    Text(
                                        text = log.createdAt,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Informasi tracking approval tidak ditemukan.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

/**
 * Tampilan saat list pengajuan kosong
 */
@Composable
private fun EmptyOvertimeView(filter: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCCFBF1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF0F766E),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (filter == "Semua") "Belum ada riwayat pengajuan lembur" else "Tidak ada data lembur dengan status '$filter'",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Klik tombol '+' di bawah untuk mengajukan lembur baru",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
