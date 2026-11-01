package com.jangeby.system.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun MapViewComponent(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    branchLatitude: Double?,
    branchLongitude: Double?,
    radius: Double,
    branches: List<BranchItemData>,
    recenterTrigger: Long
) {
    Box(modifier = modifier.background(Color.LightGray), contentAlignment = Alignment.Center) {
        Text("Map View (iOS Mock)\nLat: $latitude, Lon: $longitude\nBranches: ${branches.size}")
    }
}
