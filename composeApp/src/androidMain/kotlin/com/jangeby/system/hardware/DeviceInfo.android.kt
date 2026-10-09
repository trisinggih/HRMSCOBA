package com.jangeby.system.hardware

import android.provider.Settings
import com.jangeby.system.data.local.getAppContext

actual object DeviceInfoProvider {
    actual fun getDeviceId(): String {
        val context = getAppContext()
        val androidId = if (context != null) {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } else null

        return if (!androidId.isNullOrBlank()) {
            androidId
        } else {
            "android_unknown_device"
        }
    }

    actual fun getDeviceName(): String {
        val manufacturer = android.os.Build.MANUFACTURER ?: ""
        val model = android.os.Build.MODEL ?: ""
        val full = "$manufacturer $model".trim()
        return if (full.isNotEmpty()) full else "Android Device"
    }
}
