package com.jangeby.system.hardware

import com.jangeby.system.data.local.getAppContext
import com.jangeby.system.service.LocationService

actual object LocationTracker {
    actual fun start() {
        val ctx = getAppContext() ?: return
        LocationService.startService(ctx)
    }

    actual fun stop() {
        val ctx = getAppContext() ?: return
        LocationService.stopService(ctx)
    }

    actual val isRunning: Boolean
        get() = LocationService.isRunning
}
