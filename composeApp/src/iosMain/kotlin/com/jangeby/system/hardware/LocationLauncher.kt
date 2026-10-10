package com.jangeby.system.hardware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.darwin.NSObject

actual class LocationLauncher(
    private val onLaunch: () -> Unit
) {
    actual fun fetchLocation() {
        onLaunch()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberLocationLauncher(onResult: (LocationResult) -> Unit): LocationLauncher {
    val delegate = remember {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            var locationManager: CLLocationManager? = null

            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                if (location != null) {
                    onResult(LocationResult(location.coordinate.useContents { latitude }, location.coordinate.useContents { longitude }, true))
                    manager.stopUpdatingLocation()
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                onResult(LocationResult(0.0, 0.0, false, didFailWithError.localizedDescription))
            }
            
            override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: Int) {
                if (didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse || 
                    didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedAlways) {
                    manager.startUpdatingLocation()
                } else if (didChangeAuthorizationStatus == kCLAuthorizationStatusDenied || 
                           didChangeAuthorizationStatus == kCLAuthorizationStatusRestricted) {
                    onResult(LocationResult(0.0, 0.0, false, "Izin lokasi ditolak"))
                }
            }
        }
    }

    return remember {
        LocationLauncher {
            val manager = CLLocationManager()
            delegate.locationManager = manager
            manager.delegate = delegate
            
            val status = CLLocationManager.authorizationStatus()
            if (status == kCLAuthorizationStatusNotDetermined) {
                manager.requestWhenInUseAuthorization()
            } else if (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways) {
                manager.startUpdatingLocation()
            } else {
                onResult(LocationResult(0.0, 0.0, false, "Izin lokasi ditolak"))
            }
        }
    }
}
