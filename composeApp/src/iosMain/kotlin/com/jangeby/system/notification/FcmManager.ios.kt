package com.jangeby.system.notification

actual object FcmManager {
    actual fun syncToken() {
        // No-op on iOS
    }
}
