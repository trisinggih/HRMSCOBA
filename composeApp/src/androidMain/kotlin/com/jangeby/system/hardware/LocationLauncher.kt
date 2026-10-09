package com.jangeby.system.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

actual class LocationLauncher(
    private val onLaunch: () -> Unit
) {
    actual fun fetchLocation() {
        onLaunch()
    }
}

@Composable
actual fun rememberLocationLauncher(onResult: (LocationResult) -> Unit): LocationLauncher {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            obtainFreshLocation(context) { res ->
                currentOnResult(res)
            }
        } else {
            currentOnResult(LocationResult(0.0, 0.0, false, "Izin akses lokasi ditolak"))
        }
    }

    return remember(context) {
        LocationLauncher {
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                obtainFreshLocation(context) { res ->
                    currentOnResult(res)
                }
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun obtainFreshLocation(context: Context, onResult: (LocationResult) -> Unit) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        var completed = false
        val handler = Handler(Looper.getMainLooper())

        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                try { cts.cancel() } catch (_: Exception) {}
                fallbackToLocationManager(context, onResult)
            }
        }
        // Give FusedLocationProviderClient 6 seconds for fresh GPS lock before fallback
        handler.postDelayed(timeoutRunnable, 6000L)

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    if (location != null) {
                        onResult(LocationResult(location.latitude, location.longitude, true))
                    } else {
                        // If null, check lastLocation first or fallback to LocationManager
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                onResult(LocationResult(lastLoc.latitude, lastLoc.longitude, true))
                            } else {
                                fallbackToLocationManager(context, onResult)
                            }
                        }.addOnFailureListener {
                            fallbackToLocationManager(context, onResult)
                        }
                    }
                }
            }
            .addOnFailureListener {
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    fallbackToLocationManager(context, onResult)
                }
            }
    } catch (e: SecurityException) {
        onResult(LocationResult(0.0, 0.0, false, "Izin akses lokasi diperlukan"))
    } catch (e: Exception) {
        fallbackToLocationManager(context, onResult)
    }
}

@SuppressLint("MissingPermission")
private fun fallbackToLocationManager(context: Context, onResult: (LocationResult) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onResult(LocationResult(0.0, 0.0, false, "Layanan lokasi tidak tersedia"))
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            onResult(LocationResult(0.0, 0.0, false, "GPS tidak aktif. Silakan aktifkan GPS pada perangkat."))
            return
        }

        val provider = when {
            isGpsEnabled -> LocationManager.GPS_PROVIDER
            isNetworkEnabled -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }

        var delivered = false
        val handler = Handler(Looper.getMainLooper())

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!delivered) {
                    delivered = true
                    try { locationManager.removeUpdates(this) } catch (_: Exception) {}
                    onResult(LocationResult(location.latitude, location.longitude, true))
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val timeoutRunnable = Runnable {
            if (!delivered) {
                delivered = true
                try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                val lastKnown = try {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (_: Exception) { null }

                if (lastKnown != null) {
                    onResult(LocationResult(lastKnown.latitude, lastKnown.longitude, true))
                } else {
                    onResult(LocationResult(0.0, 0.0, false, "Gagal mendapatkan koordinat GPS terbaru."))
                }
            }
        }
        handler.postDelayed(timeoutRunnable, 5000L)

        try {
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (_: Exception) {
            try {
                locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            } catch (e: Exception) {
                handler.removeCallbacks(timeoutRunnable)
                val lastKnown = locationManager.getLastKnownLocation(provider)
                if (lastKnown != null) {
                    onResult(LocationResult(lastKnown.latitude, lastKnown.longitude, true))
                } else {
                    onResult(LocationResult(0.0, 0.0, false, e.message ?: "Gagal mendapatkan lokasi"))
                }
            }
        }
    } catch (e: SecurityException) {
        onResult(LocationResult(0.0, 0.0, false, "Izin lokasi diperlukan"))
    } catch (e: Exception) {
        onResult(LocationResult(0.0, 0.0, false, "Gagal mengambil lokasi: ${e.message}"))
    }
}
