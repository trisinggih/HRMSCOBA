package com.jangeby.system.hardware

import androidx.compose.ui.geometry.Offset

expect object SignatureExporter {
    fun exportToPngBase64(
        strokes: List<List<Offset>>,
        width: Int,
        height: Int,
        strokeWidth: Float = 5f,
        strokeColor: Long = 0xFF0F172A
    ): String?
}
