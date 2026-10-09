package com.jangeby.system.ui.notification

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*

data class NotifItem(
    val title: String,
    val body: String,
    val time: String,
    val type: String, // "leave", "overtime", "payslip", "info"
    val isRead: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit
) {
    val notifications = remember {
        listOf(
            NotifItem("Cuti Disetujui", "Pengajuan cuti tahunan Anda 01–03 Sep 2026 telah disetujui oleh HRD.", "2 jam lalu", "leave", false),
            NotifItem("Slip Gaji Tersedia", "Slip gaji bulan September 2026 sudah dapat dilihat.", "1 hari lalu", "payslip", false),
            NotifItem("Pengingat Presensi", "Anda belum melakukan Clock Out hari ini.", "3 jam lalu", "info", true),
            NotifItem("Lembur Ditolak", "Pengajuan lembur tgl 15 Sep ditolak. Silakan hubungi atasan Anda.", "3 hari lalu", "overtime", true),
            NotifItem("Info: Hari Libur", "Peringatan Hari Batik Nasional – 2 Oktober 2026 adalah hari libur.", "5 hari lalu", "info", true),
        )
    }

    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Notifikasi",
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Unread badge summary
            if (unreadCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = Primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "$unreadCount notifikasi belum dibaca",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Primary
                            )
                        }
                    }
                }
            }

            items(notifications) { notif ->
                NotifCard(notif)
            }

            if (notifications.isEmpty()) {
                item { EmptyState("Belum ada notifikasi") }
            }
        }
    }
}

@Composable
fun NotifCard(notif: NotifItem) {
    val (icon, bgColor, iconColor) = when (notif.type) {
        "leave" -> Triple(Icons.Default.ExitToApp, BgLeave, IconLeave)
        "payslip" -> Triple(Icons.Default.List, BgPayslip, IconPayslip)
        "overtime" -> Triple(Icons.Default.AddCircle, BgInfo, IconInfo)
        else -> Triple(Icons.Default.Notifications, BgTasks, IconTasks)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notif.isRead) Color(0xFFF0F9FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notif.title,
                        fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notif.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = notif.body,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notif.time,
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}
