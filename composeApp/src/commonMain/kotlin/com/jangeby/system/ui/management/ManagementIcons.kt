package com.jangeby.system.ui.management

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ManagementDashboardIcon: ImageVector by lazy {
    ImageVector.Builder("ManagementDashboard", 24.dp, 24.dp, 24f, 24f).path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 13f); lineTo(7f, 13f); lineTo(7f, 21f); lineTo(3f, 21f); close()
        moveTo(10f, 3f); lineTo(14f, 3f); lineTo(14f, 21f); lineTo(10f, 21f); close()
        moveTo(17f, 8f); lineTo(21f, 8f); lineTo(21f, 21f); lineTo(17f, 21f); close()
    }.build()
}
val ManagementReportIcon: ImageVector by lazy {
    ImageVector.Builder("ManagementReport", 24.dp, 24.dp, 24f, 24f).path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 2f); lineTo(16f, 2f); lineTo(21f, 7f); lineTo(21f, 22f); lineTo(4f, 22f); close()
        moveTo(7f, 6f); lineTo(7f, 8f); lineTo(13f, 8f); lineTo(13f, 6f); close()
        moveTo(7f, 11f); lineTo(7f, 13f); lineTo(18f, 13f); lineTo(18f, 11f); close()
        moveTo(7f, 16f); lineTo(7f, 18f); lineTo(18f, 18f); lineTo(18f, 16f); close()
    }.build()
}
