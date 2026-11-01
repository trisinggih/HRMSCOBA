package com.jangeby.system.ui.asset

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.data.remote.AssetItem
import com.jangeby.system.data.remote.AssetRequestItem
import com.jangeby.system.data.remote.AssetRequestApprovalItem
import com.jangeby.system.data.remote.AssetRequestDetailItem
import com.jangeby.system.data.remote.AssetRequestItemCreate
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.theme.*
import com.jangeby.system.utils.DateTimeUtils
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetScreen(
    onNavigateBack: () -> Unit,
    viewModel: AssetViewModel = koinViewModel()
) {
    val assets by viewModel.assets.collectAsState()
    val assetRequests by viewModel.assetRequests.collectAsState()
    val hasSubordinates by viewModel.hasSubordinates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showRequestSheet by remember { mutableStateOf(false) }
    var itemForDetail by remember { mutableStateOf<AssetRequestItem?>(null) }

    LaunchedEffect(hasSubordinates) {
        if (!hasSubordinates) {
            selectedTab = 0
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

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
        }
    }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Manajemen Aset",
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = {
                            if (selectedTab == 0 || !hasSubordinates) viewModel.loadAssets() else viewModel.loadAssetRequests()
                        }
                    ) {
                        Text("Segarkan", color = Color.White, fontSize = 13.sp)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FA))
                .padding(padding)
        ) {
            // Tab Header (Aset Saya & Permintaan Aset) - Hanya ditampilkan bila karyawan memiliki bawahan
            if (hasSubordinates) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Aset Saya",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            viewModel.loadAssetRequests()
                        },
                        text = {
                            Text(
                                text = "Permintaan Aset",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0 || !hasSubordinates) {
                    // TAB 1: ASET SAYA
                    if (isLoading && assets.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (assets.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada aset yang sedang Anda pegang.",
                                color = Color(0xFF757575),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(assets, key = { it.id }) { item ->
                                AssetItemCard(item = item)
                            }
                        }
                    }
                } else {
                    // TAB 2: PERMINTAAN ASET (REQUEST PEMBELIAN)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Action Bar: Tombol Ajukan Pembelian Aset
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pengajuan Pembelian",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Ajukan kebutuhan aset kerja baru",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Button(
                                    onClick = { showRequestSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("Ajukan Aset", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (isLoading && assetRequests.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Primary)
                            }
                        } else if (assetRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Belum ada permintaan pembelian aset.",
                                        color = Color(0xFF757575),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = { showRequestSheet = true },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Buat Pengajuan Pertama", color = Primary)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(assetRequests, key = { it.id }) { req ->
                                    AssetRequestCard(
                                        item = req,
                                        onDetailClick = { itemForDetail = req }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet untuk Form Pengajuan Pembelian Aset
    if (showRequestSheet) {
        AssetRequestBottomSheet(
            isSubmitting = isSubmitting,
            onDismiss = { showRequestSheet = false },
            onSubmit = { type, items, forWho, purpose ->
                viewModel.submitAssetRequest(
                    requestType = type,
                    forWho = forWho,
                    purpose = purpose,
                    items = items,
                    onSuccess = {
                        showRequestSheet = false
                    }
                )
            }
        )
    }

    // Modal Bottom Sheet untuk Detail Permintaan Aset & Status Approval
    if (itemForDetail != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { itemForDetail = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AssetRequestDetailBottomSheetContent(
                item = itemForDetail!!,
                onClose = { itemForDetail = null }
            )
        }
    }
}

/**
 * Kartu Riwayat Pengajuan Pembelian Aset
 */
@Composable
private fun AssetRequestCard(
    item: AssetRequestItem,
    onDetailClick: () -> Unit = {}
) {
    val status = item.status
    val (statusLabel, statusBg, statusTextColor) = when (status) {
        "pending" -> Triple("Menunggu", Color(0xFFFEF3C7), Color(0xFFD97706))
        "approved" -> Triple("Disetujui", Color(0xFFE0F2FE), Color(0xFF0284C7))
        "purchased" -> Triple("Sudah Dibeli", Color(0xFFDCFCE7), Color(0xFF16A34A))
        "rejected" -> Triple("Ditolak", Color(0xFFFEE2E2), Color(0xFFDC2626))
        else -> Triple(status, Color(0xFFF3F4F6), Color(0xFF6B7280))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Nama Barang & Status Badge
            val typeLabel = when (item.requestType) {
                "kebutuhan_karyawan_baru" -> "Aset Karyawan Baru"
                "kebutuhan_perbaikan" -> "Perbaikan Lampu & Kabel"
                else -> "Rutin ATK & Kebersihan"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = typeLabel,
                        fontSize = 11.sp,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusLabel,
                        color = statusTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!item.itemSize.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFFEF9C3),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ukuran: ${item.itemSize}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF854D0E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kebutuhan Untuk Siapa
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Untuk: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Text(
                        text = item.forWho ?: "Saya Sendiri",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kebutuhan Untuk Apa / Keperluan
            Text(
                text = "Keperluan:",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.purpose,
                fontSize = 13.sp,
                color = TextPrimary,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Jumlah & Estimasi Biaya
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Jumlah: ${item.quantity ?: 1} unit",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (item.estimatedPrice != null && item.estimatedPrice!! > 0) {
                    Text(
                        text = "Estimasi: Rp ${item.estimatedPrice!!.toLong().toString().reversed().chunked(3).joinToString(".").reversed()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            // Spesifikasi jika ada
            if (!item.specifications.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Spesifikasi: ${item.specifications}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Status Konversi Aset (Jika Sudah Dibeli)
            if (status == "purchased") {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Barang sudah dibeli & resmi menjadi aset perusahaan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16A34A)
                        )
                        if (item.asset != null && !item.asset.inventoryNumber.isNullOrEmpty()) {
                            Text(
                                text = "No. Inventaris: ${item.asset.inventoryNumber}",
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Tombol & Progress Approval
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDetailClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lihat Detail & Proses Approval",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = item.currentApprovalProgressSummary,
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
    }
}

private data class AssetItemDraft(
    val key: Long = kotlin.random.Random.nextLong(),
    val itemName: String = "",
    val itemSize: String = "",
    val quantityText: String = "1",
    val estimatedPriceText: String = "",
    val specifications: String = ""
)

/**
 * Modal Bottom Sheet Form Pengajuan Aset
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetRequestBottomSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        type: String,
        items: List<AssetRequestItemCreate>,
        forWho: String?,
        purpose: String
    ) -> Unit
) {
    var requestType by remember { mutableStateOf("kebutuhan_rutin") }
    var isForSelf by remember { mutableStateOf(true) }
    var forWhoText by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }

    var itemsList by remember {
        mutableStateOf(listOf(AssetItemDraft()))
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    fun updateItem(index: Int, transform: (AssetItemDraft) -> AssetItemDraft) {
        itemsList = itemsList.mapIndexed { i, draft ->
            if (i == index) transform(draft) else draft
        }
    }

    fun addItem() {
        itemsList = itemsList + AssetItemDraft()
    }

    fun removeItem(index: Int) {
        if (itemsList.size > 1) {
            itemsList = itemsList.filterIndexed { i, _ -> i != index }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Form Pengajuan Perlengkapan & Aset",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Pengajuan multi-item untuk ATK, perbaikan sarana, dan peralatan kerja",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            if (validationError != null) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = validationError ?: "",
                        color = Danger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Pilihan Tipe Pengajuan
            Text(
                text = "Tipe Pengajuan *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = requestType == "kebutuhan_rutin",
                    onClick = { requestType = "kebutuhan_rutin" },
                    label = { Text("Rutin ATK", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = requestType == "kebutuhan_perbaikan",
                    onClick = { requestType = "kebutuhan_perbaikan" },
                    label = { Text("Perbaikan", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = requestType == "kebutuhan_karyawan_baru",
                    onClick = { requestType = "kebutuhan_karyawan_baru" },
                    label = { Text("Karyawan Baru", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = requestType == "pengadaan_baru",
                    onClick = { requestType = "pengadaan_baru" },
                    label = { Text("Alat Baru", fontSize = 11.sp) }
                )
            }

            // Kebutuhan Untuk Siapa
            Text(
                text = "Kebutuhan Untuk Siapa *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isForSelf,
                    onClick = { isForSelf = true },
                    label = { Text("Saya Sendiri") }
                )
                FilterChip(
                    selected = !isForSelf,
                    onClick = { isForSelf = false },
                    label = { Text(if (requestType == "kebutuhan_karyawan_baru") "Karyawan Baru" else "Unit / Lokasi Kerja") }
                )
            }

            if (!isForSelf) {
                OutlinedTextField(
                    value = forWhoText,
                    onValueChange = { forWhoText = it },
                    placeholder = {
                        Text(
                            if (requestType == "kebutuhan_karyawan_baru") "Nama karyawan baru yang dituju" else "Nama divisi / lokasi penempatan",
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            // Kebutuhan Untuk Apa / Keperluan
            Text(
                text = "Kebutuhan Untuk Apa (Keperluan) *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                placeholder = { Text("Jelaskan alasan dan keperluan penggunaan / perbaikan...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
                shape = RoundedCornerShape(8.dp),
                minLines = 2,
                maxLines = 4
            )

            // Section Header: Daftar Barang / Item
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Barang / Item (${itemsList.size}) *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                TextButton(
                    onClick = { addItem() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
            }

            // List of item cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsList.forEachIndexed { idx, itemDraft ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header per card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Item #${idx + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (itemsList.size > 1) {
                                    IconButton(
                                        onClick = { removeItem(idx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus Item",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Nama Barang
                            Text(
                                text = "Nama Barang *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            OutlinedTextField(
                                value = itemDraft.itemName,
                                onValueChange = { newName ->
                                    updateItem(idx) { it.copy(itemName = newName) }
                                },
                                placeholder = {
                                    Text(
                                        when (requestType) {
                                            "kebutuhan_perbaikan" -> "Contoh: Kabel Listrik NYM / Lampu TL LED"
                                            "kebutuhan_rutin" -> "Contoh: Kertas HVS A4 / Spidol Whiteboard"
                                            else -> "Contoh: Laptop Core i5 / Meja Kerja"
                                        },
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            // Keterangan Ukuran (Opsional / Wajib perbaikan)
                            val isRepair = requestType == "kebutuhan_perbaikan"
                            Text(
                                text = if (isRepair) "Keterangan Ukuran * (Wajib perbaikan)" else "Keterangan Ukuran (Opsional)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            OutlinedTextField(
                                value = itemDraft.itemSize,
                                onValueChange = { newSize ->
                                    updateItem(idx) { it.copy(itemSize = newSize) }
                                },
                                placeholder = {
                                    Text("Contoh: 50 Meter / 18 Watt / Diameter 2 inch", fontSize = 12.sp)
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            // Jumlah & Estimasi Harga
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Jumlah Unit *",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    OutlinedTextField(
                                        value = itemDraft.quantityText,
                                        onValueChange = { newQty ->
                                            updateItem(idx) { it.copy(quantityText = newQty.filter { ch -> ch.isDigit() }) }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }

                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(
                                        text = "Estimasi Harga (Rp)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    OutlinedTextField(
                                        value = itemDraft.estimatedPriceText,
                                        onValueChange = { newPrice ->
                                            updateItem(idx) { it.copy(estimatedPriceText = newPrice.filter { ch -> ch.isDigit() }) }
                                        },
                                        placeholder = { Text("0", fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }
                            }

                            // Spesifikasi / Catatan
                            Text(
                                text = "Spesifikasi / Merk (Opsional)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            OutlinedTextField(
                                value = itemDraft.specifications,
                                onValueChange = { newSpecs ->
                                    updateItem(idx) { it.copy(specifications = newSpecs) }
                                },
                                placeholder = { Text("Merk, tipe, atau spesifikasi teknis", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tombol Tambah Barang
            OutlinedButton(
                onClick = { addItem() },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Tambah Barang / Item Lainnya", fontWeight = FontWeight.Bold, color = Primary, fontSize = 13.sp)
            }

            // Summary Card jika lebih dari 1 item atau ada harga
            val totalQtySum = itemsList.sumOf { it.quantityText.toIntOrNull() ?: 1 }
            val totalEstSum = itemsList.sumOf { it.estimatedPriceText.toDoubleOrNull() ?: 0.0 }
            if (itemsList.size > 1 || totalEstSum > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Jenis Barang:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("${itemsList.size} item", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Unit:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("$totalQtySum unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                        if (totalEstSum > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val totStr = "Rp " + totalEstSum.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Estimasi Biaya:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(totStr, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        // Validasi
                        val blankIndex = itemsList.indexOfFirst { it.itemName.isBlank() }
                        if (blankIndex >= 0) {
                            validationError = "Nama barang pada Item #${blankIndex + 1} wajib diisi."
                            return@Button
                        }
                        if (requestType == "kebutuhan_perbaikan") {
                            val blankSizeIndex = itemsList.indexOfFirst { it.itemSize.isBlank() }
                            if (blankSizeIndex >= 0) {
                                validationError = "Keterangan ukuran pada Item #${blankSizeIndex + 1} wajib diisi untuk perbaikan."
                                return@Button
                            }
                        }
                        if (!isForSelf && forWhoText.isBlank()) {
                            validationError = "Nama penerima / unit kerja wajib diisi."
                            return@Button
                        }
                        if (purpose.isBlank()) {
                            validationError = "Kebutuhan untuk apa / keperluan wajib diisi."
                            return@Button
                        }

                        val finalForWho = if (isForSelf) "Saya Sendiri" else forWhoText.trim()
                        val assetItems = itemsList.map { draft ->
                            val q = draft.quantityText.toIntOrNull() ?: 1
                            val ep = draft.estimatedPriceText.toDoubleOrNull()
                            AssetRequestItemCreate(
                                itemName = draft.itemName.trim(),
                                itemSize = draft.itemSize.trim().ifBlank { null },
                                quantity = q.coerceAtLeast(1),
                                unitPrice = if (ep != null && ep > 0) (ep / q.coerceAtLeast(1)) else null,
                                estimatedPrice = ep,
                                notes = draft.specifications.trim().ifBlank { null }
                            )
                        }

                        onSubmit(
                            requestType,
                            assetItems,
                            finalForWho,
                            purpose.trim()
                        )
                    },
                    modifier = Modifier.weight(1.5f).height(46.dp),
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Kirim Pengajuan (${itemsList.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Item card layout matching item_asset
 */
@Composable
private fun AssetItemCard(item: AssetItem) {
    val status = item.status ?: "-"
    val (statusBg, statusTextColor) = when (status) {
        "Tersedia" -> Pair(Color(0xFFDCFCE7), Color(0xFF16A34A))
        "Dipinjamkan" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
        "Pemeliharaan" -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
        else -> Pair(Color(0xFFF3F4F6), Color(0xFF6B7280))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Asset Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status,
                        color = statusTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Asset Type
            Text(
                text = "Jenis: ${item.type ?: "-"}",
                color = TextSecondary,
                fontSize = 14.sp
            )

            // Serial Number
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Serial: ${item.serialNumber ?: "-"}",
                color = TextSecondary,
                fontSize = 14.sp
            )

            // Inventory Number
            if (!item.inventoryNumber.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No. Inventaris: ${item.inventoryNumber}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Purchase Date
            if (!item.purchaseDate.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tgl Pembelian: ${item.purchaseDate}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Notes
            if (!item.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan: ${item.notes}",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Bottom Sheet Detail Permintaan Aset & Status Alur Persetujuan (Approval Workflow Timeline)
 */
@Composable
fun AssetRequestDetailBottomSheetContent(
    item: AssetRequestItem,
    onClose: () -> Unit
) {
    val items = item.allItems
    val approvals = item.approvals?.sortedBy { it.level ?: 1 } ?: emptyList()
    val totalEst = item.totalEstimatedPrice
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
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Detail & Proses Approval",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Pengajuan #${item.id} · Tgl: ${DateTimeUtils.formatDisplayDate(item.createdAt)}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Ringkasan Pengajuan (Summary Card)
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
                            val reqType = item.requestType ?: "kebutuhan_rutin"
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

                            val (statusBg, statusTextColor) = when (item.status?.lowercase()) {
                                "approved" -> Pair(Color(0xFFDCFCE7), Color(0xFF16A34A))
                                "rejected" -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
                                "in_review" -> Pair(Color(0xFFDBEAFE), Color(0xFF2563EB))
                                "purchased" -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
                                else -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = (item.status ?: "PENDING").uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (totalEstStr != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Estimasi Biaya",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = totalEstStr,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))

                        if (!item.purpose.isNullOrBlank()) {
                            Text(
                                text = "Tujuan / Alasan Kebutuhan:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = item.purpose,
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!item.forWho.isNullOrBlank()) {
                            Text(
                                text = "Untuk Siapa / Penempatan:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            val placement = item.placement?.takeIf { it != item.forWho }
                            val placementStr = if (placement != null) "${item.forWho} ($placement)" else item.forWho
                            Text(
                                text = placementStr,
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!item.notes.isNullOrBlank()) {
                            Text(
                                text = "Keterangan / Catatan:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = item.notes,
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        if (!item.rejectionReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Alasan Penolakan:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.rejectionReason,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Alur & Status Persetujuan (Workflow Timeline)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alur Persetujuan (${approvals.size} Tahap)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            if (approvals.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Informasi alur persetujuan sedang diproses.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                items(approvals.size) { index ->
                    val step = approvals[index]
                    val isFirst = index == 0
                    val isLast = index == approvals.size - 1

                    AssetApprovalTimelineItem(
                        step = step,
                        index = index,
                        isFirst = isFirst,
                        isLast = isLast
                    )
                }
            }

            // 3. Rincian Barang
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
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada rincian barang",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
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

            // 4. Status Pembelian / Inventarisasi (Bila sudah terealisasi)
            val isPurchased = item.status.lowercase() == "purchased" || item.asset != null
            val invNum = item.asset?.inventoryNumber
            val purDate = item.purchasedAt ?: item.asset?.purchaseDate
            if (isPurchased) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF7E22CE),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Status Pengadaan Selesai (Terealisasi)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B21A8)
                                )
                            }
                            if (!invNum.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Nomor Inventaris: $invNum",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4C1D95),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (!purDate.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tanggal Pembelian: ${DateTimeUtils.formatDisplayDate(purDate)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B21A8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item Timeline untuk Setiap Tahap Approval Permintaan Aset
 */
@Composable
fun AssetApprovalTimelineItem(
    step: AssetRequestApprovalItem,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean
) {
    val statusStr = step.status ?: "PENDING"
    val statusColor = when (statusStr.lowercase()) {
        "approve", "approved" -> Color(0xFF16A34A)
        "reject", "rejected" -> Color(0xFFDC2626)
        "skipped" -> Color(0xFF64748B)
        else -> Color(0xFFF59E0B)
    }

    val approverName = step.approver?.name ?: step.roleName ?: "Approver"
    val actionByText = when (statusStr.lowercase()) {
        "approved" -> "Disetujui oleh: $approverName"
        "rejected" -> "Ditolak oleh: $approverName"
        "skipped" -> "Dilewati (Auto-Approved): $approverName"
        else -> "Menunggu Persetujuan: $approverName"
    }

    val dateFormatted = DateTimeUtils.formatDisplayDate(step.actionDate ?: step.createdAt)

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
                        text = "Tahap ${step.level ?: (index + 1)}: ${step.roleName ?: "Approver"}",
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

                if (step.actionDate != null || step.createdAt != null) {
                    Text(
                        text = "Tanggal: $dateFormatted",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (!step.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Catatan: ${step.notes}",
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
