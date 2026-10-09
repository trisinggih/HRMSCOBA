package com.jangeby.system.hardware

actual object LocationTracker {
    actual fun start() {}
    actual fun stop() {}
    actual val isRunning: Boolean = false
}
