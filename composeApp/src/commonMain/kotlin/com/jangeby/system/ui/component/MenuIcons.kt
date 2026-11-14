package com.jangeby.system.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Clock face for lateness monitoring. */
val LatenessIcon: ImageVector by lazy {
    ImageVector.Builder("Lateness", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(12f, 4f)
            curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
            curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
            curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
            curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
            close()
            moveTo(11f, 6f)
            lineTo(13f, 6f)
            lineTo(13f, 11.4f)
            lineTo(16.7f, 13.6f)
            lineTo(15.7f, 15.3f)
            lineTo(11f, 12.5f)
            close()
        }
    }.build()
}

/** Banknote for employee cash advances. */
val MoneyIcon: ImageVector by lazy {
    ImageVector.Builder("Money", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(2f, 5f)
            lineTo(22f, 5f)
            lineTo(22f, 19f)
            lineTo(2f, 19f)
            close()
            moveTo(4f, 7f)
            lineTo(4f, 17f)
            lineTo(20f, 17f)
            lineTo(20f, 7f)
            close()
            moveTo(12f, 8f)
            curveTo(14.2f, 8f, 16f, 9.8f, 16f, 12f)
            curveTo(16f, 14.2f, 14.2f, 16f, 12f, 16f)
            curveTo(9.8f, 16f, 8f, 14.2f, 8f, 12f)
            curveTo(8f, 9.8f, 9.8f, 8f, 12f, 8f)
            close()
            moveTo(5f, 10f)
            lineTo(7f, 10f)
            lineTo(7f, 14f)
            lineTo(5f, 14f)
            close()
            moveTo(17f, 10f)
            lineTo(19f, 10f)
            lineTo(19f, 14f)
            lineTo(17f, 14f)
            close()
        }
    }.build()
}
