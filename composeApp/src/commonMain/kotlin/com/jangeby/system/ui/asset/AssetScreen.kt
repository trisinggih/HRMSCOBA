package com.jangeby.system.ui.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jangeby.system.data.remote.AssetItem
import com.jangeby.system.ui.component.HrmsTopBar
import com.jangeby.system.ui.theme.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetScreen(
    onNavigateBack: () -> Unit,
    viewModel: AssetViewModel = koinInject()
) {
    val assets by viewModel.assets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            HrmsTopBar(
                title = "Aset Saya",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadAssets() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FA))
                .padding(padding)
        ) {
            if (isLoading && assets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (!errorMessage.isNullOrEmpty() && assets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Gagal memuat data aset.",
                            color = Danger,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadAssets() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
            } else if (assets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada aset yang dipegang.",
                        color = Color(0xFF757575),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(assets, key = { it.id }) { item ->
                        AssetItemCard(item = item)
                    }
                }
            }
        }
    }
}

/**
 * Item card layout matching item_asset.xml & AssetAdapter.kt
 */
@Composable
private fun AssetItemCard(item: AssetItem) {
    val status = item.status ?: "-"
    val (statusBg, statusTextColor) = when (status) {
        "Tersedia" -> Pair(Color(0xFFDCFCE7), Color(0xFF16A34A))
        "Dipinjamkan" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
        "Pemeliharaan" -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
        else -> Pair(Color(0xFFF3F4F6), Color(0xFF6B7280))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Asset Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status,
                        color = statusTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Asset Type
            Text(
                text = "Jenis: ${item.type ?: "-"}",
                color = TextSecondary,
                fontSize = 14.sp
            )

            // Serial Number
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Serial: ${item.serialNumber ?: "-"}",
                color = TextSecondary,
                fontSize = 14.sp
            )

            // Inventory Number (if available)
            if (!item.inventoryNumber.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No. Inventaris: ${item.inventoryNumber}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Purchase Date (if available)
            if (!item.purchaseDate.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tgl Pembelian: ${item.purchaseDate}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Notes
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Catatan: ${item.notes ?: "-"}",
                color = TextPrimary,
                fontSize = 14.sp
            )
        }
    }
}
