package com.jangeby.system.update

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlin.system.exitProcess

/**
 * Manages Google Play Store forced (immediate) in-app updates.
 *
 * When a newer release is published on the Google Play Store, this manager triggers
 * Google Play's IMMEDIATE update flow so that the app automatically prompts the user
 * to update and blocks access until the latest version is installed.
 */
class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var forcedUpdateDialog: AlertDialog? = null

    var isUpdateRequired: Boolean = false
        private set

    companion object {
        private const val TAG = "InAppUpdateManager"
        const val REQUEST_CODE_IMMEDIATE_UPDATE = 1701
    }

    /**
     * Checks if an update is available from Google Play Store and triggers an immediate update flow.
     */
    fun checkForUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val availability = appUpdateInfo.updateAvailability()
            val availableVersion = appUpdateInfo.availableVersionCode()
            val currentVersion = getCurrentVersionCode()

            Log.d(TAG, "Update check: availability=$availability, currentVer=$currentVersion, availableVer=$availableVersion")

            if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                isUpdateRequired = true
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    Log.i(TAG, "New release detected ($availableVersion). Triggering immediate auto update...")
                    startImmediateUpdate(appUpdateInfo)
                } else {
                    Log.w(TAG, "New release detected ($availableVersion), but IMMEDIATE update type is not allowed directly. Showing forced update dialog.")
                    showForcedUpdateDialog()
                }
            } else {
                isUpdateRequired = false
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to check for Play Store update: ${e.message}")
        }
    }

    /**
     * Resumes an update if an immediate update was already started and user navigated away,
     * or re-checks if an update is still pending.
     */
    fun resumeUpdateIfNeeded() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val availability = appUpdateInfo.updateAvailability()

            Log.d(TAG, "Resume check: availability=$availability")

            when (availability) {
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // Update is already in progress, resume immediate flow
                    isUpdateRequired = true
                    Log.i(TAG, "Update already in progress. Resuming immediate update flow...")
                    startImmediateUpdate(appUpdateInfo)
                }
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    isUpdateRequired = true
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        Log.i(TAG, "Update available on resume. Triggering immediate auto update...")
                        startImmediateUpdate(appUpdateInfo)
                    } else {
                        showForcedUpdateDialog()
                    }
                }
                else -> {
                    // Update has completed or none available
                    if (availability != UpdateAvailability.UPDATE_AVAILABLE &&
                        availability != UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        isUpdateRequired = false
                        forcedUpdateDialog?.dismiss()
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to check Play Store update on resume: ${e.message}")
        }
    }

    /**
     * Handles activity result for the immediate update request.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_IMMEDIATE_UPDATE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.i(TAG, "Immediate update completed successfully!")
                    isUpdateRequired = false
                    forcedUpdateDialog?.dismiss()
                }
                Activity.RESULT_CANCELED -> {
                    Log.w(TAG, "Immediate update cancelled by user! Forcing update...")
                    isUpdateRequired = true
                    showForcedUpdateDialog()
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Log.e(TAG, "Immediate update failed! Result code: $resultCode")
                    isUpdateRequired = true
                    showForcedUpdateDialog()
                }
                else -> {
                    Log.w(TAG, "Immediate update flow ended with result code: $resultCode")
                    isUpdateRequired = true
                    showForcedUpdateDialog()
                }
            }
            return true
        }
        return false
    }

    /**
     * Launches Google Play's immediate update UI overlay.
     */
    private fun startImmediateUpdate(appUpdateInfo: AppUpdateInfo) {
        try {
            val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                options,
                REQUEST_CODE_IMMEDIATE_UPDATE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting immediate update flow: ${e.message}", e)
            showForcedUpdateDialog()
        }
    }

    /**
     * Shows a non-cancelable dialog when the user cancels the update or the update fails,
     * forcing them to update before using the app.
     */
    fun showForcedUpdateDialog() {
        if (activity.isFinishing || activity.isDestroyed) return

        activity.runOnUiThread {
            if (forcedUpdateDialog?.isShowing == true) return@runOnUiThread

            forcedUpdateDialog = AlertDialog.Builder(activity)
                .setTitle("Pembaruan Aplikasi Wajib")
                .setMessage("Versi terbaru aplikasi HRMS telah tersedia di Google Play Store. Untuk memastikan kelancaran dan keamanan data sistem, Anda wajib memperbarui aplikasi ke versi terbaru.")
                .setCancelable(false)
                .setPositiveButton("Perbarui Sekarang") { _, _ ->
                    redirectToPlayStore()
                }
                .setNegativeButton("Keluar") { _, _ ->
                    activity.finishAffinity()
                    exitProcess(0)
                }
                .create().apply {
                    setCanceledOnTouchOutside(false)
                    show()
                }
        }
    }

    /**
     * Redirects the user directly to the Google Play Store app listing.
     */
    fun redirectToPlayStore() {
        val packageName = activity.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            activity.startActivity(webIntent)
        }
    }

    /**
     * Cleans up any open dialogs when activity is destroyed.
     */
    fun onDestroy() {
        forcedUpdateDialog?.dismiss()
        forcedUpdateDialog = null
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.getPackageInfo(
                    activity.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.getPackageInfo(activity.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            -1L
        }
    }
}
