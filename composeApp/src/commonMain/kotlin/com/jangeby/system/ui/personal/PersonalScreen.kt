package com.jangeby.system.ui.personal

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jangeby.system.data.remote.EducationModel
import com.jangeby.system.data.remote.EducationRequest
import com.jangeby.system.data.remote.EmployeeProfile
import com.jangeby.system.data.remote.FamilyModel
import com.jangeby.system.data.remote.FamilyRequest
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(
    onNavigateBack: () -> Unit,
    viewModel: PersonalViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Info Pribadi", "Keluarga", "Pendidikan")

    val profile by viewModel.profile.collectAsState()
    val families by viewModel.families.collectAsState()
    val educations by viewModel.educations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Dialog state for family
    var showFamilyDialog by remember { mutableStateOf(false) }
    var editingFamily by remember { mutableStateOf<FamilyModel?>(null) }
    var familyToDelete by remember { mutableStateOf<FamilyModel?>(null) }

    // Dialog state for education
    var showEducationDialog by remember { mutableStateOf(false) }
    var editingEducation by remember { mutableStateOf<EducationModel?>(null) }
    var educationToDelete by remember { mutableStateOf<EducationModel?>(null) }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Data Pribadi",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadAll() }) {
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary
                )
            }

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Danger,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = successMessage!!,
                            color = Color(0xFF16A34A),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> PersonalInfoTab(profile)
                1 -> FamilyTab(
                    families = families,
                    onAddClick = {
                        editingFamily = null
                        showFamilyDialog = true
                    },
                    onEditClick = { item ->
                        editingFamily = item
                        showFamilyDialog = true
                    },
                    onDeleteClick = { item ->
                        familyToDelete = item
                    }
                )
                2 -> EducationTab(
                    educations = educations,
                    onAddClick = {
                        editingEducation = null
                        showEducationDialog = true
                    },
                    onEditClick = { item ->
                        editingEducation = item
                        showEducationDialog = true
                    },
                    onDeleteClick = { item ->
                        educationToDelete = item
                    }
                )
            }
        }
    }

    // Dialog Tambah/Edit Keluarga
    if (showFamilyDialog) {
        FamilyFormDialog(
            initialItem = editingFamily,
            isSubmitting = isSubmitting,
            onDismiss = { showFamilyDialog = false },
            onSave = { req ->
                if (editingFamily != null) {
                    viewModel.updateFamily(editingFamily!!.id, req) {
                        showFamilyDialog = false
                    }
                } else {
                    viewModel.storeFamily(req) {
                        showFamilyDialog = false
                    }
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Keluarga
    if (familyToDelete != null) {
        AlertDialog(
            onDismissRequest = { familyToDelete = null },
            title = { Text("Hapus Data Keluarga", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus ${familyToDelete?.name} dari data keluarga?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = familyToDelete!!.id
                        familyToDelete = null
                        viewModel.deleteFamily(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { familyToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Tambah/Edit Pendidikan
    if (showEducationDialog) {
        EducationFormDialog(
            initialItem = editingEducation,
            isSubmitting = isSubmitting,
            onDismiss = { showEducationDialog = false },
            onSave = { req ->
                if (editingEducation != null) {
                    viewModel.updateEducation(editingEducation!!.id, req) {
                        showEducationDialog = false
                    }
                } else {
                    viewModel.storeEducation(req) {
                        showEducationDialog = false
                    }
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Pendidikan
    if (educationToDelete != null) {
        AlertDialog(
            onDismissRequest = { educationToDelete = null },
            title = { Text("Hapus Riwayat Pendidikan", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus riwayat pendidikan di ${educationToDelete?.institutionName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = educationToDelete!!.id
                        educationToDelete = null
                        viewModel.deleteEducation(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { educationToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun PersonalInfoTab(profile: EmployeeProfile?) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryVariant)))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profile?.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = profile!!.photoUrl,
                                    contentDescription = profile.name,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(46.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = profile?.name ?: "-",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile?.employeeId ?: "-"} · ${profile?.position ?: "-"}",
                            color = Color.White.copy(0.85f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Informasi Umum")
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow("NIK Karyawan", profile?.employeeId ?: "-")
                    InfoRow("Nama Lengkap", profile?.name ?: "-")
                    InfoRow("Email", profile?.email ?: "-")
                    InfoRow("No. HP", profile?.phone ?: "-")
                    InfoRow("Tanggal Lahir", profile?.birthDate ?: "-")
                    InfoRow("Alamat", profile?.address ?: "-")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Informasi Jabatan")
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow("Perusahaan", profile?.company ?: "-")
                    InfoRow("Jabatan", profile?.position ?: "-")
                    InfoRow("Departemen", profile?.department ?: "-")
                    InfoRow("Divisi", profile?.division ?: "-")
                    InfoRow("Cabang", profile?.branch ?: "-")
                }
            }
        }
    }
}

@Composable
fun FamilyTab(
    families: List<FamilyModel>,
    onAddClick: () -> Unit,
    onEditClick: (FamilyModel) -> Unit,
    onDeleteClick: (FamilyModel) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Data Keluarga (${families.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontSize = 12.sp)
                }
            }
        }

        items(families) { member ->
            val relText = when (member.relation.lowercase()) {
                "spouse", "husband", "wife" -> "Suami / Istri"
                "child" -> "Anak"
                "parent" -> "Orang Tua"
                else -> member.relation.ifBlank { "Lainnya" }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BgTasks),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (member.gender?.lowercase() == "female" || member.gender?.lowercase() == "perempuan")
                                Icons.Default.Favorite
                            else
                                Icons.Default.Person,
                            null,
                            tint = IconTasks,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                member.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF111827)
                            )
                            if (member.isEmergencyContact == 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFFE4E6))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("Darurat", fontSize = 9.sp, color = Danger, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(relText, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Medium)
                        if (!member.phone.isNullOrBlank()) {
                            Text(member.phone, fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Row {
                        IconButton(onClick = { onEditClick(member) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteClick(member) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (families.isEmpty()) {
            item {
                EmptyState("Belum ada data keluarga")
            }
        }
    }
}

@Composable
fun EducationTab(
    educations: List<EducationModel>,
    onAddClick: () -> Unit,
    onEditClick: (EducationModel) -> Unit,
    onDeleteClick: (EducationModel) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Riwayat Pendidikan (${educations.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontSize = 12.sp)
                }
            }
        }

        items(educations) { edu ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BgPayslip),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Home, null, tint = IconPayslip, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            edu.institutionName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = edu.level + (if (!edu.major.isNullOrBlank()) " – ${edu.major}" else ""),
                            fontSize = 12.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (edu.endYear != null) {
                                Text("Lulus: ${edu.endYear}", fontSize = 11.sp, color = TextSecondary)
                            }
                            if (edu.gpa != null) {
                                Text("IPK: ${edu.gpa}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { onEditClick(edu) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteClick(edu) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (educations.isEmpty()) {
            item {
                EmptyState("Belum ada riwayat pendidikan")
            }
        }
    }
}

// Dialog Form Tambah / Edit Keluarga
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyFormDialog(
    initialItem: FamilyModel?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (FamilyRequest) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var phone by remember { mutableStateOf(initialItem?.phone ?: "") }
    var relationRaw by remember {
        mutableStateOf(
            when (initialItem?.relation?.lowercase()) {
                "spouse", "husband", "wife" -> "Suami/Istri"
                "child" -> "Anak"
                "parent" -> "Orang Tua"
                else -> "Lainnya"
            }
        )
    }
    var isEmergency by remember { mutableStateOf(initialItem?.isEmergencyContact == 1) }

    val relationOptions = listOf("Suami/Istri", "Anak", "Orang Tua", "Lainnya")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (initialItem != null) "Edit Data Keluarga" else "Tambah Data Keluarga",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Lengkap *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Hubungan Keluarga:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(relationOptions) { option ->
                    FilterChip(
                        selected = relationRaw == option,
                        onClick = { relationRaw = option },
                        label = { Text(option, fontSize = 11.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Nomor Telepon / WhatsApp") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().clickable { isEmergency = !isEmergency },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isEmergency,
                    onCheckedChange = { isEmergency = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Jadikan Kontak Darurat", fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val relationApi = when (relationRaw) {
                            "Suami/Istri" -> "spouse"
                            "Anak" -> "child"
                            "Orang Tua" -> "parent"
                            else -> "other"
                        }
                        onSave(
                            FamilyRequest(
                                name = name.trim(),
                                relation = relationApi,
                                phone = phone.trim().ifBlank { null },
                                gender = if (relationRaw == "Suami/Istri") "other" else "other",
                                dob = initialItem?.birthDate ?: initialItem?.dob ?: "2000-01-01",
                                isEmergencyContact = isEmergency
                            )
                        )
                    }
                },
                enabled = !isSubmitting && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Simpan")
                }
            }
        }
    }
}

// Dialog Form Tambah / Edit Pendidikan
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationFormDialog(
    initialItem: EducationModel?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (EducationRequest) -> Unit
) {
    var institutionName by remember { mutableStateOf(initialItem?.institutionName ?: "") }
    var selectedLevel by remember { mutableStateOf(initialItem?.level ?: "S1") }
    var major by remember { mutableStateOf(initialItem?.major ?: "") }
    var endYear by remember { mutableStateOf(initialItem?.endYear?.toString() ?: "") }
    var gpa by remember { mutableStateOf(initialItem?.gpa?.toString() ?: "") }

    val levels = listOf("SD", "SMP", "SMA/SMK", "D3", "S1", "S2", "S3")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (initialItem != null) "Edit Riwayat Pendidikan" else "Tambah Riwayat Pendidikan",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text("Jenjang Pendidikan:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(levels) { lvl ->
                    FilterChip(
                        selected = selectedLevel == lvl,
                        onClick = { selectedLevel = lvl },
                        label = { Text(lvl, fontSize = 11.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = institutionName,
                onValueChange = { institutionName = it },
                label = { Text("Nama Sekolah / Universitas *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Jurusan / Program Studi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = endYear,
                    onValueChange = { endYear = it },
                    label = { Text("Tahun Lulus") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = gpa,
                    onValueChange = { gpa = it },
                    label = { Text("IPK / Nilai") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (institutionName.isNotBlank()) {
                        onSave(
                            EducationRequest(
                                institutionName = institutionName.trim(),
                                level = selectedLevel,
                                major = major.trim().ifBlank { null },
                                endYear = endYear.toIntOrNull(),
                                gpa = gpa.toDoubleOrNull()
                            )
                        )
                    }
                },
                enabled = !isSubmitting && institutionName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Simpan")
                }
            }
        }
    }
}
