package com.jangeby.system.ui.checkpoint

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.CheckPointItem
import com.jangeby.system.hardware.rememberLocationLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckPointScreen(
    onNavigateBack: () -> Unit,
    viewModel: CheckPointViewModel = viewModel { CheckPointViewModel() }
) {
    val checkPoints by viewModel.checkPoints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedCheckPoint by remember { mutableStateOf<CheckPointItem?>(null) }

    LaunchedEffect(errorMessage, successMessage) {
        if (successMessage != null) {
            showAddSheet = false
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            com.jangeby.system.ui.component.HrmsTopBar(
                title = "Check Point",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = com.jangeby.system.ui.theme.Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Check Point",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && checkPoints.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (checkPoints.isEmpty()) {
                Text(
                    "Belum ada data check point.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(checkPoints) { cp ->
                        CheckPointCard(
                            item = cp,
                            formattedDate = viewModel.formatDateDMY(cp.createdAt),
                            onClick = { selectedCheckPoint = cp }
                        )
                    }
                }
            }

            if (errorMessage != null && !showAddSheet) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(errorMessage ?: "")
                }
            }

            if (successMessage != null && !showAddSheet) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(successMessage ?: "")
                }
            }
        }
    }

    if (showAddSheet) {
        AddCheckPointBottomSheet(
            onDismiss = { showAddSheet = false },
            onSubmit = { client, purpose, lat, lng, notes, photos ->
                viewModel.submitCheckPoint(client, purpose, lat, lng, notes, photos)
            },
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            clearError = { viewModel.clearMessages() }
        )
    }

    if (selectedCheckPoint != null) {
        CheckPointDetailBottomSheet(
            item = selectedCheckPoint!!,
            formattedDate = viewModel.formatDateDMY(selectedCheckPoint!!.createdAt),
            onDismiss = { selectedCheckPoint = null }
        )
    }
}

@Composable
fun CheckPointCard(item: CheckPointItem, formattedDate: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.clientName ?: "-",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tujuan: ${item.purpose ?: "-"}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.employee?.name != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Oleh: ${item.employee.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val photoUrl = item.photos?.firstOrNull() ?: item.photoPath
                if (!photoUrl.isNullOrEmpty()) {
                    val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "${ApiClient.BASE_URL.replace("api/v1/", "storage/")}$photoUrl"
                    Box(modifier = Modifier.size(64.dp)) {
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        val totalPhotos = item.photos?.size ?: if (item.photoPath != null) 1 else 0
                        if (totalPhotos > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("+${totalPhotos - 1}", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.LightGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.address ?: "${item.latitude}, ${item.longitude}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCheckPointBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (client: String, purpose: String, lat: Double, lng: Double, notes: String, photos: List<PlatformFile>) -> Unit,
    isSubmitting: Boolean,
    errorMessage: String?,
    clearError: () -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationFetched by remember { mutableStateOf(false) }

    val locationLauncher = rememberLocationLauncher { result ->
        if (result.isSuccess) {
            latitude = result.latitude
            longitude = result.longitude
            locationFetched = true
        } else {
            // Handle error or fallback
        }
    }

    LaunchedEffect(Unit) {
        locationLauncher.fetchLocation()
    }

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single
    ) { file ->
        if (file != null) {
            photos = photos + file
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Tambah Check Point", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text("Nama Klien / Tempat") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text("Tujuan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Location Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (locationFetched) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (locationFetched) "${latitude}, ${longitude}" else "Mencari lokasi...",
                    fontSize = 14.sp,
                    color = if (locationFetched) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Foto Bukti Kunjungan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.LightGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(photo.name, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(4.dp))
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { filePicker.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Foto", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (latitude != null && longitude != null) {
                        onSubmit(clientName, purpose, latitude!!, longitude!!, notes, photos)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && locationFetched
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Submit")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckPointDetailBottomSheet(
    item: CheckPointItem,
    formattedDate: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Detail Check Point",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = item.clientName ?: "-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "Tujuan: ${item.purpose ?: "-"}", fontSize = 14.sp, color = Color.Gray)
            Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))

            val photos = item.photos ?: if (item.photoPath != null) listOf(item.photoPath) else emptyList()
            if (photos.isNotEmpty()) {
                Text("Foto Bukti", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos) { photoUrl ->
                        val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "${ApiClient.BASE_URL.replace("api/v1/", "storage/")}$photoUrl"
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = "Foto",
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("Lokasi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            
            val lat = item.latitude?.toDoubleOrNull()
            val lng = item.longitude?.toDoubleOrNull()
            if (lat != null && lng != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                ) {
                    com.jangeby.system.ui.component.MapViewComponent(
                        modifier = Modifier.fillMaxSize(),
                        latitude = lat,
                        longitude = lng
                    )
                }
            } else {
                Text("Lokasi tidak tersedia", color = Color.Gray)
            }
            
            if (!item.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Catatan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(item.notes, fontSize = 14.sp)
            }
        }
    }
}
