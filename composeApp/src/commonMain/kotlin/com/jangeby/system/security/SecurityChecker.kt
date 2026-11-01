package com.jangeby.system.security

import kotlinx.coroutines.flow.StateFlow

expect object SecurityChecker {
    val violationState: StateFlow<SecurityViolation>
    fun checkSecurity(): SecurityViolation
    fun setMockDetected(detected: Boolean, reason: String = "")
    fun openDeveloperSettings()
    fun exitApp()
}
