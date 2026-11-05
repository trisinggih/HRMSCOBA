package com.jangeby.system.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.jangeby.system.data.remote.MilestoneItemData
import com.jangeby.system.ui.theme.Primary

@Composable
fun MilestonePopupDialog(
    milestones: List<MilestoneItemData>,
    currentEmployeeId: Long = 0L,
    myMilestoneToday: MilestoneItemData? = null,
    companyName: String = "Organisasi",
    onDismiss: () -> Unit,
    onNavigateToWishes: () -> Unit
) {
    val myMilestone = myMilestoneToday
        ?: milestones.firstOrNull { it.isToday && it.employeeId == currentEmployeeId && currentEmployeeId != 0L }
    val isMyMilestone = myMilestone != null

    val colleaguesToWish = milestones.filter {
        it.isToday && (currentEmployeeId == 0L || it.employeeId != currentEmployeeId)
    }

    Dialog(
        onDismissRequest = { /* Hanya bisa ditutup lewat klik tombol */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isMyMilestone && myMilestone != null) {
                    // =========================================================================
                    // 1. POP UP KHUSUS KARYAWAN YANG HARI INI BERULANG TAHUN / WORK ANNIVERSARY
                    // (Ucapan Resmi dari Manajemen & Keluarga Besar Jangeby Group)
                    // =========================================================================
                    val isBirthday = myMilestone.type == "birthday"
                    val gradientColors = if (isBirthday) {
                        listOf(Color(0xFFBE185D), Color(0xFFE11D48), Color(0xFFD97706))
                    } else {
                        listOf(Color(0xFF0F766E), Color(0xFF0284C7), Color(0xFF4338CA))
                    }

                    // Header Banner (Elegan & Formal)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(gradientColors))
                            .padding(20.dp)
                    ) {
                        // Close button at top right
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = companyName.uppercase(),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isBirthday) "Selamat Ulang Tahun" else "Selamat Work Anniversary",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "Ucapan Spesial dari Manajemen & Keluarga Besar",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isBirthday) Color(0xFFFFE4E6) else Color(0xFFE0F2FE))
                                .border(2.dp, if (isBirthday) Color(0xFFF43F5E) else Color(0xFF0284C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!myMilestone.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = myMilestone.photoUrl,
                                    contentDescription = myMilestone.employeeName,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = myMilestone.employeeName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = if (isBirthday) Color(0xFFBE123C) else Color(0xFF0369A1)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = myMilestone.employeeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )

                        val deptBranch = listOfNotNull(
                            myMilestone.positionName?.takeIf { it != "-" },
                            myMilestone.branchName?.takeIf { it != "-" }
                        ).joinToString(" • ")

                        if (deptBranch.isNotBlank()) {
                            Text(
                                text = deptBranch,
                                fontSize = 11.5.sp,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isBirthday) Color(0xFFFFF1F2) else Color(0xFFF0FDF4))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = myMilestone.badgeTitle ?: if (isBirthday) "Hari Ini Berulang Tahun" else "Work Anniversary Hari Ini",
                                color = if (isBirthday) Color(0xFFBE123C) else Color(0xFF15803D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pesan Ucapan Resmi dari Jangeby Group (Bebas Icon / Emoji)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBirthday) Color(0xFFFFFBEB) else Color(0xFFF0FDF4)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isBirthday) Color(0xFFFDE68A) else Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBirthday) {
                                    "Keluarga Besar $companyName mengucapkan Selamat Ulang Tahun.\n\nSemoga senantiasa diberikan kesehatan, keberkahan, serta kelancaran dan kesuksesan dalam setiap langkah karier Anda bersama kami. Terima kasih atas dedikasi dan kontribusi yang telah diberikan."
                                } else {
                                    "Keluarga Besar $companyName mengucapkan Selamat atas ${myMilestone.yearsCount} Tahun Dedikasi dan Pengabdian Anda.\n\nTerima kasih atas loyalitas, kerja keras, dan dedikasi luar biasa yang telah diberikan. Semoga semakin berprestasi dan terus berkembang bersama kami."
                                },
                                fontSize = 12.sp,
                                color = if (isBirthday) Color(0xFF78350F) else Color(0xFF166534),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        if (colleaguesToWish.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Terdapat ${colleaguesToWish.size} rekan kerja lainnya yang berulang tahun hari ini",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Button(
                            onClick = onNavigateToWishes,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Text(
                                text = "Lihat Ucapan Rekan Kerja",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tutup",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                } else {
                    // =========================================================================
                    // 2. POP UP UNTUK KARYAWAN YANG TIDAK BERULANG TAHUN HARI INI
                    // =========================================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE11D48), Color(0xFFBE185D), Color(0xFF9D174D))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = companyName.uppercase(),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Momen Perayaan Hari Ini",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Daftar rekan kerja yang sedang merayakan momen hari ini.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // List of celebrated colleagues
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(colleaguesToWish) { item ->
                            val isBirthday = item.type == "birthday"
                            val badgeBg = if (isBirthday) Color(0xFFFFE4E6) else Color(0xFFFEF3C7)
                            val badgeText = if (isBirthday) Color(0xFFBE123C) else Color(0xFFB45309)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(0.8.dp, Color(0xFFF3F4F6), RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFAFAFA))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(badgeBg),
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
                                            fontSize = 15.sp,
                                            color = badgeText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.employeeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF111827),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

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

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.badgeTitle ?: if (isBirthday) "Hari Ini Berulang Tahun" else "Work Anniversary Hari Ini",
                                            color = badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF3F4F6))

                    // Actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onNavigateToWishes,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Text(
                                text = "Beri Ucapan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tutup",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}
