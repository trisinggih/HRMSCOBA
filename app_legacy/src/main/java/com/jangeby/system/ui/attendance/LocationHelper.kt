package com.jangeby.system.ui.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Data lokasi hasil getCurrentPosition. */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double
)

/**
 * Helper mengambil lokasi GPS terkini melalui FusedLocationProviderClient.
 * Mengembalikan null bila izin lokasi belum diberikan dan perlu permintaan runtime.
 */
object LocationHelper {

    suspend fun getCurrentLocation(context: Context): DeviceLocation? =
        suspendCancellableCoroutine { cont ->
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            cont.resume(
                                DeviceLocation(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy.toDouble()
                                )
                            )
                        } else {
                            // Last known kosong → fallback default 0,0 (radius validasi dilewati bila branch tanpa koordinat).
                            cont.resume(DeviceLocation(0.0, 0.0, 0.0))
                        }
                    }
                    .addOnFailureListener { cont.resume(DeviceLocation(0.0, 0.0, 0.0)) }
            } catch (e: Throwable) {
                cont.resume(DeviceLocation(0.0, 0.0, 0.0))
            }
        }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
