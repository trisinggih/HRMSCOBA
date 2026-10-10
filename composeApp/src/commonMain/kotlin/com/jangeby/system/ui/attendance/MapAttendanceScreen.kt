package com.jangeby.system.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.jangeby.system.hardware.rememberCameraLauncher
import com.jangeby.system.hardware.rememberLocationLauncher
import com.jangeby.system.hardware.FaceCameraView
import com.jangeby.system.ui.component.MapViewComponent
import com.jangeby.system.ui.component.BranchItemData
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapAttendanceScreen(
    action: String,
    onNavigateBack: () -> Unit,
    viewModel: AttendanceViewModel = koinViewModel()
) {
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var locationFetched by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showInlineCamera by remember { mutableStateOf(false) }
    
    val state by viewModel.state.collectAsState()
    val isBranchLoading by viewModel.isBranchLoading.collectAsState()
    val isBranchLoadFailed by viewModel.isBranchLoadFailed.collectAsState()
    
    LaunchedEffect(state) {
        when (state) {
            is AttendanceState.Success -> {
                isSubmitting = false
                onNavigateBack()
            }
            is AttendanceState.Error -> {
                isSubmitting = false
            }
            is AttendanceState.LatePrompt -> {
                isSubmitting = false
            }
            else -> {}
        }
    }

    var statusText by remember { mutableStateOf("Mencari lokasi...") }
    var statusColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.Gray) }
    var isLocationValid by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableStateOf(0L) }

    if (showInlineCamera) {
        FaceCameraView(
            title = if (action.contains("overtime")) "Lembur" else "Absensi",
            buttonText = if (action.contains("in")) "Clock In" else "Clock Out",
            latitude = latitude,
            longitude = longitude,
            onPhotoCaptured = { base64 ->
                showInlineCamera = false
                if (base64 != null) {
                    isSubmitting = true
                    when (action) {
                        "clock_in" -> viewModel.submitClockIn(latitude, longitude, false, base64)
                        "clock_out" -> viewModel.submitClockOut(latitude, longitude, false, base64)
                        "overtime_clock_in" -> viewModel.submitClockIn(latitude, longitude, true, base64)
                        "overtime_clock_out" -> viewModel.submitClockOut(latitude, longitude, true, base64)
                    }
                } else {
                    statusText = "Gagal mengambil foto wajah"
                    statusColor = androidx.compose.ui.graphics.Color.Red
                }
            },
            onClose = {
                showInlineCamera = false
            }
        )
        return
    }

    val cameraLauncher = rememberCameraLauncher { result ->
        if (result.isSuccess && result.base64Image != null) {
            isSubmitting = true
            when (action) {
                "clock_in" -> viewModel.submitClockIn(latitude, longitude, false, result.base64Image)
                "clock_out" -> viewModel.submitClockOut(latitude, longitude, false, result.base64Image)
                "overtime_clock_in" -> viewModel.submitClockIn(latitude, longitude, true, result.base64Image)
                "overtime_clock_out" -> viewModel.submitClockOut(latitude, longitude, true, result.base64Image)
            }
        } else if (!result.isSuccess && !result.errorMessage.isNullOrEmpty()) {
            statusText = result.errorMessage
            statusColor = androidx.compose.ui.graphics.Color.Red
        }
    }

    val primaryBranch by viewModel.primaryBranch.collectAsState()
    val allBranches by viewModel.allBranches.collectAsState()
    val canClockInAnywhere by viewModel.canClockInAnywhere.collectAsState()

    // Jika data cabang belum ada saat screen dibuka, paksa reload
    LaunchedEffect(Unit) {
        if (primaryBranch == null && allBranches.isEmpty() && !isBranchLoading) {
            viewModel.loadBranchData()
        }
    }


    fun validateLocation(lat: Double, lon: Double) {
        val pBranch = primaryBranch
        val pLat = pBranch?.latitude?.toDoubleOrNull()
        val pLon = pBranch?.longitude?.toDoubleOrNull()
        val pRadius = pBranch?.radius ?: 100.0

        var isInsideBranch = false
        var activeBranchName = ""
        var minDistance = Double.MAX_VALUE

        // 1. Check primary branch
        if (pBranch != null && pLat != null && pLon != null) {
            val dist = com.jangeby.system.utils.LocationUtils.distanceInMeters(lat, lon, pLat, pLon)
            if (dist < minDistance) minDistance = dist
            if (dist <= pRadius) {
                isInsideBranch = true
                activeBranchName = pBranch.name ?: "Kantor Utama"
            }
        }

        // 2. Check all loaded branches
        if (!isInsideBranch) {
            for (b in allBranches) {
                val bLat = b.latitude?.toDoubleOrNull()
                val bLon = b.longitude?.toDoubleOrNull()
                if (bLat == null || bLon == null) continue

                val dist = com.jangeby.system.utils.LocationUtils.distanceInMeters(lat, lon, bLat, bLon)
                if (dist < minDistance) minDistance = dist
                val r = b.attendanceRadius ?: 100.0
                if (dist <= r) {
                    isInsideBranch = true
                    activeBranchName = b.name ?: "Cabang"
                    break
                }
            }
        }

        val radiusMax = pRadius.toInt()
        val distSuffix = if (minDistance != Double.MAX_VALUE) " (Jarak: ${minDistance.toInt()}m / Radius Max: ${radiusMax}m)" else ""

        if (isInsideBranch) {
            statusText = "Di $activeBranchName$distSuffix"
            statusColor = androidx.compose.ui.graphics.Color(0xFF388E3C)
            isLocationValid = true
        } else if (canClockInAnywhere) {
            statusText = "Bebas Presensi$distSuffix"
            statusColor = androidx.compose.ui.graphics.Color(0xFF388E3C)
            isLocationValid = true
        } else if (pBranch != null || allBranches.isNotEmpty()) {
            statusText = "Di luar area kantor$distSuffix"
            statusColor = androidx.compose.ui.graphics.Color.Red
            isLocationValid = false
        } else {
            statusText = "Data cabang belum dimuat."
            statusColor = androidx.compose.ui.graphics.Color.Red
            isLocationValid = false
        }
    }

    val locationLauncher = rememberLocationLauncher { result ->
        isRefreshing = false
        if (result.isSuccess) {
            latitude = result.latitude
            longitude = result.longitude
            locationFetched = true
            recenterTrigger++
            validateLocation(result.latitude, result.longitude)
        } else {
            statusText = "Gagal mengambil lokasi: ${result.errorMessage}"
            statusColor = androidx.compose.ui.graphics.Color.Red
            isLocationValid = false
        }
    }

    val doRefreshLocation = {
        isRefreshing = true
        statusText = "Memperbarui lokasi..."
        statusColor = androidx.compose.ui.graphics.Color.Gray
        // Jika data cabang belum ada, muat ulang bersamaan
        if (primaryBranch == null && allBranches.isEmpty() && !isBranchLoading) {
            viewModel.loadBranchData()
        }
        locationLauncher.fetchLocation()
    }

    LaunchedEffect(Unit) {
        doRefreshLocation()
    }

    LaunchedEffect(primaryBranch, allBranches, canClockInAnywhere) {
        if (locationFetched) {
            validateLocation(latitude, longitude)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verifikasi Lokasi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = doRefreshLocation,
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Lokasi")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = doRefreshLocation,
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (locationFetched) {
                    val allBranchItems = remember(primaryBranch, allBranches) {
                        val list = mutableListOf<BranchItemData>()
                        val pLat = primaryBranch?.latitude?.toDoubleOrNull()
                        val pLon = primaryBranch?.longitude?.toDoubleOrNull()
                        val pR = primaryBranch?.radius ?: 100.0
                        if (pLat != null && pLon != null && pLat != 0.0 && pLon != 0.0) {
                            list.add(BranchItemData(pLat, pLon, pR, primaryBranch?.name ?: "Kantor Utama"))
                        }
                        for (b in allBranches) {
                            val bLat = b.latitude?.toDoubleOrNull()
                            val bLon = b.longitude?.toDoubleOrNull()
                            val bR = b.attendanceRadius ?: 100.0
                            if (bLat != null && bLon != null && bLat != 0.0 && bLon != 0.0) {
                                if (list.none { it.latitude == bLat && it.longitude == bLon }) {
                                    list.add(BranchItemData(bLat, bLon, bR, b.name ?: "Cabang"))
                                }
                            }
                        }
                        list
                    }

                    val firstBranch = allBranchItems.firstOrNull()

                    MapViewComponent(
                        modifier = Modifier.fillMaxSize(),
                        latitude = latitude,
                        longitude = longitude,
                        branchLatitude = firstBranch?.latitude,
                        branchLongitude = firstBranch?.longitude,
                        radius = firstBranch?.radius ?: 100.0,
                        branches = allBranchItems,
                        recenterTrigger = recenterTrigger
                    )
                    
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Status lokasi / cabang
                            when {
                                isBranchLoading && primaryBranch == null && allBranches.isEmpty() -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Memuat data cabang...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                isBranchLoadFailed -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            statusText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = statusColor
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.loadBranchData() },
                                            enabled = !isBranchLoading
                                        ) {
                                            if (isBranchLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text("Muat Ulang Data Cabang")
                                        }
                                    }
                                }
                                else -> {
                                    Text(
                                        statusText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = statusColor
                                    )
                                }
                            }
                            if (latitude != 0.0 || longitude != 0.0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Koordinat: ${((latitude * 1000000.0).toLong() / 1000000.0)}, ${((longitude * 1000000.0).toLong() / 1000000.0)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = doRefreshLocation,
                                    modifier = Modifier.weight(1f),
                                    enabled = !isRefreshing && !isSubmitting && !isBranchLoading
                                ) {
                                    if (isRefreshing || isBranchLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        when {
                                            isBranchLoading -> "Memuat..."
                                            isRefreshing -> "Mencari..."
                                            else -> "Refresh"
                                        }
                                    )
                                }
                                Button(
                                    onClick = { showInlineCamera = true },
                                    modifier = Modifier.weight(1.5f),
                                    enabled = !isSubmitting && !isRefreshing && !isBranchLoading && isLocationValid
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("Lanjut Absen (Foto)")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    if (isSubmitting || state is AttendanceState.Loading) {
        Dialog(
            onDismissRequest = {}
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Mengirim data presensi...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mohon tunggu sebentar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (state is AttendanceState.LatePrompt) {
        val lateState = state as AttendanceState.LatePrompt
        var reason by remember { mutableStateOf("") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                viewModel.dismissLatePrompt()
                onNavigateBack()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Anda Terlambat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Clock-in tercatat terlambat. Silakan masukkan alasan keterlambatan Anda untuk diajukan ke atasan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan keterlambatan") },
                    placeholder = { Text("Contoh: Terjebak macet / keperluan mendadak") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.dismissLatePrompt()
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            if (reason.isNotBlank()) {
                                isSubmitting = true
                                viewModel.submitLateReason(lateState.date, reason)
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        enabled = reason.isNotBlank() && !isSubmitting
                    ) {
                        Text("Kirim")
                    }
                }
            }
        }
    }

    if (state is AttendanceState.Error) {
        val err = state as AttendanceState.Error
        val isCancellationError = err.message.contains("Parent job is Completed", ignoreCase = true) ||
                err.message.contains("JobCancellationException", ignoreCase = true)

        if (isCancellationError) {
            LaunchedEffect(err) {
                viewModel.resetState()
            }
        } else {
            AlertDialog(
                onDismissRequest = {
                    viewModel.resetState()
                },
                title = {
                    Text(
                        text = "Gagal Presensi",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(err.message)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetState()
                        }
                    ) {
                        Text("Coba Lagi")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetState()
                            onNavigateBack()
                        }
                    ) {
                        Text("Kembali ke Jadwal")
                    }
                }
            )
        }
    }
}
