package com.jangeby.system.ui.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.jangeby.system.data.remote.CalendarItem
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: (action: String) -> Unit = {},
    viewModel: AttendanceViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val calendarItems by viewModel.calendarItems.collectAsState()
    val todayItem by viewModel.todayItem.collectAsState()
    val todayDateStr by viewModel.todayDateStr.collectAsState()
    val isLoadingData by viewModel.isLoadingData.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLateReasonSheet by remember { mutableStateOf(false) }
    var selectedLateDate by remember { mutableStateOf("") }
    var currentLateReasonInput by remember { mutableStateOf("") }

    // Pull-to-refresh state
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = isLoadingData

    LaunchedEffect(Unit) {
        viewModel.loadShiftCalendar()
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
                TodayAttendanceHeader(
                    todayItem = activeToday,
                    todayStr = todayDateStr,
                    isLoading = isLoadingData || state is AttendanceState.Loading,
                    onClockIn = { onNavigateToMap("clock_in") },
                    onClockOut = { onNavigateToMap("clock_out") },
                    onOvertimeClockIn = { onNavigateToMap("overtime_clock_in") },
                    onOvertimeClockOut = { onNavigateToMap("overtime_clock_out") },
                    onEditLateReason = {
                        selectedLateDate = activeToday?.date ?: todayDateStr
                        currentLateReasonInput = activeToday?.attendance?.lateReason ?: activeToday?.attendance?.notes ?: activeToday?.lateReason ?: ""
                        showLateReasonSheet = true
                    }
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
                    .padding(bottom = 32.dp),
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
                    text = "Tanggal: $selectedLateDate",
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
    onEditLateReason: (() -> Unit)? = null
) {
    // Exact bindTodayStatus logic from AttendanceActivity.kt
    val dateHeaderStr = formatDateHeader(todayItem?.date ?: todayStr)

    val tvStatusText = if (todayItem == null) {
        "Belum ada jadwal hari ini."
    } else {
        val shiftText = todayItem.shift?.name ?: if (todayItem.isHoliday == true) "Libur Nasional" else "Libur/Off"
        val sStart = todayItem.shift?.startTime?.take(5) ?: "-"
        val sEnd = todayItem.shift?.endTime?.take(5) ?: "-"

        if (todayItem.shift == null) {
            shiftText
        } else {
            "$shiftText ($sStart - $sEnd)"
        }
    }

    val att = todayItem?.attendance
    val clockInStr = att?.clockIn?.take(5) ?: "-"
    val clockOutStr = att?.clockOut?.take(5) ?: "-"

    val isCheckedIn = att != null && !att.clockIn.isNullOrBlank()
    val isCheckedOut = att != null && !att.clockOut.isNullOrBlank()

    val hasApprovedOvertime = todayItem?.hasApprovedOvertime == true
    val isOvertimeIn = att != null && !att.overtimeIn.isNullOrBlank()
    val isOvertimeOut = att != null && !att.overtimeOut.isNullOrBlank()

    var isLate = (att?.lateMinutes ?: 0) > 0
    if (!isLate && att?.clockIn != null && todayItem?.shift?.startTime != null) {
        if (att.clockIn.take(5) > todayItem.shift.startTime.take(5)) {
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
        if (hasApprovedOvertime) {
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
        if (todayItem?.shift != null) {
            showBtnClockIn = true
        }
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
                    } else if (todayItem == null || (todayItem.shift == null && todayItem.isHoliday != true)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Belum ada jadwal hari ini.",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (todayItem.shift == null && todayItem.isHoliday == true) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Libur Nasional",
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
                    Button(
                        onClick = onClockIn,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF007A87))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLOCK IN HARI INI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (showBtnClockOut) {
                    Button(
                        onClick = onClockOut,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLOCK OUT HARI INI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (showBtnOvertimeClockIn) {
                    Button(
                        onClick = onOvertimeClockIn,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White)
                    ) {
                        Text("CLOCK IN LEMBUR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (showBtnOvertimeClockOut) {
                    Button(
                        onClick = onOvertimeClockOut,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B21B6), contentColor = Color.White)
                    ) {
                        Text("CLOCK OUT LEMBUR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (isCheckedOut) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (hasApprovedOvertime && isOvertimeOut) "✓ Presensi & Lembur Hari Ini Selesai" else "✓ Presensi Hari Ini Selesai",
                            modifier = Modifier.padding(12.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
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
    val clockInStr = att?.clockIn?.take(5) ?: "-"

    val outLabel = "Out: "
    val clockOutStr = att?.clockOut?.take(5) ?: "-"

    val statusInfoText = if (isHoliday) "Libur" else (att?.status?.replaceFirstChar { it.uppercase() } ?: "Working Day")

    var isLate = (att?.lateMinutes ?: 0) > 0
    val shiftStartStr = shift?.startTime?.take(5)
    if (!isLate && clockInStr != "-" && shiftStartStr != null && clockInStr > shiftStartStr) {
        isLate = true
    }

    val finalStatusText = if (isLate) "$statusInfoText (Telat)" else statusInfoText
    val lateRedColor = Color(0xFFFF5252)

    val otIn = att?.overtimeIn?.take(5)
    val otOut = att?.overtimeOut?.take(5)
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
                            .background(if (isHoliday) Color(0xFFF1F5F9) else Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isHoliday) Color(0xFF64748B) else Primary,
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
                    color = if (isLate) Color(0xFFFFE4E6) else if (isHoliday) Color(0xFFF1F5F9) else Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = finalStatusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (isLate) lateRedColor else if (isHoliday) Color(0xFF64748B) else Primary,
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
