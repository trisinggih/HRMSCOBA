package com.jangeby.system.ui.approval

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.jangeby.system.utils.ApprovalUtils
import com.jangeby.system.utils.StepStatusInfo
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ApprovalViewModel = koinViewModel()
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
    var approvedAssetItemIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Dialog state for Reject
    var itemToReject by remember { mutableStateOf<ApprovalItem?>(null) }
    var rejectNote by remember { mutableStateOf("") }

    // BottomSheet state for Asset Detail
    var itemForAssetDetail by remember { mutableStateOf<ApprovalItem?>(null) }

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
                                approvedAssetItemIds = item.assetItems.mapNotNull { it.id }.toSet()
                            },
                            onRejectClick = {
                                itemToReject = item
                                rejectNote = ""
                            },
                            onTrackingClick = {
                                viewModel.loadTracking(item.id)
                            },
                            onAssetDetailClick = {
                                itemForAssetDetail = item
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

    // Modal BottomSheet for Asset Detail
    if (itemForAssetDetail != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { itemForAssetDetail = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AssetDetailBottomSheetContent(
                item = itemForAssetDetail!!,
                onClose = { itemForAssetDetail = null }
            )
        }
    }

    // Dialog / BottomSheet for Approve
    if (itemToApprove != null) {
        val currentApproveItem = itemToApprove!!
        if (currentApproveItem.isAssetRequest && currentApproveItem.assetItems.isNotEmpty()) {
            AssetApprovalBottomSheet(
                item = currentApproveItem,
                isActionLoading = isActionLoading,
                selectedIds = approvedAssetItemIds,
                onToggleItem = { id ->
                    approvedAssetItemIds = if (approvedAssetItemIds.contains(id)) {
                        approvedAssetItemIds - id
                    } else {
                        approvedAssetItemIds + id
                    }
                },
                onSelectAll = {
                    approvedAssetItemIds = currentApproveItem.assetItems.mapNotNull { it.id }.toSet()
                },
                onUnselectAll = {
                    approvedAssetItemIds = emptySet()
                },
                note = approveNote,
                onNoteChange = { approveNote = it },
                onDismiss = {
                    if (!isActionLoading) itemToApprove = null
                },
                onConfirm = {
                    viewModel.approveRequest(
                        id = currentApproveItem.id,
                        note = approveNote,
                        tenor = null,
                        approvedItemIds = approvedAssetItemIds.toList()
                    ) { success, msg ->
                        itemToApprove = null
                        scope.launch {
                            snackbarHostState.showSnackbar(msg ?: if (success) "Pengajuan disetujui" else "Gagal")
                        }
                    }
                }
            )
        } else {
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
    onAssetDetailClick: () -> Unit = {},
    onOpenAttachment: (String) -> Unit
) {
    val photoUrl = item.requester?.photo?.let {
        if (it.startsWith("http")) it else "${com.jangeby.system.data.remote.ApiClient.BASE_URL.replace("/api/v1/", "/storage/")}${it.removePrefix("/")}"
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
                    text = item.moduleLabel,
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

            // Card visual khusus Surat Mutasi / Promosi
            if (item.isMutation) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isPromosi = item.mutationType?.equals("promosi", ignoreCase = true) == true
                            val isDemosi = item.mutationType?.equals("demosi", ignoreCase = true) == true
                            val badgeBg = when {
                                isPromosi -> Color(0xFFDCFCE7)
                                isDemosi -> Color(0xFFFFEDD5)
                                else -> Color(0xFFCCFBF1)
                            }
                            val badgeTextColor = when {
                                isPromosi -> Color(0xFF15803D)
                                isDemosi -> Color(0xFFC2410C)
                                else -> Color(0xFF0F766E)
                            }
                            val badgeLabel = when {
                                isPromosi -> "PROMOSI JABATAN"
                                isDemosi -> "DEMOSI LEVEL"
                                else -> "MUTASI PENEMPATAN"
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (!item.mutationMemoNumber.isNullOrBlank()) {
                                Text(
                                    text = item.mutationMemoNumber!!,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Asal
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "POSISI ASAL",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.mutationOldPosition ?: "-",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                if (!item.mutationOldBranch.isNullOrBlank()) {
                                    Text(
                                        text = item.mutationOldBranch!!,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                            )
                            
                            // Tujuan
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TARGET BARU",
                                    fontSize = 10.sp,
                                    color = Color(0xFF0D9488),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.mutationNewPosition ?: "-",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E)
                                )
                                if (!item.mutationNewBranch.isNullOrBlank()) {
                                    Text(
                                        text = item.mutationNewBranch!!,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        if (!item.mutationEffectiveDate.isNullOrBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color(0xFFE2E8F0)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Efektif: ${com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(item.mutationEffectiveDate!!)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Button to view Asset Request Details if module is asset_request
            if (item.isAssetRequest) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAssetDetailClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val count = item.assetItems.size
                            Text(
                                text = if (count > 1) "Lihat Detail Pengajuan ($count Barang)" else "Lihat Detail Pengajuan Aset",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "Ketuk untuk melihat rincian barang & estimasi",
                                fontSize = 11.sp,
                                color = Color(0xFF16A34A)
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
            .navigationBarsPadding()
            .padding(bottom = 36.dp)
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
                    text = "${item.moduleLabel} · Pemohon: ${item.requester?.name ?: "-"}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (steps.isEmpty() && logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada alur workflow yang ditemukan.", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else if (steps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(steps.size) { index ->
                    val step = steps[index]
                    val isFirst = index == 0
                    val isLast = index == steps.size - 1
                    val statusInfo = ApprovalUtils.resolveStepStatus(item, step, index)

                    TrackingStepTimelineItem(
                        step = step,
                        statusInfo = statusInfo,
                        isFirst = isFirst,
                        isLast = isLast
                    )
                }
            }
        } else {
            // Steps kosong namun logs approval tersedia
            val displayLogs = logs.filter { it.action?.lowercase() !in listOf("escalate") }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(displayLogs.size) { index ->
                    val l = displayLogs[index]
                    val isFirst = index == 0
                    val isLast = index == displayLogs.size - 1
                    val logAction = l.action?.lowercase()
                    val (logColor, statusLabel) = when (logAction) {
                        "approved", "approve" -> Color(0xFF16A34A) to "DISETUJUI"
                        "rejected", "reject" -> Color(0xFFDC2626) to "DITOLAK"
                        "cancelled", "cancel" -> Color(0xFF64748B) to "DIBATALKAN"
                        "submit" -> Color(0xFF0284C7) to "DIAJUKAN"
                        else -> Color(0xFFF59E0B) to (l.action?.uppercase() ?: "DIPROSES")
                    }
                    val actorName = l.approverName ?: "Sistem / Admin"
                    val actorLine = when (logAction) {
                        "approved", "approve" -> "Disetujui oleh: $actorName"
                        "rejected", "reject" -> "Ditolak oleh: $actorName"
                        "cancelled", "cancel" -> "Dibatalkan oleh: $actorName"
                        "submit" -> "Diajukan oleh: $actorName"
                        else -> "Oleh: $actorName"
                    }
                    val dummyStep = ApprovalStep(
                        id = l.stepId ?: (index + 1).toLong(),
                        order = index + 1,
                        approverName = actorName
                    )
                    val dummyInfo = StepStatusInfo(
                        statusText = statusLabel,
                        statusColor = logColor,
                        actorLine = actorLine,
                        note = l.note,
                        dateText = l.createdAt
                    )
                    TrackingStepTimelineItem(
                        step = dummyStep,
                        statusInfo = dummyInfo,
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
    statusInfo: StepStatusInfo,
    isFirst: Boolean,
    isLast: Boolean
) {
    val expectedName = step.approverName ?: step.expectedApproverName ?: step.approverType ?: "Approver"
    val dateFormatted = formatDisplayDate(statusInfo.dateText)

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
                    .background(statusInfo.statusColor)
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
                            .background(statusInfo.statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusInfo.statusText,
                            color = statusInfo.statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusInfo.actorLine,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )

                if (!statusInfo.dateText.isNullOrBlank()) {
                    Text(
                        text = "Tanggal: $dateFormatted",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (!statusInfo.note.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Catatan: ${statusInfo.note}",
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

@Composable
fun AssetDetailBottomSheetContent(
    item: ApprovalItem,
    onClose: () -> Unit
) {
    val items = item.assetItems
    val totalEst = item.assetTotalEstimatedPrice
    val totalEstStr = if (totalEst != null && totalEst > 0) {
        val rounded = totalEst.toLong()
        "Rp " + rounded.toString().reversed().chunked(3).joinToString(".").reversed()
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .padding(bottom = 36.dp)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Detail Pengajuan Aset",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Pemohon: ${item.requester?.name ?: "-"}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val reqType = item.assetRequestType ?: "kebutuhan_rutin"
                            val typeLabel = when (reqType) {
                                "pengadaan_baru" -> "Pengadaan Baru"
                                "pergantian_alat" -> "Pergantian Alat"
                                "kebutuhan_karyawan_baru" -> "Karyawan Baru"
                                else -> "Kebutuhan Rutin"
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (totalEstStr != null) {
                                Text(
                                    text = totalEstStr,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))

                        if (!item.assetPurpose.isNullOrBlank()) {
                            Text(
                                text = "Tujuan / Alasan Kebutuhan:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = item.assetPurpose ?: "-",
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!item.assetForWho.isNullOrBlank()) {
                            Text(
                                text = "Untuk Siapa / Penempatan:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            val placement = item.assetPlacement?.takeIf { it != item.assetForWho }
                            val placementStr = if (placement != null) "${item.assetForWho} ($placement)" else item.assetForWho ?: "-"
                            Text(
                                text = placementStr,
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!item.assetNotes.isNullOrBlank()) {
                            Text(
                                text = "Keterangan / Catatan:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = item.assetNotes ?: "-",
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            // Section Header: Daftar Barang
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rincian Barang (${items.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada rincian barang",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                items(items.size) { idx ->
                    val it = items[idx]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. ${it.itemName ?: "Barang"}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (!it.itemSize.isNullOrBlank()) {
                                        Text(
                                            text = "Ukuran / Spesifikasi: ${it.itemSize}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "${it.quantity ?: 1} unit",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            val est = it.estimatedPrice
                            val unitP = it.unitPrice
                            if ((est != null && est > 0) || (unitP != null && unitP > 0)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (unitP != null && unitP > 0) {
                                        val uStr = "Rp " + unitP.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
                                        Text(
                                            text = "@ $uStr",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }
                                    if (est != null && est > 0) {
                                        val eStr = "Rp " + est.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
                                        Text(
                                            text = "Total: $eStr",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }
                            }

                            if (!it.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Catatan: ${it.notes}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDisplayDate(dateStr: String?): String {
    return com.jangeby.system.utils.DateTimeUtils.formatDisplayDate(dateStr)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetApprovalBottomSheet(
    item: ApprovalItem,
    isActionLoading: Boolean,
    selectedIds: Set<Long>,
    onToggleItem: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val items = item.assetItems
    val allSelected = items.isNotEmpty() && items.all { it.id != null && selectedIds.contains(it.id) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (!isActionLoading) onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Persetujuan Pengajuan Aset",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Pemohon: ${item.requester?.name ?: "-"}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isActionLoading) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brief summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!item.assetPurpose.isNullOrBlank()) {
                        Text(
                            text = "Tujuan / Keperluan: ${item.assetPurpose}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    if (!item.assetForWho.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Untuk: ${item.assetForWho}" + (item.assetPlacement?.let { " ($it)" } ?: ""),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checklist Header & Select All Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pilih Barang yang Disetujui *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${selectedIds.size} dari ${items.size} barang dipilih",
                        fontSize = 12.sp,
                        color = if (selectedIds.isEmpty()) Danger else Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = {
                        if (allSelected) onUnselectAll() else onSelectAll()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (allSelected) "Batal Semua" else "Pilih Semua",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items Checklist
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEachIndexed { idx, itm ->
                    val isChecked = itm.id != null && selectedIds.contains(itm.id)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) Color(0xFFF0FDF4) else Color.White,
                        border = BorderStroke(
                            width = if (isChecked) 1.5.dp else 1.dp,
                            color = if (isChecked) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                itm.id?.let { onToggleItem(it) }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    itm.id?.let { id -> onToggleItem(id) }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF16A34A),
                                    checkmarkColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${idx + 1}. ${itm.itemName ?: "Barang"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                                if (!itm.itemSize.isNullOrBlank()) {
                                    Text(
                                        text = "Ukuran: ${itm.itemSize}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (!itm.notes.isNullOrBlank()) {
                                    Text(
                                        text = "Catatan: ${itm.notes}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isChecked) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "${itm.quantity ?: 1} unit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChecked) Color(0xFF15803D) else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                val est = itm.estimatedPrice
                                if (est != null && est > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val estStr = "Rp " + est.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
                                    Text(
                                        text = estStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedIds.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Minimal pilih 1 barang untuk disetujui. Jika Anda ingin menolak seluruh pengajuan, silakan gunakan tombol 'Tolak'.",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Catatan Approver
            Text(
                text = "Catatan Persetujuan (Opsional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text("Tambahkan catatan bila perlu...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                shape = RoundedCornerShape(8.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    enabled = !isActionLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1.5f).height(46.dp),
                    enabled = !isActionLoading && selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        val countText = if (selectedIds.isNotEmpty()) " (${selectedIds.size})" else ""
                        Text("Setujui$countText", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
