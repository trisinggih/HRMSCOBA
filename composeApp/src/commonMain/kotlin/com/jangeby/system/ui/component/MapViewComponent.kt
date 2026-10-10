package com.jangeby.system.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class BranchItemData(
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val name: String = ""
)

@Composable
expect fun MapViewComponent(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    branchLatitude: Double? = null,
    branchLongitude: Double? = null,
    radius: Double = 100.0,
    branches: List<BranchItemData> = emptyList(),
    recenterTrigger: Long = 0L
)
