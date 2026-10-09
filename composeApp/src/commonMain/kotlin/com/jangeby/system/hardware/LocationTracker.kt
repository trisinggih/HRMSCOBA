package com.jangeby.system.hardware

expect object LocationTracker {
    fun start()
    fun stop()
    val isRunning: Boolean
}
