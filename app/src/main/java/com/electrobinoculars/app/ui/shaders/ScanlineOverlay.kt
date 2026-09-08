package com.electrobinoculars.app.ui.shaders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache

/**
 * Procedural CRT scanline raster overlay for Tactical Monochrome mode.
 * Highly optimized with cached Skia Path geometry to eliminate per-frame allocations and loop iterations.
 */
@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    scanlineSpacingDp: Float = 4f,
    scanlineAlpha: Float = 0.25f
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val spacingPx = scanlineSpacingDp.dp.toPx()
                val scanlinePath = Path().apply {
                    var y = 0f
                    val h = size.height
                    val w = size.width
                    while (y <= h) {
                        moveTo(0f, y)
                        lineTo(w, y)
                        y += spacingPx
                    }
                }
                val strokeStyle = Stroke(width = 1.5f)
                val lineColor = Color.Black.copy(alpha = scanlineAlpha)

                onDrawBehind {
                    drawPath(scanlinePath, lineColor, style = strokeStyle)
                }
            }
    ) {}
}
