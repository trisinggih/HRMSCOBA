package com.jangeby.system.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object SecurityChecker {
    private val _violationState = MutableStateFlow<SecurityViolation>(SecurityViolation.None)
    actual val violationState: StateFlow<SecurityViolation> = _violationState.asStateFlow()

    actual fun setMockDetected(detected: Boolean, reason: String) {}

    actual fun checkSecurity(): SecurityViolation {
        return SecurityViolation.None
    }

    actual fun openDeveloperSettings() {}

    actual fun exitApp() {}
}
