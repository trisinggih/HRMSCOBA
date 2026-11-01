package com.jangeby.system.hardware

import androidx.compose.ui.geometry.Offset
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*
import platform.CoreGraphics.*
import platform.Foundation.*

actual object SignatureExporter {
    @OptIn(ExperimentalForeignApi::class)
    actual fun exportToPngBase64(
        strokes: List<List<Offset>>,
        width: Int,
        height: Int,
        strokeWidth: Float,
        strokeColor: Long
    ): String? {
        if (width <= 0 || height <= 0 || strokes.isEmpty()) return null
        return try {
            val size = CGSizeMake(width.toDouble(), height.toDouble())
            UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
            val context = UIGraphicsGetCurrentContext() ?: return null

            for (stroke in strokes) {
                if (stroke.isEmpty()) continue
                val path = UIBezierPath()
                path.lineWidth = strokeWidth.toDouble()
                path.lineCapStyle = CGLineCap.kCGLineCapRound
                path.lineJoinStyle = CGLineJoin.kCGLineJoinRound

                path.moveToPoint(CGPointMake(stroke[0].x.toDouble(), stroke[0].y.toDouble()))
                for (i in 1 until stroke.size) {
                    path.addLineToPoint(CGPointMake(stroke[i].x.toDouble(), stroke[i].y.toDouble()))
                }
                UIColor.blackColor.setStroke()
                path.stroke()
            }

            val image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            if (image != null) {
                val pngData = UIImagePNGRepresentation(image)
                val base64 = pngData?.base64EncodedStringWithOptions(0UL)
                if (base64 != null) "data:image/png;base64,$base64" else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
