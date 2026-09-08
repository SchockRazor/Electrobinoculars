package com.electrobinoculars.app.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.electrobinoculars.app.ui.theme.AmberDark
import com.electrobinoculars.app.ui.theme.AmberPrimary

private val RUNG_ANGLES = intArrayOf(-30, -20, -10, 10, 20, 30)

/**
 * Artificial Horizon Pitch and Roll Ladder.
 *
 * Translates vertically based on device pitch tilt angle and rotates based on roll angle,
 * displaying tactical rungs with stadiametric angle indicators.
 */
@Composable
fun HorizonLadder(
    modifier: Modifier = Modifier,
    pitchDegrees: Float = 0.0f,
    rollDegrees: Float = 0.0f,
    primaryColor: Color = AmberPrimary,
    subtleColor: Color = AmberDark
) {
    // Sanitize sensor inputs against NaN / Infinity to protect Canvas transforms
    val safePitch = if (pitchDegrees.isNaN() || pitchDegrees.isInfinite()) 0.0f else pitchDegrees
    val safeRoll = if (rollDegrees.isNaN() || rollDegrees.isInfinite()) 0.0f else rollDegrees

    // Pre-allocated reusable paths to achieve zero heap allocations on high-frequency sensor updates
    val waterlinePath = remember { Path() }
    val rungsPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)

        // Pixels per degree of pitch tilt
        val pxPerDegree = 4.dp.toPx()
        val pitchOffsetPx = (safePitch * pxPerDegree).coerceIn(-size.height * 0.4f, size.height * 0.4f)

        // Rotate around center based on roll
        rotate(degrees = -safeRoll, pivot = center) {
            val ladderCenterY = cy + pitchOffsetPx

            // 1. Center Waterline Horizon Rung (0 degrees)
            val waterlineRungHalfWidth = 70.dp.toPx()
            val waterlineGap = 28.dp.toPx()
            val waterlineHook = 6.dp.toPx()
            val waterlineStrokePx = 2.dp.toPx()

            waterlinePath.reset()
            // Left waterline wing & downward hook
            waterlinePath.moveTo(cx - waterlineRungHalfWidth, ladderCenterY + waterlineHook)
            waterlinePath.lineTo(cx - waterlineRungHalfWidth, ladderCenterY)
            waterlinePath.lineTo(cx - waterlineGap, ladderCenterY)

            // Right waterline wing & downward hook
            waterlinePath.moveTo(cx + waterlineGap, ladderCenterY)
            waterlinePath.lineTo(cx + waterlineRungHalfWidth, ladderCenterY)
            waterlinePath.lineTo(cx + waterlineRungHalfWidth, ladderCenterY + waterlineHook)

            drawPath(
                path = waterlinePath,
                color = primaryColor,
                style = Stroke(width = waterlineStrokePx)
            )

            // 2. Pitch Ladder Rungs (±10°, ±20°, ±30°)
            val rungHalfWidth = 45.dp.toPx()
            val rungGap = 24.dp.toPx()
            val rungHook = 5.dp.toPx()
            val rungStrokePx = 1.5.dp.toPx()
            val viewportMinY = cy - size.height * 0.35f
            val viewportMaxY = cy + size.height * 0.35f

            rungsPath.reset()
            for (angle in RUNG_ANGLES) {
                val rungY = ladderCenterY - (angle * pxPerDegree)

                // Only accumulate if within visible vertical viewport
                if (rungY in viewportMinY..viewportMaxY) {
                    val hookDir = if (angle > 0) -rungHook else rungHook

                    // Left rung
                    rungsPath.moveTo(cx - rungHalfWidth, rungY + hookDir)
                    rungsPath.lineTo(cx - rungHalfWidth, rungY)
                    rungsPath.lineTo(cx - rungGap, rungY)

                    // Right rung
                    rungsPath.moveTo(cx + rungGap, rungY)
                    rungsPath.lineTo(cx + rungHalfWidth, rungY)
                    rungsPath.lineTo(cx + rungHalfWidth, rungY + hookDir)
                }
            }

            drawPath(
                path = rungsPath,
                color = subtleColor,
                style = Stroke(width = rungStrokePx)
            )
        }
    }
}
