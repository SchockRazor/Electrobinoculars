package com.electrobinoculars.app.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.electrobinoculars.app.ui.theme.AmberDark
import com.electrobinoculars.app.ui.theme.AmberPrimary

/**
 * Tactical Panoramic Visor Corner Brackets and Aperture Vignette Framing.
 * Renders authentic Imperial Amber corner brackets framing the landscape view.
 */
@Composable
fun CornerBrackets(
    modifier: Modifier = Modifier,
    bracketLengthDp: Float = 36f,
    strokeWidthDp: Float = 2.5f,
    bracketColor: Color = AmberPrimary,
    accentColor: Color = AmberDark,
    insetDp: Float = 16f
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val armLen = bracketLengthDp.dp.toPx()
                val stroke = strokeWidthDp.dp.toPx()
                val inset = insetDp.dp.toPx()
                val tickGap = 8.dp.toPx()
                val tickLen = armLen * 0.4f
                val tickStroke = stroke * 0.7f

                val left = inset
                val top = inset
                val right = size.width - inset
                val bottom = size.height - inset

                // Batched primary corner brackets path
                val bracketsPath = Path().apply {
                    // 1. Top-Left Bracket
                    moveTo(left, top + armLen)
                    lineTo(left, top)
                    lineTo(left + armLen, top)

                    // 2. Top-Right Bracket
                    moveTo(right - armLen, top)
                    lineTo(right, top)
                    lineTo(right, top + armLen)

                    // 3. Bottom-Left Bracket
                    moveTo(left, bottom - armLen)
                    lineTo(left, bottom)
                    lineTo(left + armLen, bottom)

                    // 4. Bottom-Right Bracket
                    moveTo(right - armLen, bottom)
                    lineTo(right, bottom)
                    lineTo(right, bottom - armLen)
                }

                // Batched secondary tick accents path
                val accentTicksPath = Path().apply {
                    // TL accent tick
                    moveTo(left + tickGap + tickLen, top + tickGap)
                    lineTo(left + tickGap, top + tickGap)
                    lineTo(left + tickGap, top + tickGap + tickLen)

                    // TR accent tick
                    moveTo(right - tickGap - tickLen, top + tickGap)
                    lineTo(right - tickGap, top + tickGap)
                    lineTo(right - tickGap, top + tickGap + tickLen)

                    // BL accent tick
                    moveTo(left + tickGap + tickLen, bottom - tickGap)
                    lineTo(left + tickGap, bottom - tickGap)
                    lineTo(left + tickGap, bottom - tickGap - tickLen)

                    // BR accent tick
                    moveTo(right - tickGap - tickLen, bottom - tickGap)
                    lineTo(right - tickGap, bottom - tickGap)
                    lineTo(right - tickGap, bottom - tickGap - tickLen)
                }

                val mainStrokeStyle = Stroke(width = stroke)
                val accentStrokeStyle = Stroke(width = tickStroke)

                onDrawBehind {
                    drawPath(bracketsPath, bracketColor, style = mainStrokeStyle)
                    drawPath(accentTicksPath, accentColor, style = accentStrokeStyle)
                }
            }
    ) {}
}
