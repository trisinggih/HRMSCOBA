package com.jangeby.system.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignatureCanvasComponent(
    modifier: Modifier = Modifier,
    strokes: List<List<Offset>>,
    onStrokeAdded: (List<Offset>) -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit = {}
) {
    val currentStroke = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                width = 1.5.dp,
                color = Color(0xFFCBD5E1),
                shape = RoundedCornerShape(16.dp)
            )
            .onGloballyPositioned { coordinates ->
                canvasSize = coordinates.size
                onCanvasSizeChanged(coordinates.size)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke.clear()
                            currentStroke.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStroke.add(change.position)
                        },
                        onDragEnd = {
                            if (currentStroke.isNotEmpty()) {
                                onStrokeAdded(currentStroke.toList())
                                currentStroke.clear()
                            }
                        },
                        onDragCancel = {
                            currentStroke.clear()
                        }
                    )
                }
        ) {
            // Garis panduan dasar tanda tangan (Baseline)
            val lineY = size.height * 0.78f
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(x = size.width * 0.08f, y = lineY),
                end = Offset(x = size.width * 0.92f, y = lineY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )

            // Gambar goresan yang sudah selesai
            val strokeColor = Color(0xFF0F172A)
            val strokeWidthPx = 4.5.dp.toPx()

            for (stroke in strokes) {
                if (stroke.isEmpty()) continue
                if (stroke.size == 1) {
                    drawCircle(
                        color = strokeColor,
                        radius = strokeWidthPx / 2f,
                        center = stroke[0]
                    )
                } else {
                    val path = Path().apply {
                        moveTo(stroke[0].x, stroke[0].y)
                        for (i in 1 until stroke.size) {
                            lineTo(stroke[i].x, stroke[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Gambar goresan yang sedang aktif digoreskan jari
            if (currentStroke.isNotEmpty()) {
                if (currentStroke.size == 1) {
                    drawCircle(
                        color = strokeColor,
                        radius = strokeWidthPx / 2f,
                        center = currentStroke[0]
                    )
                } else {
                    val path = Path().apply {
                        moveTo(currentStroke[0].x, currentStroke[0].y)
                        for (i in 1 until currentStroke.size) {
                            lineTo(currentStroke[i].x, currentStroke[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Teks petunjuk saat kanvas masih kosong
        if (strokes.isEmpty() && currentStroke.isEmpty()) {
            Text(
                text = "Goreskan tanda tangan Anda di area kotak ini",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
