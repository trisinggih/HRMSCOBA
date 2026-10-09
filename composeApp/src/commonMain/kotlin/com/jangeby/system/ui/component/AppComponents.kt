package com.jangeby.system.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jangeby.system.ui.theme.Primary
import com.jangeby.system.ui.theme.PrimaryVariant

/**
 * Standard TopAppBar with gradient background matching the original app design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrmsTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

/**
 * Status badge chip for leave, overtime, attendance status etc.
 */
@Composable
fun StatusChip(status: String) {
    val (bg, textColor, label) = when (status.lowercase()) {
        "approved", "hadir", "present" -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "Disetujui")
        "pending" -> Triple(Color(0xFFFEF9C3), Color(0xFFB45309), "Menunggu")
        "rejected" -> Triple(Color(0xFFFFE4E6), Color(0xFFDC2626), "Ditolak")
        "late" -> Triple(Color(0xFFFFF7ED), Color(0xFFEA580C), "Terlambat")
        "absent" -> Triple(Color(0xFFFFE4E6), Color(0xFFDC2626), "Mangkir")
        "leave", "cuti" -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), "Cuti")
        "sick", "sakit" -> Triple(Color(0xFFF3E8FF), Color(0xFF7C3AED), "Sakit")
        "permit", "izin" -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "Izin")
        "in_progress" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Berjalan")
        "completed", "done" -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "Selesai")
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF6B7280), status.replaceFirstChar { it.uppercase() })
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Section header with divider.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color(0xFFE5E7EB))
    }
}

/**
 * Info row for displaying label-value pair.
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF111827),
            modifier = Modifier.weight(1.5f)
        )
    }
}

/**
 * Empty state composable.
 */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9CA3AF)
        )
    }
}

/**
 * Full-width loading indicator.
 */
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Primary)
    }
}

/**
 * Error card with message.
 */
@Composable
fun ErrorMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = Color(0xFFDC2626),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
