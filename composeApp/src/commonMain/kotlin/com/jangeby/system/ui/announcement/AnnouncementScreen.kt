package com.jangeby.system.ui.announcement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jangeby.system.data.remote.AnnouncementData
import com.jangeby.system.data.remote.MilestoneItemData
import com.jangeby.system.data.remote.MilestoneWishData
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementScreen(
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    viewModel: AnnouncementViewModel = koinViewModel()
) {
    LaunchedEffect(initialTab) {
        if (initialTab != 0) {
            viewModel.selectTab(initialTab)
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val isAnnouncementsLoading by viewModel.isLoading.collectAsState()
    val announcementError by viewModel.errorMessage.collectAsState()

    val milestones by viewModel.milestones.collectAsState()
    val isMilestonesLoading by viewModel.isMilestonesLoading.collectAsState()
    val milestonesError by viewModel.milestonesError.collectAsState()
    val todayCount by viewModel.todayCount.collectAsState()
    val activeFilter by viewModel.milestoneFilter.collectAsState()

    val wishesMilestone by viewModel.wishesMilestone.collectAsState()

    var selectedAnnouncement by remember { mutableStateOf<AnnouncementData?>(null) }

    if (selectedAnnouncement != null) {
        AnnouncementDetailScreen(
            announcement = selectedAnnouncement!!,
            viewModel = viewModel,
            onNavigateBack = { selectedAnnouncement = null }
        )
        return
    }

    val unreadCount = announcements.count { !it.isRead }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Pengumuman & Momen",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        if (selectedTab == 0) viewModel.loadAnnouncements()
                        else viewModel.loadMilestones()
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
        ) {
            // ================= 2 TABS NAVIGATION =================
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Primary
            ) {
                // Tab 1: Pengumuman Perusahaan
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Pengumuman",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Danger)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "$unreadCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )

                // Tab 2: Momen & Ucapan
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Momen & Ucapan",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            if (todayCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFE11D48))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "$todayCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // ================= TAB CONTENT =================
            when (selectedTab) {
                0 -> {
                    // TAB PENGUMUMAN
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        if (isAnnouncementsLoading && announcements.isEmpty()) {
                            item { LoadingIndicator() }
                        }

                        if (announcementError != null) {
                            item { ErrorMessage(announcementError!!) }
                        }

                        if (unreadCount > 0) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "$unreadCount pengumuman belum dibaca",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Primary
                                        )
                                    }
                                }
                            }
                        }

                        items(announcements) { item ->
                            AnnouncementCard(
                                item = item,
                                viewModel = viewModel,
                                onClick = {
                                    if (!item.isRead) {
                                        viewModel.markAsRead(item.id)
                                    }
                                    selectedAnnouncement = item
                                }
                            )
                        }

                        if (!isAnnouncementsLoading && announcements.isEmpty() && announcementError == null) {
                            item {
                                EmptyState("Belum ada pengumuman")
                            }
                        }
                    }
                }
                1 -> {
                    // TAB MOMEN & UCAPAN
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // Filter row chips
                        item {
                            MilestoneFilterChips(
                                selectedFilter = activeFilter,
                                onSelectFilter = { viewModel.setMilestoneFilter(it) }
                            )
                        }

                        // Banner celebratory header
                        item {
                            CelebrationHeaderBanner(todayCount = todayCount)
                        }

                        if (isMilestonesLoading && milestones.isEmpty()) {
                            item { LoadingIndicator() }
                        }

                        if (milestonesError != null) {
                            item { ErrorMessage(milestonesError!!) }
                        }

                        items(milestones) { item ->
                            MilestoneCelebrationCard(
                                item = item,
                                onOpenWishes = { viewModel.openWishesSheet(item) },
                                onQuickWish = { message ->
                                    viewModel.sendWish(item, message)
                                }
                            )
                        }

                        if (!isMilestonesLoading && milestones.isEmpty() && milestonesError == null) {
                            item {
                                EmptyState("Tidak ada perayaan atau momen di bulan ini")
                            }
                        }
                    }
                }
            }
        }
    }

    // ================= BOTTOM SHEET UCAPAN =================
    if (wishesMilestone != null) {
        MilestoneWishesBottomSheet(
            milestone = wishesMilestone!!,
            viewModel = viewModel,
            onDismiss = { viewModel.closeWishesSheet() }
        )
    }
}

@Composable
fun MilestoneFilterChips(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    val filters = listOf(
        "all" to "Semua Momen",
        "birthday" to "Ulang Tahun",
        "anniversary" to "Work Anniversary"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(filters) { (key, label) ->
            val isSelected = selectedFilter == key
            val bgColor = if (isSelected) Primary else Color.White
            val textColor = if (isSelected) Color.White else Color(0xFF4B5563)
            val borderColor = if (isSelected) Primary else Color(0xFFE5E7EB)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onSelectFilter(key) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CelebrationHeaderBanner(todayCount: Int) {
    val bgGradient = if (todayCount > 0) {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFFE11D48), Color(0xFFBE185D), Color(0xFFFB7185))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF0C768A), Color(0xFF0F766E), Color(0xFF065F46))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (todayCount > 0) "Hari Spesial Hari Ini" else "Momen Kebersamaan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (todayCount > 0)
                        "Ada $todayCount rekan yang sedang merayakan momen hari ini. Yuk kirimkan doa dan ucapan terbaikmu!"
                    else
                        "Daftar perayaan ulang tahun dan capaian bergabung rekan kerja bulan ini.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun MilestoneCelebrationCard(
    item: MilestoneItemData,
    onOpenWishes: () -> Unit,
    onQuickWish: (String) -> Unit
) {
    val isBirthday = item.type == "birthday"
    val isToday = item.isToday

    // Subtle celebratory accents
    val badgeBgColor = when {
        isToday && isBirthday -> Color(0xFFFFE4E6)
        isToday && !isBirthday -> Color(0xFFFEF3C7)
        isBirthday -> Color(0xFFF3E8FF)
        else -> Color(0xFFE0F2FE)
    }

    val badgeTextColor = when {
        isToday && isBirthday -> Color(0xFFBE123C)
        isToday && !isBirthday -> Color(0xFFB45309)
        isBirthday -> Color(0xFF7E22CE)
        else -> Color(0xFF0369A1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) Color(0xFFFFFDF8) else Color.White
        ),
        border = if (isToday) {
            androidx.compose.foundation.BorderStroke(1.5.dp, if (isBirthday) Color(0xFFFDA4AF) else Color(0xFFFDE68A))
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE5E7EB))
        },
        elevation = CardDefaults.cardElevation(if (isToday) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Header Row: Avatar, Names, Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isBirthday) Color(0xFFFFE4E6) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = item.photoUrl,
                            contentDescription = item.employeeName,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = item.employeeName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (isBirthday) Color(0xFFBE123C) else Color(0xFFB45309)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.employeeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Tanggal / Hari
                        Text(
                            text = item.formattedDate ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val deptBranch = listOfNotNull(
                        item.positionName?.takeIf { it != "-" },
                        item.branchName?.takeIf { it != "-" }
                    ).joinToString(" • ")

                    if (deptBranch.isNotBlank()) {
                        Text(
                            text = deptBranch,
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Event Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBgColor)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.badgeTitle ?: if (isBirthday) "Ulang Tahun" else "Work Anniversary",
                            color = badgeTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Recent Wish Highlight (if exists)
            if (item.recentWishes.isNotEmpty()) {
                val latest = item.recentWishes.first()
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF9FAFB))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("“", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${latest.senderNickname ?: latest.senderName}: \"${latest.message}\"",
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Wish Chips
            Text(
                text = "Kirim ucapan cepat:",
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))

            val quickChips = if (isBirthday) {
                listOf(
                    "Selamat Ulang Tahun",
                    "Sehat & sukses selalu",
                    "Semoga berkah usianya"
                )
            } else {
                listOf(
                    "Selamat Work Anniversary",
                    "Semoga semakin berprestasi",
                    "Terima kasih atas dedikasinya"
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickChips) { chipText ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(0.8.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                            .background(Color(0xFFF9FAFB))
                            .clickable { onQuickWish(chipText) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 11.sp,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions: "Lihat Semua Ucapan (X)" & "Kirim Ucapan"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenWishes,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.wishesCount > 0)
                            "Lihat Semua Ucapan (${item.wishesCount})"
                        else
                            "Lihat Ucapan",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onOpenWishes,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.hasWished) Color(0xFFF0FDF4) else Primary,
                        contentColor = if (item.hasWished) Color(0xFF16A34A) else Color.White
                    ),
                    border = if (item.hasWished) {
                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                    } else null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (item.hasWished) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sudah Mengucapkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Beri Ucapan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneWishesBottomSheet(
    milestone: MilestoneItemData,
    viewModel: AnnouncementViewModel,
    onDismiss: () -> Unit
) {
    val wishesList by viewModel.wishesList.collectAsState()
    val isWishesLoading by viewModel.isWishesLoading.collectAsState()
    val isSendingWish by viewModel.isSendingWish.collectAsState()

    var customMessage by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE5E7EB))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (milestone.type == "birthday") Color(0xFFFFE4E6) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!milestone.photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = milestone.photoUrl,
                            contentDescription = milestone.employeeName,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = milestone.employeeName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (milestone.type == "birthday") Color(0xFFBE123C) else Color(0xFFB45309)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = milestone.employeeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "${milestone.badgeTitle} • ${milestone.formattedDate ?: ""}",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF9CA3AF))
                }
            }

            HorizontalDivider(color = Color(0xFFF3F4F6))

            // Wishes List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (isWishesLoading && wishesList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                if (!isWishesLoading && wishesList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Belum ada ucapan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF374151)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Jadilah yang pertama memberikan doa dan selamat!",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                items(wishesList) { wish ->
                    WishItemBubble(
                        wish = wish,
                        onDelete = {
                            viewModel.deleteWish(wish.id, milestone)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Quick greeting chips bar
            val quickWishes = if (milestone.type == "birthday") {
                listOf(
                    "🎉 Selamat Ulang Tahun!",
                    "🎂 Panjang umur & sehat selalu!",
                    "✨ Berkah usianya!"
                )
            } else {
                listOf(
                    "👏 Happy Work Anniversary!",
                    "🌟 Semakin sukses dan jaya!",
                    "💪 Terima kasih dedikasinya!"
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickWishes) { text ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.8.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable {
                                customMessage = text
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(text, fontSize = 11.sp, color = Color(0xFF374151))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE5E7EB))

            // Bottom Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Tulis ucapan selamat...", fontSize = 13.sp) },
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Color(0xFFD1D5DB)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (customMessage.isNotBlank()) {
                                val textToSend = customMessage
                                viewModel.sendWish(milestone, textToSend) {
                                    customMessage = ""
                                }
                            }
                        },
                        enabled = customMessage.isNotBlank() && !isSendingWish,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (customMessage.isNotBlank() && !isSendingWish) Primary else Color(0xFFE5E7EB))
                    ) {
                        if (isSendingWish) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Kirim",
                                tint = if (customMessage.isNotBlank()) Color.White else Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WishItemBubble(
    wish: MilestoneWishData,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Sender Avatar
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E7FF)),
            contentAlignment = Alignment.Center
        ) {
            if (!wish.senderPhotoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = wish.senderPhotoUrl,
                    contentDescription = wish.senderName,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = wish.senderName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Message Bubble
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (wish.isMine) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                .border(
                    0.5.dp,
                    if (wish.isMine) Color(0xFFBFDBFE) else Color(0xFFE5E7EB),
                    RoundedCornerShape(12.dp)
                )
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (wish.isMine) "${wish.senderName} (Saya)" else wish.senderName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (wish.isMine) Primary else Color(0xFF111827)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wish.timeAgo ?: "",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )

                    if (wish.isMine) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Ucapan",
                            tint = Danger.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDelete() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = wish.message,
                fontSize = 12.sp,
                color = Color(0xFF374151),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun AnnouncementCard(
    item: AnnouncementData,
    viewModel: AnnouncementViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) Color(0xFFF0F9FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (!item.isRead) BgTasks else Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (!item.isRead) IconTasks else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.title,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (!item.isRead) Color(0xFF111827) else Color(0xFF374151),
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                    }
                }

                val previewBody = viewModel.parseHtml(item.body)
                if (previewBody.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = previewBody,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.formatDateTime(item.createdAt),
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )

                    if (!item.attachmentUrl.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (item.attachmentType?.lowercase() == "image") "Gambar" else "Lampiran",
                                fontSize = 10.sp,
                                color = Primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailScreen(
    announcement: AnnouncementData,
    viewModel: AnnouncementViewModel,
    onNavigateBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            HrmsTopBar(title = "Detail Pengumuman", onNavigateBack = onNavigateBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryVariant)))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = announcement.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.formatDateTime(announcement.createdAt),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = viewModel.parseHtml(announcement.body).ifBlank { "Tidak ada konten." },
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )

                        // Attachment preview / action
                        if (!announcement.attachmentUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Lampiran:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF374151)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (announcement.attachmentType?.lowercase() == "image") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            uriHandler.openUri(announcement.attachmentUrl)
                                        }
                                ) {
                                    AsyncImage(
                                        model = announcement.attachmentUrl,
                                        contentDescription = "Lampiran Pengumuman",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ketuk gambar untuk melihat ukuran penuh",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            } else {
                                Button(
                                    onClick = {
                                        uriHandler.openUri(announcement.attachmentUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (announcement.attachmentType?.lowercase() == "pdf") "Buka Dokumen PDF" else "Buka Lampiran",
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
