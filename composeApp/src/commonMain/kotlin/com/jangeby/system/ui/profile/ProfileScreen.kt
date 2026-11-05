package com.jangeby.system.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.jangeby.system.hardware.SignatureExporter
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uriHandler = LocalUriHandler.current
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form fields initialized with profile data
    var etName by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var etEmail by remember(profile) { mutableStateOf(profile?.email ?: "") }
    var etPhone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var etBirthDate by remember(profile) { mutableStateOf(profile?.birthDate ?: "") }
    var etAddress by remember(profile) { mutableStateOf(profile?.address ?: "") }
    var selectedPhoto by remember { mutableStateOf<PlatformFile?>(null) }
    var selectedPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Dialog & BottomSheet states
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmSheet by remember { mutableStateOf(false) }
    var showSignatureSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val currentSignatureUrl by viewModel.currentSignatureUrl.collectAsState()
    val isSignatureSaving by viewModel.isSignatureSaving.collectAsState()

    // Change password fields
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Pilih Foto Profil"
    ) { file ->
        file?.let {
            selectedPhoto = it
            scope.launch {
                selectedPhotoBytes = it.readBytes()
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("JANGEBY SYSTEM") },
            text = {
                Text("Aplikasi manajemen SDM dan operasional untuk mengelola presensi, pengajuan, tugas, dan informasi penggajian sesuai akses akun.")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Tutup") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAboutDialog = false
                    uriHandler.openUri("https://hrms.jangeby.com/support/")
                }) { Text("Bantuan") }
            }
        )
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            selectedPhoto = null
            selectedPhotoBytes = null
            viewModel.clearMessages()
        }
    }

    // Modal Change Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showChangePasswordDialog = false },
            title = { Text("Ganti Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Password Lama") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Password Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Password Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePassword(oldPassword, newPassword, confirmPassword) { success, msg ->
                            if (success) {
                                showChangePasswordDialog = false
                                oldPassword = ""; newPassword = ""; confirmPassword = ""
                                scope.launch { snackbarHostState.showSnackbar(msg ?: "Password berhasil diubah") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(msg ?: "Gagal mengubah password") }
                            }
                        }
                    },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Batal") }
            }
        )
    }

    // Modal BottomSheet Logout Confirmation
    if (showLogoutConfirmSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLogoutConfirmSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Konfirmasi Keluar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Apakah Anda yakin ingin keluar dari aplikasi HRMS?",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showLogoutConfirmSheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showLogoutConfirmSheet = false
                            viewModel.logout { onLogout() }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ya, Keluar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal BottomSheet Tanda Tangan Digital
    if (showSignatureSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val strokes = remember { mutableStateListOf<List<Offset>>() }
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }

        ModalBottomSheet(
            onDismissRequest = { showSignatureSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 36.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Tanda Tangan Digital",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Goreskan tanda tangan Anda pada kotak di bawah ini",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    if (strokes.isNotEmpty()) {
                        IconButton(
                            onClick = { strokes.clear() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Hapus / Ulangi",
                                tint = Danger
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kotak Free-Form Canvas di Tengah Layar
                SignatureCanvasComponent(
                    strokes = strokes,
                    onStrokeAdded = { strokes.add(it) },
                    onCanvasSizeChanged = { canvasSize = it }
                )

                // Preview jika tanda tangan sudah ada di database
                if (currentSignatureUrl != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tanda tangan tersimpan aktif",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(70.dp)
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = currentSignatureUrl,
                                contentDescription = "Tanda Tangan Tersimpan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tombol Aksi Batal & Simpan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSignatureSheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (strokes.isEmpty()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Silakan goreskan tanda tangan terlebih dahulu")
                                }
                                return@Button
                            }
                            val widthPx = if (canvasSize.width > 0) canvasSize.width else 600
                            val heightPx = if (canvasSize.height > 0) canvasSize.height else 360
                            val base64 = SignatureExporter.exportToPngBase64(
                                strokes = strokes,
                                width = widthPx,
                                height = heightPx
                            )
                            if (base64 == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Gagal memproses gambar tanda tangan")
                                }
                                return@Button
                            }

                            val strokesJson = strokes.map { stroke ->
                                stroke.map { pt -> """{"x":${pt.x},"y":${pt.y}}""" }
                            }.toString()

                            viewModel.saveSignature(base64, strokesJson) { success, msg ->
                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar(msg ?: "Tanda tangan berhasil disimpan")
                                        showSignatureSheet = false
                                    } else {
                                        snackbarHostState.showSnackbar(msg ?: "Gagal menyimpan tanda tangan")
                                    }
                                }
                            }
                        },
                        enabled = strokes.isNotEmpty() && !isSignatureSaving,
                        modifier = Modifier.weight(1.5f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSignatureSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Profil Saya",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadProfile() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = Color.White)
                    }
                }
            )
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
            // Profile Header Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryVariant)))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Profile Image with Edit Overlay
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.2f))
                                    .clickable { filePicker.launch() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(54.dp))
                                val displayPhoto: Any? = if (selectedPhotoBytes != null) {
                                    selectedPhotoBytes
                                } else {
                                    com.jangeby.system.data.remote.ApiClient.sanitizeImageUrl(profile?.photoUrl)
                                }
                                if (displayPhoto != null) {
                                    AsyncImage(
                                        model = displayPhoto,
                                        contentDescription = "Foto Profil",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            // Edit photo button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { filePicker.launch() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Ubah Foto", tint = Primary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = profile?.name ?: "User",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile?.position ?: "Karyawan"} · ${profile?.branch ?: "Head Office"}",
                            color = Color.White.copy(0.85f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = profile?.employeeId ?: "EMP-000",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Interactive Account Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Informasi Profil & Akun",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = etName,
                            onValueChange = { etName = it },
                            label = { Text("Nama Lengkap") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = etEmail,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Email (Utama)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = etPhone,
                            onValueChange = { etPhone = it },
                            label = { Text("Nomor HP / Telepon") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        DatePickerField(
                            value = etBirthDate,
                            onValueChange = { etBirthDate = it },
                            label = "Tanggal Lahir"
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = etAddress,
                            onValueChange = { etAddress = it },
                            label = { Text("Alamat") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    name = etName,
                                    phone = etPhone,
                                    address = etAddress,
                                    birthDate = etBirthDate,
                                    photoFile = selectedPhoto,
                                    photoBytes = selectedPhotoBytes
                                )
                            },
                            enabled = !isUpdating,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Simpan Perubahan Profil", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Menu Options Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        ProfileMenuRow(
                            icon = Icons.Default.Lock,
                            label = "Ganti Password",
                            color = Primary,
                            onClick = { showChangePasswordDialog = true }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileMenuRow(
                            icon = Icons.Default.Edit,
                            label = "Tanda Tangan",
                            color = Color(0xFF0284C7),
                            onClick = { showSignatureSheet = true }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileMenuRow(
                            icon = Icons.Default.Notifications,
                            label = "Pengaturan Notifikasi",
                            color = Color(0xFFD97706),
                            onClick = { }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileMenuRow(
                            icon = Icons.Default.Info,
                            label = "Tentang JANGEBY SYSTEM",
                            color = Color(0xFF64748B),
                            onClick = { showAboutDialog = true }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileMenuRow(
                            icon = Icons.Default.Info,
                            label = "Kebijakan Privasi",
                            color = Color(0xFF0284C7),
                            onClick = { uriHandler.openUri("https://hrms.jangeby.com/privacy-policy") }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileMenuRow(
                            icon = Icons.Default.Delete,
                            label = "Hapus Akun & Data",
                            color = Danger,
                            onClick = { uriHandler.openUri("https://hrms.jangeby.com/delete-account") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Logout Button
            item {
                Button(
                    onClick = { showLogoutConfirmSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("KELUAR / LOGOUT", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // App footer info
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("JANGEBY SYSTEM", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun ProfileMenuRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
    }
}
