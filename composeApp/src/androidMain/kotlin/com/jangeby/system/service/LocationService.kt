package com.jangeby.system.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class SyncLocationBody(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double
)

class LocationService : Service() {

    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var wakeLock: PowerManager.WakeLock? = null
    private var isLocationUpdatesActive: Boolean = false
    private var lastSentTimestamp: Long = 0L
    private var httpClient: HttpClient? = null

    companion object {
        private const val TAG = "LocationService"
        private const val CHANNEL_ID = "location_service_channel"
        private const val NOTIFICATION_ID = 1001

        // Interval pembaruan lokasi: tiap 2 menit (120 detik) agar tidak membebani server
        private const val UPDATE_INTERVAL_MS = 120_000L
        private const val FASTEST_INTERVAL_MS = 120_000L
        private const val MAX_UPDATE_DELAY_MS = 150_000L
        private const val MIN_SEND_INTERVAL_MS = 100_000L // Minimal jeda antar pengiriman ke server

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val startIntent = Intent(context, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, LocationService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        // Acquire WakeLock agar CPU tetap aktif di background saat layar mati / mode hemat daya
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "HRMS:LocationTrackingWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.d(TAG, "WakeLock acquired successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock: ${e.message}")
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processAndSendLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else null

        val iconRes = try {
            val resId = resources.getIdentifier("ic_jangeby2_round", "mipmap", packageName)
            if (resId != 0) resId else android.R.drawable.ic_menu_mylocation
        } catch (e: Exception) {
            android.R.drawable.ic_menu_mylocation
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jangeby System")
            .setContentText("Pastikan aplikasi ini selalu on agar anda selalu menerima informasi")
            .setSmallIcon(iconRes)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)

        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent)
        }

        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (!isLocationUpdatesActive) {
            requestLocationUpdates()
        }

        return START_STICKY
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setMaxUpdateDelayMillis(MAX_UPDATE_DELAY_MS)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permissions are not granted.")
            return
        }

        try {
            // Ambil lokasi terakhir langsung agar segera dikirim ke server saat pertama kali clock-in
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                lastLoc?.let { processAndSendLocation(it) }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isLocationUpdatesActive = true
            Log.d(TAG, "Location updates requested (Interval: ${UPDATE_INTERVAL_MS / 1000}s)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates", e)
        }
    }

    private fun processAndSendLocation(location: Location) {
        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }

        if (isMock) {
            Log.w(TAG, "Fake GPS / Mock Location detected! Discarding location.")
            return
        }

        val now = System.currentTimeMillis()
        if (lastSentTimestamp != 0L && (now - lastSentTimestamp) < MIN_SEND_INTERVAL_MS) {
            Log.d(TAG, "Throttled location ping: sent recently (${(now - lastSentTimestamp) / 1000}s ago)")
            return
        }

        lastSentTimestamp = now
        Log.d(TAG, "Live Location: ${location.latitude}, ${location.longitude} (Acc: ${location.accuracy}m)")
        sendLocationToServer(location.latitude, location.longitude, location.accuracy)
    }

    private fun sendLocationToServer(lat: Double, lng: Double, acc: Float) {
        serviceScope.launch {
            try {
                val token = AppSettings().token
                if (token.isNullOrEmpty()) {
                    Log.w(TAG, "No authentication token found. Skipping sync.")
                    return@launch
                }

                val client = httpClient ?: ApiClient.create().also { httpClient = it }
                val response = client.post(ApiClient.BASE_URL + "attendance/sync-location") {
                    contentType(ContentType.Application.Json)
                    setBody(SyncLocationBody(latitude = lat, longitude = lng, accuracy = acc.toDouble()))
                }

                if (response.status.value in 200..299) {
                    Log.d(TAG, "Location synced successfully: $lat, $lng")
                } else {
                    Log.e(TAG, "Failed to sync location (${response.status.value})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing location to server", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isLocationUpdatesActive = false

        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates: ${e.message}")
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock: ${e.message}")
        }

        try {
            httpClient?.close()
        } catch (_: Exception) {}

        serviceScope.cancel()
        Log.d(TAG, "LocationService destroyed and stopped")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel untuk pembaruan lokasi presensi latar belakang"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}

