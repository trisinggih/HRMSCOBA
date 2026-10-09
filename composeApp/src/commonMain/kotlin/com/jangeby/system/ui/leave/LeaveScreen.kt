package com.jangeby.system.ui.leave

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaveViewModel = koinViewModel()
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var selectedLeaveType by remember { mutableStateOf("Cuti Tahunan") }
    var expandedTypeMenu by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val leaveTypes = listOf("Cuti Tahunan", "Cuti Sakit", "Cuti Melahirkan", "Cuti Penting")
    val leaveTypeIds = mapOf("Cuti Tahunan" to 1L, "Cuti Sakit" to 2L, "Cuti Melahirkan" to 3L, "Cuti Penting" to 4L)

    // Mock balance info
    val balanceItems = listOf(
        Triple("Cuti Tahunan", 12, 5),
        Triple("Cuti Sakit", 10, 2),
    )

    // Mock history
    val historyItems = remember {
        listOf(
            Triple("01 Sep – 03 Sep 2026", "Cuti Tahunan", "approved"),
            Triple("15 Aug – 15 Aug 2026", "Cuti Sakit", "approved"),
            Triple("10 Okt 2026", "Cuti Tahunan", "pending"),
        )
    }

    LaunchedEffect(state) {
        when (state) {
            is LeaveState.Success -> {
                snackbarHostState.showSnackbar("Pengajuan cuti berhasil dikirim!")
                viewModel.resetState()
                startDate = ""; endDate = ""; reason = ""
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            HrmsTopBar(title = "Cuti & Izin", onNavigateBack = onNavigateBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Leave Balance
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Saldo Cuti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    balanceItems.forEach { (type, quota, used) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(type, fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${quota - used} Hari",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Text("Sisa dari $quota hari", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Form Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ajukan Cuti / Izin",
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Leave Type Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedTypeMenu,
                            onExpandedChange = { expandedTypeMenu = it }
                        ) {
                            OutlinedTextField(
                                value = selectedLeaveType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Jenis Cuti") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeMenu) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTypeMenu,
                                onDismissRequest = { expandedTypeMenu = false }
                            ) {
                                leaveTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = { selectedLeaveType = type; expandedTypeMenu = false }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Mulai") },
                                placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("Selesai") },
                                placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Alasan / Keterangan") },
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (state is LeaveState.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ErrorMessage((state as LeaveState.Error).message)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val typeId = leaveTypeIds[selectedLeaveType] ?: 1L
                                viewModel.submitLeave(typeId, startDate, endDate, reason)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = state !is LeaveState.Loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (state is LeaveState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AJUKAN SEKARANG", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // History
            item {
                Text(
                    text = "Riwayat Pengajuan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(historyItems) { (date, type, status) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BgLeave),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DateRange, null, tint = IconLeave, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(type, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(date, fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        StatusChip(status)
                    }
                }
            }

            if (historyItems.isEmpty()) {
                item { EmptyState("Belum ada riwayat pengajuan") }
            }
        }
    }
}
