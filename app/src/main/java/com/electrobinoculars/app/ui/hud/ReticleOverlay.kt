package com.electrobinoculars.app.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.electrobinoculars.app.ui.theme.AmberDark
import com.electrobinoculars.app.ui.theme.AmberGlow
import com.electrobinoculars.app.ui.theme.AmberPrimary

/**
 * Tactical Reticle Overlay.
 *
 * Renders an Imperial Amber central targeting reticle with:
 * - Central pipper dot and target gap
 * - Stadiametric mil-dot ticks along horizontal and vertical axes
 * - Outer target bracket ring
 * - Dynamic tick spacing responsive to zoom ratio
 */
@Composable
fun ReticleOverlay(
    modifier: Modifier = Modifier,
    zoomRatio: Float = 1.0f,
    primaryColor: Color = AmberPrimary,
    accentColor: Color = AmberGlow,
    subtleColor: Color = AmberDark
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val center = Offset(cx, cy)

                // Dynamic expansion factor based on digital zoom (sanitized against NaN / negative)
                val safeZoom = if (zoomRatio.isNaN() || zoomRatio < 1.0f) 1.0f else zoomRatio
                val zoomScale = (1.0f + (safeZoom - 1.0f) * 0.08f).coerceIn(1.0f, 1.6f)

                val centralGap = 16.dp.toPx() * zoomScale
                val armLength = 70.dp.toPx() * zoomScale
                val strokeWidth = 2.dp.toPx()
                val tickStrokeWidth = 1.5.dp.toPx()
                val tickHalfLen = 5.dp.toPx()
                val majorTickHalfLen = tickHalfLen * 1.4f
                val ringStrokeWidth = 1.5.dp.toPx()
                val pipperRadius = 2.dp.toPx()

                // Batched Crosshair Arms Path
                val reticleArmsPath = Path().apply {
                    // Left and right horizontal arms
                    moveTo(cx - armLength, cy)
                    lineTo(cx - centralGap, cy)
                    moveTo(cx + centralGap, cy)
                    lineTo(cx + armLength, cy)

                    // Top and bottom vertical arms
                    moveTo(cx, cy - armLength)
                    lineTo(cx, cy - centralGap)
                    moveTo(cx, cy + centralGap)
                    lineTo(cx, cy + armLength)
                }

                // Batched Stadiametric Mil-Dot Ticks Path
                val tickCount = 4
                val tickStep = (armLength - centralGap) / (tickCount + 1)
                val ticksPath = Path().apply {
                    for (i in 1..tickCount) {
                        val offset = centralGap + (i * tickStep)
                        val currentTickHalf = if (i % 2 == 0) majorTickHalfLen else tickHalfLen

                        // Horizontal arm ticks
                        moveTo(cx - offset, cy - currentTickHalf)
                        lineTo(cx - offset, cy + currentTickHalf)
                        moveTo(cx + offset, cy - currentTickHalf)
                        lineTo(cx + offset, cy + currentTickHalf)

                        // Vertical arm ticks
                        moveTo(cx - currentTickHalf, cy - offset)
                        lineTo(cx + currentTickHalf, cy - offset)
                        moveTo(cx - currentTickHalf, cy + offset)
                        lineTo(cx + currentTickHalf, cy + offset)
                    }
                }

                // Outer Segmented Target Bracket Ring
                val ringRadius = armLength * 0.9f

                // Diamond corner brackets outside ring
                val diamondDist = ringRadius + 14.dp.toPx()
                val bracketArm = 8.dp.toPx()
                val diamondTicksPath = Path().apply {
                    moveTo(cx, cy - diamondDist)
                    lineTo(cx, cy - diamondDist + bracketArm)
                    moveTo(cx, cy + diamondDist)
                    lineTo(cx, cy + diamondDist - bracketArm)
                    moveTo(cx - diamondDist, cy)
                    lineTo(cx - diamondDist + bracketArm, cy)
                    moveTo(cx + diamondDist, cy)
                    lineTo(cx + diamondDist - bracketArm, cy)
                }

                val crosshairStroke = Stroke(width = strokeWidth)
                val tickStroke = Stroke(width = tickStrokeWidth)
                val ringStroke = Stroke(width = ringStrokeWidth)

                onDrawBehind {
                    // 1. Center Pipper Dot
                    drawCircle(
                        color = accentColor,
                        radius = pipperRadius,
                        center = center
                    )

                    // 2. Crosshair Arms
                    drawPath(reticleArmsPath, primaryColor, style = crosshairStroke)

                    // 3. Mil-Dot Ticks
                    drawPath(ticksPath, primaryColor, style = tickStroke)

                    // 4. Outer Ring
                    drawCircle(
                        color = subtleColor,
                        radius = ringRadius,
                        center = center,
                        style = ringStroke
                    )

                    // 5. Diamond Corner Ticks
                    drawPath(diamondTicksPath, accentColor, style = crosshairStroke)
                }
            }
    ) {}
}
