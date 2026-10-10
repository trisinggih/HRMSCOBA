package com.jangeby.system.ui.task

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.jangeby.system.data.remote.TaskItem
import com.jangeby.system.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: TaskViewModel = koinViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val currentDate by viewModel.selectedDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state for item completion confirmation
    var taskToComplete by remember { mutableStateOf<TaskItem?>(null) }

    // Dialog state for complete all confirmation
    var showCompleteAllDialog by remember { mutableStateOf(false) }

    // BottomSheet state for Add Task
    var showAddTaskSheet by remember { mutableStateOf(false) }

    // DatePicker dialog state
    var showDatePicker by remember { mutableStateOf(false) }

    // Handle messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Tugas",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FA))
                .padding(padding)
        ) {
            // Header Section with rounded bottom corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Top Bar (Back button & Title)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Daily Report",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { viewModel.loadTasks() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat Ulang",
                                tint = Color.White
                            )
                        }
                    }

                    // Date Navigator Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.goToPrevDay() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Hari Sebelumnya",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePicker = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.formatDisplayDate(currentDate),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { viewModel.goToNextDay() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Hari Berikutnya",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Body Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile & Tasks Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Profile Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0F2F1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = Primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = viewModel.departmentName,
                                        color = Color.Black,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "PIC: ${viewModel.picName}",
                                        color = Color(0xFF757575),
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Divider Line
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color = Color(0xFFF0F0F0)
                            )

                            // Loading state
                            if (isLoading && tasks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Primary)
                                }
                            } else if (tasks.isEmpty()) {
                                // Empty state
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada tugas",
                                        color = Color(0xFF888888),
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                // Task Items List
                                tasks.forEachIndexed { index, task ->
                                    TaskItemRow(
                                        number = index + 1,
                                        task = task,
                                        onClick = {
                                            if (task.status?.lowercase() != "done" && task.status?.uppercase() != "SELESAI") {
                                                taskToComplete = task
                                            }
                                        }
                                    )
                                    if (index < tasks.size - 1) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }

                                // Mark Complete All Button if there are unfinished tasks
                                val hasUnfinished = tasks.any {
                                    it.status?.lowercase() != "done" && it.status?.uppercase() != "SELESAI"
                                }
                                if (hasUnfinished) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    OutlinedButton(
                                        onClick = { showCompleteAllDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF555555)
                                        )
                                    ) {
                                        Text(
                                            text = "Mark as Complete",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
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

    // Modal Confirmation Dialog: Selesaikan Tugas
    taskToComplete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToComplete = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Selesaikan Tugas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin tugas '${task.title ?: ""}' sudah selesai?",
                    fontSize = 14.sp,
                    color = Color(0xFF555555)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeTask(task.id)
                        taskToComplete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ya", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { taskToComplete = null },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFCCCCCC))
                ) {
                    Text("Belum", color = Color(0xFF555555))
                }
            }
        )
    }

    // Dialog: Selesaikan Semua Tugas
    if (showCompleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteAllDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Selesaikan Semua Tugas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menyelesaikan semua tugas pada tanggal ${viewModel.formatDisplayDate(currentDate)}?",
                    fontSize = 14.sp,
                    color = Color(0xFF555555)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeAllTasks()
                        showCompleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ya, Selesaikan Semua", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCompleteAllDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFCCCCCC))
                ) {
                    Text("Batal", color = Color(0xFF555555))
                }
            }
        )
    }

    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val picked = instant.toLocalDateTime(TimeZone.UTC).date
                        viewModel.setDate(picked)
                    }
                    showDatePicker = false
                }) {
                    Text("Pilih", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal", color = Color(0xFF757575))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // BottomSheet: Tambah Tugas
    if (showAddTaskSheet) {
        BottomSheetAddTask(
            onDismiss = { showAddTaskSheet = false },
            onSave = { taskTitles ->
                viewModel.saveTasksBatch(taskTitles)
                showAddTaskSheet = false
            }
        )
    }
}

/**
 * Item row layout matching item_task_daily.xml
 */
@Composable
private fun TaskItemRow(
    number: Int,
    task: TaskItem,
    onClick: () -> Unit
) {
    val statusLabel = when (task.status?.lowercase()) {
        "todo" -> "TUNGGU"
        "in_progress" -> "PROSES"
        "done" -> "SELESAI"
        else -> task.status?.uppercase() ?: "PROSES"
    }

    val (cardBg, textColor) = when (statusLabel) {
        "SELESAI" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "TUNGGU" -> Pair(Color(0xFFFFF3E0), Color(0xFFF57C00))
        else -> Pair(Color(0xFFE3F2FD), Color(0xFF1976D2)) // PROSES
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Number
        Text(
            text = "$number.",
            color = Color(0xFF555555),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(24.dp)
        )

        // Title
        Text(
            text = task.title ?: "Tugas",
            color = Color(0xFF333333),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        // Status Card
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusLabel,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * BottomSheet layout matching layout_bottom_sheet_add_task.xml & BottomSheetAddTask.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetAddTask(
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var taskInputs by remember { mutableStateOf(listOf("")) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE0E0E0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Tambah Tugas Harian",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Masukkan daftar tugas yang akan Anda kerjakan hari ini.",
                color = Color(0xFF757575),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable task rows
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(taskInputs) { index, value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { newValue ->
                                val updated = taskInputs.toMutableList()
                                updated[index] = newValue
                                taskInputs = updated
                                validationError = null
                            },
                            label = { Text("Deskripsi Tugas") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                focusedLabelColor = Color.Black
                            )
                        )

                        IconButton(
                            onClick = {
                                val updated = taskInputs.toMutableList()
                                updated.removeAt(index)
                                taskInputs = if (updated.isEmpty()) listOf("") else updated
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Baris",
                                tint = Color(0xFFF44336)
                            )
                        }
                    }
                }
            }

            validationError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        taskInputs = taskInputs + ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Tambah Baris", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        val validTasks = taskInputs.map { it.trim() }.filter { it.isNotEmpty() }
                        if (validTasks.isEmpty()) {
                            validationError = "Harap isi minimal 1 tugas"
                        } else {
                            onSave(validTasks)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text(text = "Simpan", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}
