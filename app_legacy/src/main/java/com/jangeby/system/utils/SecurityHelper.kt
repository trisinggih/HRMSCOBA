package com.jangeby.system.utils

import android.content.Context
import android.location.Location
import android.os.Build
import android.provider.Settings
import com.scottyab.rootbeer.RootBeer

object SecurityHelper {

    /**
     * Mengecek apakah device telah di-root.
     * Menggunakan library RootBeer untuk pengecekan yang lebih mendalam.
     */
    fun isDeviceRooted(context: Context): Boolean {
        if (com.jangeby.system.BuildConfig.DEBUG) {
            return false
        }
        val rootBeer = RootBeer(context)
        return rootBeer.isRooted
    }

    /**
     * Mengecek apakah opsi Mock Location (Fake GPS) aktif di level OS (untuk device lawas).
     */
    fun isMockLocationEnabled(context: Context): Boolean {
        var isMock = false
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                isMock = Settings.Secure.getString(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION) != "0"
            } else {
                // Di Android M ke atas, kita butuh izin ACCESS_MOCK_LOCATION atau rely on location.isMock()
                // Untuk simplifikasi, kita asumsikan check khusus dilakukan per titik kordinat (isLocationMocked).
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isMock
    }

    /**
     * Mengecek apakah objek lokasi yang diterima berasal dari provider Mock (Fake GPS).
     */
    fun isLocationMocked(location: Location?): Boolean {
        if (com.jangeby.system.BuildConfig.DEBUG) return false
        if (location == null) return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    /**
     * Mengecek apakah VPN sedang aktif.
     */
    fun isVpnActive(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                return capabilities != null && capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.getNetworkInfo(android.net.ConnectivityManager.TYPE_VPN)
                return networkInfo != null && networkInfo.isConnected
            }
        }
        return false
    }

    /**
     * Mengecek apakah Developer Mode (Opsi Pengembang) aktif.
     */
    fun isDeveloperModeEnabled(context: Context): Boolean {
        // Jangan blokir jika dalam mode Debug
        if (com.jangeby.system.BuildConfig.DEBUG) {
            return false
        }
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        } catch (e: Exception) {
            false
        }
    }
}
