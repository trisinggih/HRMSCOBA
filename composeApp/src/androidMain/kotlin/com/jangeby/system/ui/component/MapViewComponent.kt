package com.jangeby.system.ui.component

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.5)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Reposition camera whenever coordinates change or refresh / recenter is triggered
    LaunchedEffect(latitude, longitude, recenterTrigger) {
        if (latitude != 0.0 || longitude != 0.0) {
            val targetPoint = GeoPoint(latitude, longitude)
            mapView.controller.animateTo(targetPoint)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    ) { view ->
        val userPoint = GeoPoint(latitude, longitude)
        if (latitude != 0.0 || longitude != 0.0) {
            view.controller.setCenter(userPoint)
        }
        
        view.overlays.clear()

        // 1. Draw All Branch Radius Circles & Markers
        val branchesToDraw = if (branches.isNotEmpty()) {
            branches
        } else if (branchLatitude != null && branchLongitude != null && branchLatitude != 0.0 && branchLongitude != 0.0) {
            listOf(BranchItemData(branchLatitude, branchLongitude, radius, "Lokasi Kantor Cabang"))
        } else {
            emptyList()
        }

        for (b in branchesToDraw) {
            if (b.latitude == 0.0 || b.longitude == 0.0) continue
            val branchPoint = GeoPoint(b.latitude, b.longitude)
            
            val circlePolygon = Polygon(view).apply {
                points = Polygon.pointsAsCircle(branchPoint, b.radius)
                fillColor = android.graphics.Color.parseColor("#44008779")
                strokeColor = android.graphics.Color.parseColor("#FF008779")
                strokeWidth = 4f
            }
            view.overlays.add(circlePolygon)
        }

        // 2. Draw User Location Marker
        if (latitude != 0.0 || longitude != 0.0) {
            val userMarker = Marker(view).apply {
                position = userPoint
                title = "Lokasi Anda"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            view.overlays.add(userMarker)
        }

        view.invalidate()
    }
}
