package com.jangeby.system.ui.component

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIColor
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapViewComponent(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    branchLatitude: Double?,
    branchLongitude: Double?,
    radius: Double,
    branches: List<BranchItemData>,
    recenterTrigger: Long
) {
    val delegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
                if (rendererForOverlay is MKCircle) {
                    val circleRenderer = MKCircleRenderer(rendererForOverlay)
                    circleRenderer.fillColor = UIColor(red = 0.0, green = 0.53, blue = 0.47, alpha = 0.25)
                    circleRenderer.strokeColor = UIColor(red = 0.0, green = 0.53, blue = 0.47, alpha = 0.9)
                    circleRenderer.lineWidth = 2.0
                    return circleRenderer
                }
                return MKOverlayRenderer(rendererForOverlay)
            }
        }
    }

    val mapView = remember {
        MKMapView().apply {
            this.delegate = delegate
            this.showsUserLocation = false
        }
    }

    LaunchedEffect(latitude, longitude, branchLatitude, branchLongitude, recenterTrigger) {
        val targetLat = if (latitude != 0.0) latitude else (branchLatitude ?: 0.0)
        val targetLon = if (longitude != 0.0) longitude else (branchLongitude ?: 0.0)
        if (targetLat != 0.0 && targetLon != 0.0) {
            val centerCoord = CLLocationCoordinate2DMake(targetLat, targetLon)
            val zoomDistance = (radius * 3.5).coerceAtLeast(800.0)
            val region = MKCoordinateRegionMakeWithDistance(centerCoord, zoomDistance, zoomDistance)
            mapView.setRegion(region, animated = true)
        }
    }

    UIKitView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // Safely remove existing annotations without crashing if view.annotations is null
            try {
                val oldAnnotations = view.annotations
                if (oldAnnotations != null) {
                    val toRemove = oldAnnotations.filter { it !is MKUserLocation }
                    if (toRemove.isNotEmpty()) {
                        view.removeAnnotations(toRemove)
                    }
                }
            } catch (_: Throwable) {}

            // Safely remove existing overlays without crashing if view.overlays is null
            try {
                val oldOverlays = view.overlays
                if (oldOverlays != null && oldOverlays.isNotEmpty()) {
                    view.removeOverlays(oldOverlays)
                }
            } catch (_: Throwable) {}

            // 1. Draw All Branch Geofence Radii & Annotations
            val safeBranches = branches ?: emptyList()
            val branchesToDraw = if (safeBranches.isNotEmpty()) {
                safeBranches
            } else if (branchLatitude != null && branchLongitude != null && branchLatitude != 0.0 && branchLongitude != 0.0) {
                listOf(BranchItemData(branchLatitude, branchLongitude, radius, "Lokasi Kantor Cabang"))
            } else {
                emptyList()
            }

            for (b in branchesToDraw) {
                if (b.latitude == 0.0 || b.longitude == 0.0) continue
                val branchCoord = CLLocationCoordinate2DMake(b.latitude, b.longitude)
                val circle = MKCircle.circleWithCenterCoordinate(branchCoord, b.radius)
                view.addOverlay(circle)

                val branchPin = MKPointAnnotation().apply {
                    setCoordinate(branchCoord)
                    setTitle(if (b.name.isNotBlank()) b.name else "Kantor / Cabang")
                }
                view.addAnnotation(branchPin)
            }

            // 2. Draw User Location Marker
            if (latitude != 0.0 && longitude != 0.0) {
                val userCoord = CLLocationCoordinate2DMake(latitude, longitude)
                val userPin = MKPointAnnotation().apply {
                    setCoordinate(userCoord)
                    setTitle("Lokasi Anda")
                }
                view.addAnnotation(userPin)
            }
        }
    )
}

