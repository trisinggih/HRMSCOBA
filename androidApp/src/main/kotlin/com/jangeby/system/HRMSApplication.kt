package com.jangeby.system

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jangeby.system.data.local.initSettings
import com.jangeby.system.di.initKoin

class HRMSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Inisialisasi context untuk EncryptedSharedPreferences
        initSettings(this)
        
        // Inisialisasi Koin untuk shared module
        initKoin()

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return

            // FCM Notification Channel
            val fcmChannel = NotificationChannel(
                "hrms_notification_channel",
                "HRMS Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi dari sistem HRMS"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(fcmChannel)

            // Location Foreground Service Channel
            val locationChannel = NotificationChannel(
                "location_service_channel",
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan pemantauan lokasi presensi"
            }
            notificationManager.createNotificationChannel(locationChannel)
        }
    }
}
