package com.jangeby.system.hardware

expect object DeviceInfoProvider {
    fun getDeviceId(): String
    fun getDeviceName(): String
}
