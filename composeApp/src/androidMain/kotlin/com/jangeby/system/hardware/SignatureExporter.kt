package com.jangeby.system.hardware

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import java.io.ByteArrayOutputStream

actual object SignatureExporter {
    actual fun exportToPngBase64(
        strokes: List<List<Offset>>,
        width: Int,
        height: Int,
        strokeWidth: Float,
        strokeColor: Long
    ): String? {
        if (width <= 0 || height <= 0 || strokes.isEmpty()) return null
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)

            val paint = Paint().apply {
                color = strokeColor.toInt()
                this.strokeWidth = strokeWidth
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            for (stroke in strokes) {
                if (stroke.isEmpty()) continue
                if (stroke.size == 1) {
                    canvas.drawCircle(stroke[0].x, stroke[0].y, strokeWidth / 2f, Paint().apply {
                        color = strokeColor.toInt()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    })
                    continue
                }
                val path = Path()
                path.moveTo(stroke[0].x, stroke[0].y)
                for (i in 1 until stroke.size) {
                    path.lineTo(stroke[i].x, stroke[i].y)
                }
                canvas.drawPath(path, paint)
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
