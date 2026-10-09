package com.jangeby.system.hardware

import platform.UIKit.UIDevice

actual object DeviceInfoProvider {
    actual fun getDeviceId(): String {
        return UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "ios_unknown_device"
    }

    actual fun getDeviceName(): String {
        return UIDevice.currentDevice.name
    }
}
