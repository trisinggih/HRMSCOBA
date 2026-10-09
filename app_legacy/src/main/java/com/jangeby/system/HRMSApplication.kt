package com.jangeby.system

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.jangeby.system.ui.BlockedActivity
import com.jangeby.system.utils.SecurityHelper

class HRMSApplication : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    // Security check dipindahkan ke SplashActivity/MainActivity saja
    // agar tidak memicu force close pada setiap Activity (termasuk CameraActivity)
    private fun checkSecurity(activity: Activity) {
        // Jangan block layar BlockedActivity itu sendiri
        if (activity is BlockedActivity) return

        // Hanya jalankan pada RELEASE build
        if (BuildConfig.DEBUG) return

        var reason: String? = null

        if (SecurityHelper.isDeviceRooted(this)) {
            reason = "Aplikasi mendeteksi bahwa perangkat Anda telah di-Root. Akses diblokir."
        }
        // DINONAKTIFKAN: developer mode dan VPN check terlalu agresif dan menyebabkan force close
        // karena banyak device testing memiliki developer mode aktif
        // else if (SecurityHelper.isDeveloperModeEnabled(this)) { ... }
        // else if (SecurityHelper.isVpnActive(this)) { ... }
        // else if (SecurityHelper.isMockLocationEnabled(this)) { ... }

        if (reason != null) {
            val intent = Intent(activity, BlockedActivity::class.java).apply {
                putExtra("EXTRA_REASON", reason)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            activity.startActivity(intent)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        // DINONAKTIFKAN sementara — pemicu force close di CameraActivity & emulator
        // checkSecurity(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
