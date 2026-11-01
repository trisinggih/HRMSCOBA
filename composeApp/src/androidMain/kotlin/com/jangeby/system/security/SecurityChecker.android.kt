package com.jangeby.system.security

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.jangeby.system.data.local.getAppContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.system.exitProcess

actual object SecurityChecker {
    private const val TAG = "SecurityChecker"

    private val _violationState = MutableStateFlow<SecurityViolation>(SecurityViolation.None)
    actual val violationState: StateFlow<SecurityViolation> = _violationState.asStateFlow()

    private var mockDetectedManually = false
    private var mockManualReason: String? = null

    // Daftar package Fake GPS / Mock Location populer
    private val KNOWN_MOCK_PACKAGES = listOf(
        "com.lexa.fakegps",
        "com.incorporateapps.fakegps",
        "com.incorporateapps.fakegps_route",
        "com.rosteam.gpsemulator",
        "ru.gavrikov.mocklocations",
        "com.fakegps.mock",
        "com.location.changer",
        "com.gsmartstudio.fakegps",
        "com.marlon.floating.fake.location",
        "com.dreams.mocklocation",
        "com.fake.gps.location",
        "com.fly.gps",
        "com.ninja.gmaps",
        "com.app.fakegps",
        "com.theappninjas.fakegpsjoystick",
        "com.marlove.mockgps",
        "com.hopeway.fakegps",
        "com.blackout.fakegps",
        "cl.santander.fakegps",
        "com.mock.location",
        "com.pe.fakegps"
    )

    actual fun setMockDetected(detected: Boolean, reason: String) {
        val context = getAppContext()
        val isDebuggable = if (context != null) {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } else false

        // Pengecekan Mock GPS HANYA aktif pada versi RELEASE.
        // Pada versi DEBUG jangan dijalankan.
        if (isDebuggable) {
            mockDetectedManually = false
            mockManualReason = null
            return
        }

        mockDetectedManually = detected
        mockManualReason = if (detected) reason else null
        checkSecurity()
    }

    actual fun checkSecurity(): SecurityViolation {
        val context = getAppContext()
        if (context == null) {
            Log.w(TAG, "Application context not yet available for SecurityChecker")
            return _violationState.value
        }

        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Pengecekan keamanan (Mock GPS & Opsi Pengembang) HANYA berjalan pada APK versi RELEASE.
        // Untuk versi DEBUG jangan jalan sama sekali.
        if (isDebuggable) {
            mockDetectedManually = false
            mockManualReason = null
            _violationState.value = SecurityViolation.None
            return SecurityViolation.None
        }

        // 1. Pengecekan Aplikasi Mock GPS terpasang / aktif di sistem
        val mockReason = detectMockLocation(context)
        if (mockReason != null) {
            Log.w(TAG, "Security violation: $mockReason")
            val violation = SecurityViolation.MockLocationActive(
                title = "Lokasi Tiruan Terdeteksi",
                message = "$mockReason\n\nDemi keabsahan data presensi, silakan matikan atau copot pemasangan aplikasi tersebut."
            )
            _violationState.value = violation
            return violation
        }

        // 2. Khusus VERSI RELEASE: Larang Opsi Pengembang (Developer Options)
        if (isDeveloperOptionsEnabled(context)) {
            Log.w(TAG, "Security violation: Developer Options is ENABLED on release APK")
            val violation = SecurityViolation.DeveloperOptionsActive(
                title = "Opsi Pengembang Aktif",
                message = "Mode Opsi Pengembang (Developer Options) terdeteksi aktif pada perangkat Anda. Demi keamanan data dan keabsahan presensi, silakan nonaktifkan Opsi Pengembang pada Pengaturan perangkat untuk melanjutkan."
            )
            _violationState.value = violation
            return violation
        }

        // 3. Pengecekan Mock Location manual (terdeteksi saat pengambilan koordinat GPS)
        // Jika tidak ada aplikasi fake GPS dan Developer Options mati, flag manual di-reset saat recheck
        // agar pengguna dapat mencoba ulang membaca GPS yang valid.
        mockDetectedManually = false
        mockManualReason = null

        // Tidak ada pelanggaran keamanan
        _violationState.value = SecurityViolation.None
        return SecurityViolation.None
    }

    private fun isDeveloperOptionsEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun detectMockLocation(context: Context): String? {
        val pm = context.packageManager

        // A. Cek AppOps OPSTR_MOCK_LOCATION (Android M+)
        // Ketika suatu aplikasi pihak ketiga dipilih sebagai "Select mock location app" di Opsi Pengembang,
        // Android memberikan mode MODE_ALLOWED untuk op ini.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            if (appOpsManager != null) {
                try {
                    val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                    }
                    for (pkg in packages) {
                        if (pkg.packageName == context.packageName) continue
                        val appInfo = pkg.applicationInfo ?: continue

                        // 1. FILTER: Abaikan aplikasi bawaan sistem / vendor ROM (FLAG_SYSTEM / FLAG_UPDATED_SYSTEM_APP)
                        // seperti SalesStatistics, Google Play Services, Transsion/Infinix internal services.
                        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        if (isSystemApp) continue

                        // 2. FILTER: Abaikan package prefix bawaan OS / vendor OEM
                        val pkgLower = pkg.packageName.lowercase()
                        if (pkgLower.startsWith("android") ||
                            pkgLower.startsWith("com.android.") ||
                            pkgLower.startsWith("com.google.") ||
                            pkgLower.startsWith("com.transsion.") ||
                            pkgLower.startsWith("com.infinix.") ||
                            pkgLower.startsWith("com.tecno.") ||
                            pkgLower.startsWith("com.itel.") ||
                            pkgLower.startsWith("com.miui.") ||
                            pkgLower.startsWith("com.xiaomi.") ||
                            pkgLower.startsWith("com.samsung.") ||
                            pkgLower.startsWith("com.oppo.") ||
                            pkgLower.startsWith("com.coloros.") ||
                            pkgLower.startsWith("com.vivo.")
                        ) {
                            continue
                        }

                        // 3. FILTER: Hanya aplikasi yang secara eksplisit meminta permission ACCESS_MOCK_LOCATION
                        val requestedPerms = pkg.requestedPermissions
                        val requestsMock = requestedPerms?.contains("android.permission.ACCESS_MOCK_LOCATION") == true
                        if (!requestsMock) continue

                        val mode = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                appOpsManager.unsafeCheckOpNoThrow(
                                    AppOpsManager.OPSTR_MOCK_LOCATION,
                                    appInfo.uid,
                                    pkg.packageName
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                appOpsManager.checkOpNoThrow(
                                    AppOpsManager.OPSTR_MOCK_LOCATION,
                                    appInfo.uid,
                                    pkg.packageName
                                )
                            }
                        } catch (_: Exception) {
                            AppOpsManager.MODE_DEFAULT
                        }
                        if (mode == AppOpsManager.MODE_ALLOWED) {
                            val appName = try {
                                pm.getApplicationLabel(appInfo).toString()
                            } catch (_: Exception) {
                                pkg.packageName
                            }
                            return "Aplikasi '$appName' dipilih sebagai penyedia lokasi tiruan."
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "AppOps check exception: ${e.message}")
                }
            }
        }

        // B. Cek aplikasi Fake GPS populer yang terpasang
        for (pkgName in KNOWN_MOCK_PACKAGES) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, 0)
                }
                return "Aplikasi Fake GPS ($pkgName) terdeteksi terpasang di perangkat Anda."
            } catch (_: PackageManager.NameNotFoundException) {
                // Tidak terpasang
            } catch (_: Exception) {}
        }

        // C. Cek pengaturan OS lawas ALLOW_MOCK_LOCATION
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                if (Settings.Secure.getString(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION) != "0") {
                    return "Fitur Lokasi Tiruan (Mock Location) aktif pada sistem perangkat Anda."
                }
            } catch (_: Exception) {}
        }

        return null
    }

    actual fun openDeveloperSettings() {
        val context = getAppContext() ?: return
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    actual fun exitApp() {
        try {
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (_: Exception) {}
        exitProcess(0)
    }
}
