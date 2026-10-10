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

    LaunchedEffect(latitude, longitude, recenterTrigger) {
        if (latitude != 0.0 && longitude != 0.0) {
            val userCoord = CLLocationCoordinate2DMake(latitude, longitude)
            val zoomDistance = (radius * 3.5).coerceAtLeast(800.0)
            val region = MKCoordinateRegionMakeWithDistance(userCoord, zoomDistance, zoomDistance)
            mapView.setRegion(region, animated = true)
        }
    }

    UIKitView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            val oldAnnotations = view.annotations
            if (oldAnnotations.isNotEmpty()) {
                view.removeAnnotations(oldAnnotations)
            }
            val oldOverlays = view.overlays
            if (oldOverlays.isNotEmpty()) {
                view.removeOverlays(oldOverlays)
            }

            // 1. Draw Branch Geofence Radius & Annotation
            if (branchLatitude != null && branchLongitude != null && branchLatitude != 0.0 && branchLongitude != 0.0) {
                val branchCoord = CLLocationCoordinate2DMake(branchLatitude, branchLongitude)
                val circle = MKCircle.circleWithCenterCoordinate(branchCoord, radius)
                view.addOverlay(circle)

                val branchPin = MKPointAnnotation().apply {
                    setCoordinate(branchCoord)
                    setTitle("Kantor / Cabang")
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
