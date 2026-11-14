package com.jangeby.system.ui.kpi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.theme.Background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.data.remote.KpiAssessmentItem
import com.jangeby.system.data.remote.KpiAssessmentScoreItem
import com.jangeby.system.data.remote.KpiTemplateIndicatorItem
import com.jangeby.system.data.remote.SelfAssessmentScorePayload
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpiScreen(
    onNavigateBack: () -> Unit,
    viewModel: KpiViewModel = koinViewModel()
) {
    val assessments by viewModel.assessments.collectAsState()
    val selectedAssessment by viewModel.selectedAssessment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Local state for actual values inputs: Map<TemplateIndicatorId, String>
    val actualInputs = remember { mutableStateMapOf<Long, String>() }
    var selfNotesInput by remember { mutableStateOf("") }

    // Sync local state when selected assessment changes
    LaunchedEffect(selectedAssessment) {
        actualInputs.clear()
        selectedAssessment?.let { assess ->
            selfNotesInput = assess.selfNotes ?: ""
            assess.scores?.forEach { score ->
                score.templateIndicatorId?.let { id ->
                    score.actualValue?.let { v ->
                        actualInputs[id] = v
                    }
                }
            }
        }
    }

    val primaryTeal = Color(0xFF007A87)

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "KPI Saya",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadMyKpi() }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = Color.White)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background,
        bottomBar = {
            val assess = selectedAssessment
            val isEditable = assess != null && assess.status in listOf("pending", "self_assessed")
            if (isEditable) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = {
                                val current = selectedAssessment ?: return@Button
                                val indicators = current.template?.templateIndicators ?: emptyList()
                                val payloadList = mutableListOf<SelfAssessmentScorePayload>()

                                indicators.forEach { ti ->
                                    val strVal = actualInputs[ti.id]?.trim()
                                    val doubleVal = strVal?.toDoubleOrNull()
                                    if (doubleVal != null) {
                                        payloadList.add(
                                            SelfAssessmentScorePayload(
                                                templateIndicatorId = ti.id,
                                                actualValue = doubleVal,
                                                notes = ""
                                            )
                                        )
                                    }
                                }

                                if (payloadList.isEmpty()) {
                                    return@Button
                                }

                                viewModel.submitSelfAssessment(
                                    assessmentId = current.id,
                                    scores = payloadList,
                                    selfNotes = selfNotesInput
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryTeal),
                            enabled = !isSubmitting && actualInputs.isNotEmpty()
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mengirim...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Text(
                                    text = if (assess?.status == "self_assessed") "Perbarui Self-Assessment" else "Kirim Self-Assessment ke Atasan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading && assessments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryTeal)
            }
        } else if (assessments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (errorMessage != null) "Gagal Memuat KPI" else "Belum Ada Data KPI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage ?: "Belum ada lembar penilaian KPI yang ditugaskan kepada Anda saat ini.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { viewModel.loadMyKpi() }) { Text("Muat Ulang") }
                }
            }
        } else {
            val currentAssessment = selectedAssessment ?: assessments.first()
            val indicators = currentAssessment.template?.templateIndicators ?: emptyList()
            val isApproved = currentAssessment.status !in listOf("pending", "self_assessed")

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Horizontal Period Selector Chips
                if (assessments.size > 1) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(assessments) { item ->
                                val isSelected = item.id == currentAssessment.id
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) primaryTeal else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) primaryTeal else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable { viewModel.selectAssessment(item) }
                                ) {
                                    Text(
                                        text = item.period?.name ?: "Periode #${item.id}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Score & Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentAssessment.period?.name ?: "Evaluasi KPI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                StatusBadge(status = currentAssessment.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Big Score Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Skor Capaian",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val scoreVal = currentAssessment.finalScore?.toDoubleOrNull()
                                    Text(
                                        text = if (scoreVal != null) "${((scoreVal * 100).toLong() / 100.0)}%" else "—",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getScoreColor(scoreVal)
                                    )
                                }

                                currentAssessment.predicate?.let { pred ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = getPredicateBgColor(pred.grade),
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "${pred.grade ?: ""} · ${pred.label ?: ""}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = getPredicateTextColor(pred.grade),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Metadata info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Atasan: ${currentAssessment.reviewer?.name ?: "—"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Skala: 0 – 120%",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // Notice Banner if already submitted
                if (currentAssessment.status == "self_assessed") {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Self-assessment Anda telah dikirim dan sedang menunggu review dari atasan.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E40AF),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Section Header
                item {
                    Text(
                        text = "Indikator Capaian (${indicators.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                // Indicators List
                items(indicators) { ti ->
                    val tiId = ti.id
                    val inputVal = actualInputs[tiId] ?: ""
                    val targetNum = ti.targetValue?.toDoubleOrNull() ?: 0.0
                    val dir = ti.direction ?: ti.indicator?.direction ?: "higher_better"
                    val unit = ti.unit ?: ti.indicator?.measurementUnit ?: ""
                    val weight = ti.weight?.toDoubleOrNull() ?: 0.0

                    val actualDouble = inputVal.toDoubleOrNull()
                    val achPct = if (actualDouble != null && targetNum > 0) {
                        val maxAch = ti.maxAchievement?.toDoubleOrNull() ?: 120.0
                        if (dir == "lower_better") {
                            if (actualDouble <= 0) maxAch else minOf((targetNum / actualDouble) * 100.0, maxAch)
                        } else {
                            minOf((actualDouble / targetNum) * 100.0, maxAch)
                        }
                    } else null

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            // Title & Weight
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = ti.indicator?.name ?: "Indikator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "Bobot: ${weight.toLong()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTeal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Direction Badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (dir == "lower_better") Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = if (dir == "lower_better") "↓ Rendah Lebih Baik" else "↑ Tinggi Lebih Baik",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (dir == "lower_better") Color(0xFF92400E) else Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Target: ${ti.targetValue ?: ti.target ?: "—"} $unit",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Actual input / display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isApproved) {
                                    OutlinedTextField(
                                        value = inputVal,
                                        onValueChange = { newValue ->
                                            actualInputs[tiId] = newValue
                                        },
                                        label = { Text("Angka Aktual ($unit)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).height(54.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                } else {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Aktual Tercapai:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(
                                            text = "${inputVal.ifBlank { "0" }} $unit",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Live Capaian Badge
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Capaian:", fontSize = 10.sp, color = Color(0xFF64748B))
                                    if (achPct != null) {
                                        Text(
                                            text = "${((achPct * 10).toLong() / 10.0)}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = getScoreColor(achPct)
                                        )
                                    } else {
                                        Text(text = "—", fontSize = 14.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }

                // Self-Notes Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                text = "Catatan & Bukti Capaian Mandiri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tuliskan ringkasan bukti hasil kerja atau kendala yang dihadapi untuk reviewer.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (!isApproved) {
                                OutlinedTextField(
                                    value = selfNotesInput,
                                    onValueChange = { selfNotesInput = it },
                                    placeholder = { Text("Contoh: Target penjualan tercapai 105%, laporan telah diarsip di drive...", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth().height(90.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            } else {
                                Text(
                                    text = selfNotesInput.ifBlank { "Tidak ada catatan." },
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // Reviewer Notes (if available)
                currentAssessment.reviewerNotes?.let { notes ->
                    if (notes.isNotBlank()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Catatan & Masukan dari Evaluator",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = notes,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String?) {
    val (bgColor, textColor, label) = when (status) {
        "approved" -> Triple(Color(0xFFDCFCE7), Color(0xFF166534), "Disetujui")
        "self_assessed" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Menunggu Review")
        "in_review" -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "Sedang Dinilai")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "Perlu Diisi")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun getScoreColor(score: Double?): Color {
    if (score == null) return Color(0xFF94A3B8)
    return when {
        score >= 110.0 -> Color(0xFF16A34A)
        score >= 100.0 -> Color(0xFF0284C7)
        score >= 90.0 -> Color(0xFF0D9488)
        score >= 80.0 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }
}

fun getPredicateBgColor(grade: String?): Color {
    return when (grade?.uppercase()) {
        "A+" -> Color(0xFFDCFCE7)
        "A" -> Color(0xFFE0F2FE)
        "B" -> Color(0xFFCCFBF1)
        "C" -> Color(0xFFFEF3C7)
        else -> Color(0xFFFEE2E2)
    }
}

fun getPredicateTextColor(grade: String?): Color {
    return when (grade?.uppercase()) {
        "A+" -> Color(0xFF166534)
        "A" -> Color(0xFF0369A1)
        "B" -> Color(0xFF0F766E)
        "C" -> Color(0xFF92400E)
        else -> Color(0xFF991B1B)
    }
}
