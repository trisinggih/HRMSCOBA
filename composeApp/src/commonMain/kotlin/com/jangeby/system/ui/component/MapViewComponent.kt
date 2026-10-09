package com.jangeby.system.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapViewComponent(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    branchLatitude: Double? = null,
    branchLongitude: Double? = null,
    radius: Double = 100.0,
    recenterTrigger: Long = 0L
)
