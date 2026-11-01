package com.jangeby.system.ui.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.CalendarItem
import com.jangeby.system.data.remote.MilestoneItemData
import com.jangeby.system.data.remote.MilestoneResponseData
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.jangeby.system.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: (action: String) -> Unit = {},
    onNavigateToOvertime: () -> Unit = {},
    onNavigateToAnnouncement: (initialTab: Int) -> Unit = {},
    viewModel: AttendanceViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val calendarItems by viewModel.calendarItems.collectAsState()
    val todayItem by viewModel.todayItem.collectAsState()
    val todayDateStr by viewModel.todayDateStr.collectAsState()
    val isLoadingData by viewModel.isLoadingData.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val appSettings: AppSettings = koinInject()
    var showLateReasonSheet by remember { mutableStateOf(false) }
    var selectedLateDate by remember { mutableStateOf("") }
    var currentLateReasonInput by remember { mutableStateOf("") }
    var showMilestonePopup by remember { mutableStateOf(false) }
    var todayMilestonesForPopup by remember { mutableStateOf<List<MilestoneItemData>>(emptyList()) }

    // Pull-to-refresh state
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = isLoadingData

    LaunchedEffect(Unit) {
        viewModel.loadShiftCalendar()

        // Check milestone celebrations today (>= 07:00, show once per day)
        val now = DateTimeUtils.nowInJakarta()
        val todayStr = DateTimeUtils.todayDateString()
        val isAfter7Am = now.hour >= 7
        val alreadyShownToday = (appSettings.lastSeenMilestonePopupDate == todayStr)

        if (isAfter7Am && !alreadyShownToday) {
            try {
                val client = ApiClient.create()
                val mileRes = client.get(ApiClient.BASE_URL + "employee/milestones")
                if (mileRes.status.value in 200..299) {
                    val mileData: ApiResponse<MilestoneResponseData> = mileRes.body()
                    val todayList = mileData.data?.milestones?.filter { it.isToday } ?: emptyList()
                    if (todayList.isNotEmpty()) {
                        todayMilestonesForPopup = todayList
                        showMilestonePopup = true
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is AttendanceState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.loadShiftCalendar()
                viewModel.resetState()
            }
            is AttendanceState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Default kosong — tidak ada mock data, biarkan loading terlihat jelas
    val displayCalendar = calendarItems
    val activeToday = todayItem

    Scaffold(
        topBar = {
            HrmsTopBar(title = "Jadwal & Presensi", onNavigateBack = onNavigateBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadShiftCalendar() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
            // Header Banner Shift Hari Ini (bindTodayStatus logic)
            item {
                val hasUnclosed = calendarItems.any {
                    it.attendance?.clockIn != null && it.attendance.clockOut.isNullOrBlank() && (it.date ?: "") < todayDateStr
                }
                TodayAttendanceHeader(
                    todayItem = activeToday,
                    todayStr = todayDateStr,
                    isLoading = isLoadingData || state is AttendanceState.Loading,
                    onClockIn = { onNavigateToMap("clock_in") },
                    onClockOut = { onNavigateToMap("clock_out") },
                    onOvertimeClockIn = { onNavigateToMap("overtime_clock_in") },
                    onOvertimeClockOut = { onNavigateToMap("overtime_clock_out") },
                    onApplyOvertime = onNavigateToOvertime,
                    onEditLateReason = {
                        selectedLateDate = activeToday?.date ?: todayDateStr
                        currentLateReasonInput = activeToday?.attendance?.lateReason ?: activeToday?.attendance?.notes ?: activeToday?.lateReason ?: ""
                        showLateReasonSheet = true
                    },
                    hasUnclosedPrevious = hasUnclosed,
                    onSwitchToToday = { viewModel.switchToToday() },
                    onSwitchToUnclosed = { viewModel.switchToUnclosedPrevious() }
                )
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Jadwal Shift 1 Bulan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = formatMonthHeader(todayDateStr),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }
            }

            // 1-Month Shift Calendar Rows (HistoryAdapter logic)
            if (isLoadingData && displayCalendar.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Memuat data jadwal...",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else if (displayCalendar.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum ada data jadwal presensi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tarik ke bawah untuk memperbarui",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                items(displayCalendar) { item ->
                    ShiftCalendarRow(
                        item = item,
                        onEditLateReason = {
                            selectedLateDate = item.date ?: ""
                            currentLateReasonInput = item.attendance?.lateReason ?: item.attendance?.notes ?: item.lateReason ?: ""
                            showLateReasonSheet = true
                        }
                    )
                }
            }
        }
    }
}

    if (showLateReasonSheet) {
        var inputReason by remember { mutableStateOf(currentLateReasonInput) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showLateReasonSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Alasan Keterlambatan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tanggal: $selectedLateDate • Akan diinfokan ke atasan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputReason,
                    onValueChange = { inputReason = it },
                    label = { Text("Alasan keterlambatan") },
                    placeholder = { Text("Contoh: Terjebak macet / kendaraan bermasalah") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showLateReasonSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            if (inputReason.isNotBlank()) {
                                showLateReasonSheet = false
                                viewModel.submitLateReason(selectedLateDate, inputReason)
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        enabled = inputReason.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }

    if (showMilestonePopup && todayMilestonesForPopup.isNotEmpty()) {
        MilestonePopupDialog(
            milestones = todayMilestonesForPopup,
            onDismiss = {
                showMilestonePopup = false
                appSettings.lastSeenMilestonePopupDate = DateTimeUtils.todayDateString()
            },
            onNavigateToWishes = {
                showMilestonePopup = false
                appSettings.lastSeenMilestonePopupDate = DateTimeUtils.todayDateString()
                onNavigateToAnnouncement(1)
            }
        )
    }
}

@Composable
fun TodayAttendanceHeader(
    todayItem: CalendarItem?,
    todayStr: String,
    isLoading: Boolean,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onOvertimeClockIn: () -> Unit,
    onOvertimeClockOut: () -> Unit,
    onApplyOvertime: (() -> Unit)? = null,
    onEditLateReason: (() -> Unit)? = null,
    hasUnclosedPrevious: Boolean = false,
    onSwitchToToday: (() -> Unit)? = null,
    onSwitchToUnclosed: (() -> Unit)? = null
) {
    val att = todayItem?.attendance
    val isCheckedIn = att != null && !att.clockIn.isNullOrBlank()
    val isCheckedOut = att != null && !att.clockOut.isNullOrBlank()

    val isOvernightOverlap = todayItem?.date != null && todayItem.date != todayStr && isCheckedIn && !isCheckedOut
    val dateHeaderStr = if (isOvernightOverlap) {
        "${formatDateHeader(todayItem.date)} • Belum Clock Out"
    } else {
        formatDateHeader(todayItem?.date ?: todayStr)
    }

    val tvStatusText = if (todayItem == null) {
        "Belum ada jadwal hari ini."
    } else {
        val shiftText = todayItem.shift?.name ?: if (todayItem.isHoliday == true) {
            if (isCheckedIn) "Masuk di Hari Libur" else "Libur / Off"
        } else "Libur / Off"
        val sStart = todayItem.shift?.startTime?.take(5) ?: "-"
        val sEnd = todayItem.shift?.endTime?.take(5) ?: "-"

        if (todayItem.shift == null) {
            shiftText
        } else {
            "$shiftText ($sStart - $sEnd)"
        }
    }

    val clockInStr = DateTimeUtils.formatTimeOnly(att?.clockIn)
    val clockOutStr = DateTimeUtils.formatTimeOnly(att?.clockOut)

    val hasApprovedOvertime = todayItem?.hasApprovedOvertime == true
    val isOvertimeIn = att != null && !att.overtimeIn.isNullOrBlank()
    val isOvertimeOut = att != null && !att.overtimeOut.isNullOrBlank()

    var isLate = (att?.lateMinutes ?: 0) > 0
    if (!isLate && att?.clockIn != null && todayItem?.shift?.startTime != null) {
        val inTime = DateTimeUtils.formatTimeOnly(att.clockIn)
        if (inTime != "-" && inTime > todayItem.shift.startTime.take(5)) {
            isLate = true
        }
    }

    // Button visibility flags based on AttendanceActivity.kt
    var showBtnClockIn = false
    var showBtnClockOut = false
    var showBtnOvertimeClockIn = false
    var showBtnOvertimeClockOut = false

    if (isCheckedOut) {
        showBtnClockIn = false
        showBtnClockOut = false
        if (hasApprovedOvertime || isOvertimeIn || isOvertimeOut) {
            if (isOvertimeOut) {
                showBtnOvertimeClockIn = false
                showBtnOvertimeClockOut = false
            } else if (isOvertimeIn) {
                showBtnOvertimeClockIn = false
                showBtnOvertimeClockOut = true
            } else {
                showBtnOvertimeClockIn = true
                showBtnOvertimeClockOut = false
            }
        }
    } else if (isCheckedIn) {
        showBtnClockIn = false
        showBtnClockOut = true
    } else {
        // Tetap izinkan Clock In meskipun hari ini dijadwalkan Libur/Off
        showBtnClockIn = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF007A87), Color(0xFF005662))))
                .padding(20.dp)
        ) {
            Column {
                // Header Date
                Text(
                    text = dateHeaderStr,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading && todayItem == null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Memuat data presensi hari ini...",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (todayItem == null || (todayItem.shift == null && todayItem.isHoliday != true && !isCheckedIn)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Belum ada jadwal hari ini.",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (todayItem.shift == null && todayItem.isHoliday == true && !isCheckedIn) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Libur / Off",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            Column {
                                Text("Clock In", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                Text(if (clockInStr != "-") "$clockInStr WIB" else "--:--", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Clock Out", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                Text(if (clockOutStr != "-") "$clockOutStr WIB" else "--:--", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Late Status Chip
                    if (isCheckedIn) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLate) Color(0xFFEF4444) else Color(0xFF10B981)
                        ) {
                            Text(
                                text = if (isLate) "Telat (${att?.lateMinutes ?: 0} mnt)" else "Tepat Waktu",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (todayItem?.shift != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "Belum Presensi",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Alasan Terlambat Display
                val lateReasonText = att?.lateReason ?: att?.notes ?: todayItem?.lateReason
                if (isLate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onEditLateReason != null) Modifier.clickable { onEditLateReason() }
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFFD1D1),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alasan Terlambat:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD1D1)
                                )
                                Text(
                                    text = if (!lateReasonText.isNullOrBlank()) lateReasonText else "Klik untuk menambahkan alasan terlambat",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = if (!lateReasonText.isNullOrBlank()) FontWeight.Normal else FontWeight.Light
                                )
                            }
                            if (onEditLateReason != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Alasan",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }


                // Action Buttons matching bindTodayStatus logic
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    }
                } else if (showBtnClockIn) {
                    val isOffDay = todayItem?.shift == null
                    val btnTitle = if (isOffDay) "CLOCK IN (MASUK LIBUR)" else "CLOCK IN HARI INI"
                    Button(
                        onClick = onClockIn,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF007A87))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(btnTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (hasUnclosedPrevious && onSwitchToUnclosed != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.TextButton(
                            onClick = onSwitchToUnclosed,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ada sesi sebelumnya yang belum Clock Out • Beralih", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        }
                    }
                } else if (showBtnClockOut) {
                    val outBtnTitle = if (isOvernightOverlap) "CLOCK OUT (${formatDateHeader(todayItem?.date ?: "")})" else "CLOCK OUT HARI INI"
                    Button(
                        onClick = onClockOut,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(outBtnTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (isOvernightOverlap && onSwitchToToday != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.TextButton(
                            onClick = onSwitchToToday,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lupa Clock Out? Mulai Clock In Hari Ini", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        }
                    }
                } else if (showBtnOvertimeClockIn) {
                    Button(
                        onClick = onOvertimeClockIn,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLOCK IN LEMBUR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (showBtnOvertimeClockOut) {
                    Button(
                        onClick = onOvertimeClockOut,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLOCK OUT LEMBUR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (isCheckedOut) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isOvertimeOut) "✓ Presensi & Lembur Hari Ini Selesai" else "✓ Presensi Hari Ini Selesai",
                                modifier = Modifier.padding(12.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        if (!hasApprovedOvertime && !isOvertimeIn && !isOvertimeOut && onApplyOvertime != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onApplyOvertime,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LEMBUR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftCalendarRow(
    item: CalendarItem,
    onEditLateReason: (() -> Unit)? = null
) {
    // Exact HistoryAdapter.kt logic
    val shift = item.shift
    val isHoliday = item.isHoliday == true

    // Format date YYYY-MM-DD -> DD-MMM-YYYY (e.g. 05-Sep-2026)
    val dateParts = item.date?.split("-")
    val dateFormatted = if (dateParts?.size == 3) {
        val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val mIdx = dateParts[1].toIntOrNull() ?: 0
        val mStr = if (mIdx in 1..12) months[mIdx] else dateParts[1]
        "${dateParts[2]}-$mStr-${dateParts[0]}"
    } else {
        item.date ?: "-"
    }

    val shiftTimeStr = if (shift?.startTime != null && shift.endTime != null)
        "${shift.startTime.take(5)} - ${shift.endTime.take(5)}" else "-"

    val att = item.attendance
    val clockInStr = DateTimeUtils.formatTimeOnly(att?.clockIn)

    val outLabel = "Out: "
    val clockOutStr = DateTimeUtils.formatTimeOnly(att?.clockOut)

    val hasClockedIn = att?.clockIn != null
    val isEffectivelyHoliday = isHoliday && !hasClockedIn

    val statusInfoText = if (isHoliday) {
        if (hasClockedIn) "Hadir (Libur)" else "Libur"
    } else {
        att?.status?.replaceFirstChar { it.uppercase() } ?: "Working Day"
    }

    var isLate = (att?.lateMinutes ?: 0) > 0
    val shiftStartStr = shift?.startTime?.take(5)
    if (!isLate && !isHoliday && clockInStr != "-" && shiftStartStr != null && clockInStr > shiftStartStr) {
        isLate = true
    }

    val finalStatusText = if (isLate) "$statusInfoText (Telat)" else statusInfoText
    val lateRedColor = Color(0xFFFF5252)

    val otIn = att?.overtimeIn?.let { DateTimeUtils.formatTimeOnly(it).takeIf { t -> t != "-" } }
    val otOut = att?.overtimeOut?.let { DateTimeUtils.formatTimeOnly(it).takeIf { t -> t != "-" } }
    val hasOvertime = otIn != null || otOut != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isEffectivelyHoliday) Color(0xFFF1F5F9) else Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isEffectivelyHoliday) Color(0xFF64748B) else Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = dateFormatted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row {
                            Text(
                                text = "In: $clockInStr | ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isLate) lateRedColor else Primary
                            )
                            Text(
                                text = "$outLabel$clockOutStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                // Status Chip with Telat highlighting
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLate) Color(0xFFFFE4E6) else if (isEffectivelyHoliday) Color(0xFFF1F5F9) else Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = finalStatusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (isLate) lateRedColor else if (isEffectivelyHoliday) Color(0xFF64748B) else Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Alasan Terlambat Row
            val lateReasonText = att?.lateReason ?: att?.notes ?: item.lateReason
            if (isLate) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF1F2),
                    border = BorderStroke(0.5.dp, Color(0xFFFECDD3)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onEditLateReason != null) Modifier.clickable { onEditLateReason() }
                            else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alasan: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                        Text(
                            text = if (!lateReasonText.isNullOrBlank()) lateReasonText else "Belum ada alasan (klik untuk isi)",
                            fontSize = 11.sp,
                            color = if (!lateReasonText.isNullOrBlank()) Color(0xFF9F1239) else Color(0xFFE11D48).copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (onEditLateReason != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Alasan",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Overtime row matching HistoryAdapter layoutOvertime
            if (hasOvertime) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lembur: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                    Text(
                        text = "In: ${otIn ?: "-"} | Out: ${otOut ?: "-"}",
                        fontSize = 11.sp,
                        color = Color(0xFF5B21B6),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun formatDateHeader(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val parts = dateStr.split("-")
    if (parts.size == 3) {
        val months = listOf("", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        val mIdx = parts[1].toIntOrNull() ?: 0
        val mStr = if (mIdx in 1..12) months[mIdx] else parts[1]
        return "${parts[2]} $mStr ${parts[0]}"
    }
    return dateStr
}

fun formatMonthHeader(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val parts = dateStr.split("-")
    if (parts.size >= 2) {
        val months = listOf("", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        val mIdx = parts[1].toIntOrNull() ?: 0
        val mStr = if (mIdx in 1..12) months[mIdx] else parts[1]
        return "$mStr ${parts[0]}"
    }
    return dateStr
}
