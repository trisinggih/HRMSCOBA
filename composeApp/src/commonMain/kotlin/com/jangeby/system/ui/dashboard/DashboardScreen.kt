package com.jangeby.system.ui.dashboard

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.compose.AsyncImage
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.*
import com.jangeby.system.ui.component.PayslipIcon
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.jangeby.system.utils.DateTimeUtils
import org.koin.compose.koinInject

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val onClick: () -> Unit
)

@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToLeave: () -> Unit,
    onNavigateToOvertime: () -> Unit,
    onNavigateToMapAttendance: (String) -> Unit,
    onNavigateToPayslip: () -> Unit,
    onNavigateToTask: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToAnnouncement: (initialTab: Int) -> Unit,
    onNavigateToApproval: () -> Unit,
    onNavigateToKasbon: () -> Unit,
    onNavigateToReimburse: () -> Unit,
    onNavigateToPermit: () -> Unit = {},
    onNavigateToPengajuan: () -> Unit,
    onNavigateToAsset: () -> Unit,
    onNavigateToCheckPoint: () -> Unit,
    onNavigateToPersonal: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val appSettings: AppSettings = koinInject()
    var userName by remember { mutableStateOf(appSettings.userName ?: "Karyawan") }
    var companyName by remember { mutableStateOf(appSettings.companyName ?: "") }
    var userPosition by remember { mutableStateOf(appSettings.userPosition ?: "") }
    var userPhotoUrl by remember { mutableStateOf(appSettings.userPhotoUrl) }

    LifecycleResumeEffect(Unit) {
        userName = appSettings.userName ?: "Karyawan"
        companyName = appSettings.companyName ?: ""
        userPosition = appSettings.userPosition ?: ""
        userPhotoUrl = appSettings.userPhotoUrl
        com.jangeby.system.notification.FcmManager.syncToken()
        onPauseOrDispose { }
    }

    var badges by remember { mutableStateOf<BadgeResponse?>(null) }
    var hasClockedOut by remember { mutableStateOf(false) }
    var isCurrentlyCheckedIn by remember { mutableStateOf(false) }
    var overtimeToday by remember { mutableStateOf<OvertimeRequestItem?>(null) }
    var recentNotifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var showMilestonePopup by remember { mutableStateOf(false) }
    var todayMilestonesForPopup by remember { mutableStateOf<List<MilestoneItemData>>(emptyList()) }
    var myMilestoneTodayForPopup by remember { mutableStateOf<MilestoneItemData?>(null) }

    LaunchedEffect(Unit) {
        com.jangeby.system.notification.FcmManager.syncToken()
        try {
            val client = ApiClient.create()

            // Fetch employee profile to sync position & legal company
            try {
                val profRes = client.get(ApiClient.BASE_URL + "employee/profile")
                if (profRes.status.value in 200..299) {
                    val profData: ApiResponse<EmployeeProfile> = profRes.body()
                    profData.data?.let { prof ->
                        appSettings.employeeId = prof.id
                        prof.name?.takeIf { it.isNotBlank() }?.let { 
                            userName = it
                            appSettings.userName = it 
                        }
                        prof.company?.takeIf { it.isNotBlank() }?.let { 
                            companyName = it
                            appSettings.companyName = it 
                        }
                        prof.position?.takeIf { it.isNotBlank() }?.let { 
                            userPosition = it
                            appSettings.userPosition = it 
                        }
                        prof.photoUrl?.let { 
                            userPhotoUrl = it
                            appSettings.userPhotoUrl = it 
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }

            val response = client.get(ApiClient.BASE_URL + "employee/dashboard/badges")
            if (response.status.value in 200..299) {
                val res: ApiResponse<BadgeResponse> = response.body()
                badges = res.data
            }

            // Fetch 3 recent notifications for home screen
            try {
                val notifRes = client.get(ApiClient.BASE_URL + "my/notifications?limit=3")
                if (notifRes.status.value in 200..299) {
                    val notifData: ApiResponse<PaginatedResponse<NotificationItem>> = notifRes.body()
                    recentNotifications = notifData.data?.data ?: emptyList()
                }
            } catch (e: Exception) {
                // ignore
            }

            // Check today's attendance state to resume live location tracking if checked in
            try {
                val attRes = client.get(ApiClient.BASE_URL + "attendance/history")
                if (attRes.status.value in 200..299) {
                    val attData: ApiResponse<PaginatedResponse<AttendanceItem>> = attRes.body()
                    val historyList = attData.data?.data ?: emptyList()
                    val todayStr = DateTimeUtils.todayDateString()
                    
                    val todayAtt = historyList.firstOrNull { it.date?.take(10) == todayStr }
                    val unclosedPrevious = historyList.firstOrNull { 
                        it.clockOutAt.isNullOrEmpty() && !it.clockInAt.isNullOrEmpty() 
                    }
                    val currentAtt = if (todayAtt?.clockInAt != null) todayAtt else (unclosedPrevious ?: todayAtt)
                    
                    val isCurrentSession = currentAtt != null && !currentAtt.clockInAt.isNullOrEmpty() && currentAtt.clockOutAt.isNullOrEmpty()
                    
                    if (isCurrentSession) {
                        isCurrentlyCheckedIn = true
                        com.jangeby.system.hardware.LocationTracker.start()
                    } else {
                        isCurrentlyCheckedIn = false
                        com.jangeby.system.hardware.LocationTracker.stop()
                    }

                    // Logika hasClockedOut: sudah clock in dan clock out pada hari ini
                    hasClockedOut = (todayAtt != null && !todayAtt.clockInAt.isNullOrEmpty() && !todayAtt.clockOutAt.isNullOrEmpty())
                }
            } catch (e: Exception) {
                // ignore
            }

            // Check lembur hari ini
            try {
                val otRes = client.get(ApiClient.BASE_URL + "overtime/today")
                if (otRes.status.value in 200..299) {
                    val otData: ApiResponse<OvertimeRequestItem?> = otRes.body()
                    overtimeToday = otData.data
                }
            } catch (e: Exception) {
                // ignore
            }

            // Check milestone celebrations today (>= 07:00, show once per day)
            val now = DateTimeUtils.nowInJakarta()
            val todayStr = DateTimeUtils.todayDateString()
            val isAfter7Am = now.hour >= 7
            val alreadyShownToday = (appSettings.lastSeenMilestonePopupDate == todayStr)

            if (isAfter7Am && !alreadyShownToday) {
                try {
                    val mileRes = client.get(ApiClient.BASE_URL + "employee/milestones")
                    if (mileRes.status.value in 200..299) {
                        val mileData: ApiResponse<MilestoneResponseData> = mileRes.body()
                        val allList = mileData.data?.milestones ?: emptyList()
                        val todayList = allList.filter { it.isToday }
                        val currentEmpId = appSettings.employeeId
                        val myMilestone = mileData.data?.myMilestoneToday 
                            ?: todayList.firstOrNull { it.employeeId == currentEmpId && currentEmpId != 0L }

                        if (myMilestone != null || todayList.isNotEmpty()) {
                            myMilestoneTodayForPopup = myMilestone
                            todayMilestonesForPopup = todayList
                            showMilestonePopup = true
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        } catch (e: Exception) {
            // ignore error
        }
    }

    val primaryTeal = Color(0xFF007A87)
    val primaryVariant = Color(0xFF005662)

    val menuItems = listOf(
        MenuItem("Presensi", Icons.Default.DateRange, 0, onNavigateToAttendance),
        MenuItem("Pengajuan", Icons.Default.Send, badges?.pengajuanCount ?: 0, onNavigateToPengajuan),
        MenuItem("Approval", Icons.Default.CheckCircle, badges?.approvalCount ?: 0, onNavigateToApproval),
        MenuItem("Tugas", Icons.Default.List, badges?.taskCount ?: 0, onNavigateToTask),
        MenuItem("Slip Gaji", PayslipIcon, 0, onNavigateToPayslip),
        MenuItem("Aset", Icons.Default.ShoppingCart, 0, onNavigateToAsset),
        MenuItem("Kasbon", Icons.Default.Star, badges?.kasbonCount ?: 0, onNavigateToKasbon),
        // MenuItem("Klaim", Icons.Default.Person, badges?.reimburseCount ?: 0, onNavigateToReimburse), // Sementara di-hide
        MenuItem("Lembur", Icons.Default.AddCircle, badges?.overtimeCount ?: 0, onNavigateToOvertime),
        MenuItem("Check Point", Icons.Default.LocationOn, 0, onNavigateToCheckPoint),
        MenuItem("Pengumuman", Icons.Default.Notifications, badges?.infoCount ?: 0, { onNavigateToAnnouncement(0) }),
        MenuItem("Personal", Icons.Default.AccountBox, 0, onNavigateToPersonal)
    )

    Scaffold(
        // FIXED STAGNANT TOP BAR HEADER TOOLBAR
        topBar = {
            Surface(
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = primaryTeal
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(primaryTeal, primaryVariant)))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onNavigateToProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(32.dp)
                            )
                            val sanitizedUrl = com.jangeby.system.data.remote.ApiClient.sanitizeImageUrl(userPhotoUrl)
                            if (!sanitizedUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = sanitizedUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // Name & Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hay, $userName",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val subtitleText = when {
                                userPosition.isNotBlank() && companyName.isNotBlank() -> "$userPosition · $companyName"
                                userPosition.isNotBlank() -> userPosition
                                companyName.isNotBlank() -> companyName
                                else -> "Karyawan"
                            }
                            Text(
                                text = subtitleText,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Red Logout Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444))
                                .clickable {
                                    appSettings.clear()
                                    onLogout()
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Keluar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Keluar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        // FLOATING / FIXED BOTTOM NAVIGATION BAR (ADAPTIF UNTUK SEMUA TIPE HP)
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = primaryTeal,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.height(68.dp)
                    ) {
                        NavigationBarItem(
                            selected = true,
                            onClick = { },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(22.dp)) },
                            label = {
                                Text(
                                    "Home",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryTeal,
                                selectedTextColor = primaryTeal,
                                indicatorColor = Color(0xFFCCFBF1)
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { onNavigateToAttendance() },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Presensi", modifier = Modifier.size(22.dp)) },
                            label = {
                                Text(
                                    "Presensi",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { onNavigateToTask() },
                            icon = {
                                val tCount = badges?.taskCount ?: 0
                                BadgedBox(
                                    badge = {
                                        if (tCount > 0) {
                                            Badge(
                                                containerColor = Color(0xFFEF4444),
                                                contentColor = Color.White
                                            ) {
                                                Text(if (tCount > 99) "99+" else tCount.toString(), fontSize = 9.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.List, contentDescription = "Tugas", modifier = Modifier.size(22.dp))
                                }
                            },
                            label = {
                                Text(
                                    "Tugas",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { onNavigateToPengajuan() },
                            icon = {
                                val pCount = badges?.pengajuanCount ?: 0
                                BadgedBox(
                                    badge = {
                                        if (pCount > 0) {
                                            Badge(
                                                containerColor = Color(0xFFEF4444),
                                                contentColor = Color.White
                                            ) {
                                                Text(if (pCount > 99) "99+" else pCount.toString(), fontSize = 9.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Pengajuan", modifier = Modifier.size(22.dp))
                                }
                            },
                            label = {
                                Text(
                                    "Pengajuan",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { onNavigateToProfile() },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(22.dp)) },
                            label = {
                                Text(
                                    "Profile",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // List Menu Label
            item {
                Text(
                    text = "List Menu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                )
            }

            // 4-column Menu Grid
            val rows = menuItems.chunked(4)
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { item ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.0f)
                                .clickable { item.onClick() },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(26.dp),
                                        tint = primaryTeal
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }

                                if (item.badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Absensi Kehadiran Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Absensi Kehadiran",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                )
                if (overtimeToday != null && !isCurrentlyCheckedIn) {
                    // Tampilkan Tombol Lembur (jika ada pengajuan lembur yang di-approve atau in-progress dan tidak sedang shift reguler)
                    val isOtInProgress = overtimeToday?.status == "in_progress"
                    if (isOtInProgress) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.5.dp, Color.LightGray),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.LightGray,
                                    disabledContentColor = Color.LightGray
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Check In",
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { onNavigateToMapAttendance("overtime_clock_out") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Check Out",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToMapAttendance("overtime_clock_in") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7C3AED)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Check In",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.5.dp, Color.LightGray),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.LightGray,
                                    disabledContentColor = Color.LightGray
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Check Out",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (hasClockedOut) {
                    // Posisi hari ini sudah Clock In & Clock Out dan belum ada lembur di-approve:
                    // Ganti tombol Check In dengan tombol "Pengajuan Lembur"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onNavigateToOvertime,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lembur",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToPengajuan,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, primaryTeal),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryTeal)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = primaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Leave Request",
                                    color = primaryTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    // Belum absen hari ini atau sedang shift (Check Out) atau sudah berganti hari (kembali ke Check In)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onNavigateToAttendance,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isCurrentlyCheckedIn) Color(0xFFEF4444) else primaryTeal),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCurrentlyCheckedIn) "Check Out" else "Check In",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToPengajuan,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, primaryTeal),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryTeal)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = primaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Leave Request",
                                    color = primaryTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Notifikasi Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifikasi Terbaru",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Lihat Semua",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTeal,
                        modifier = Modifier.clickable { onNavigateToNotification() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (recentNotifications.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onNavigateToNotification() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = primaryTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Belum Ada Aktivitas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tidak ada notifikasi baru saat ini.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            } else {
                items(recentNotifications) { notif ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onNavigateToNotification() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (notif.isRead == true) Color(0xFFF1F5F9) else Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (notif.isRead == true) Color(0xFF64748B) else primaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title ?: "Aktivitas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                if (!notif.body.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = notif.body,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 2
                                    )
                                }
                                if (!notif.createdAt.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatNotifTime(notif.createdAt),
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMilestonePopup && (myMilestoneTodayForPopup != null || todayMilestonesForPopup.isNotEmpty())) {
        com.jangeby.system.ui.component.MilestonePopupDialog(
            milestones = todayMilestonesForPopup,
            currentEmployeeId = appSettings.employeeId,
            myMilestoneToday = myMilestoneTodayForPopup,
            companyName = appSettings.companyName ?: "Organisasi",
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

private fun formatNotifTime(dateStr: String?): String {
    return com.jangeby.system.utils.DateTimeUtils.formatNotifTime(dateStr)
}


