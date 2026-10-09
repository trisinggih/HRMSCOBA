package com.jangeby.system.data.local

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createSettings(): Settings {
    // For iOS, NSUserDefaults is used here. For secure storage (Keychain), 
    // com.russhwolf:multiplatform-settings-no-arg + KeychainSettings can be used later.
    return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
}
