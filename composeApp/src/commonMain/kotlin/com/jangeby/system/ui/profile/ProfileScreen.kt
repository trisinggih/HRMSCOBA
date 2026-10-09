package com.jangeby.system.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.ui.component.*
import com.jangeby.system.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var successMsg by remember { mutableStateOf("") }

    LaunchedEffect(successMsg) {
        if (successMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(successMsg)
            successMsg = ""
        }
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Ganti Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        label = { Text("Konfirmasi Password") },
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
                        if (newPassword == confirmPassword && newPassword.isNotEmpty()) {
                            showChangePasswordDialog = false
                            successMsg = "Password berhasil diubah!"
                            oldPassword = ""; newPassword = ""; confirmPassword = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            HrmsTopBar(title = "Profil Saya", onNavigateBack = onNavigateBack)
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
            // Profile Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryVariant)))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(50.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Budi Santoso", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Staff IT · Head Office", color = Color.White.copy(0.8f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("EMP-001", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader("Informasi Akun")
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoRow("Email", "budi.santoso@company.com")
                        InfoRow("No. HP", "081234567890")
                        InfoRow("Status", "Aktif")
                        InfoRow("Bergabung", "01 Januari 2022")
                    }
                }
            }

            // Menu Options
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
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
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        ProfileMenuRow(
                            icon = Icons.Default.Notifications,
                            label = "Pengaturan Notifikasi",
                            color = Color(0xFFD97706),
                            onClick = { }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        ProfileMenuRow(
                            icon = Icons.Default.Info,
                            label = "Tentang Aplikasi",
                            color = Color(0xFF6B7280),
                            onClick = { }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Logout Button
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("KELUAR / LOGOUT", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // App version
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("HRMS App v1.0.0 – Powered by Jangeby", fontSize = 11.sp, color = Color(0xFF9CA3AF))
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
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
    }
}
