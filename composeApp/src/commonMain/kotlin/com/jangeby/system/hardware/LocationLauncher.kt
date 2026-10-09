package com.jangeby.system.hardware

import androidx.compose.runtime.Composable

expect class LocationLauncher {
    fun fetchLocation()
}

@Composable
expect fun rememberLocationLauncher(onResult: (LocationResult) -> Unit): LocationLauncher
