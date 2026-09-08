package com.electrobinoculars.app.ui.viewfinder

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.electrobinoculars.app.ui.theme.AmberDark
import com.electrobinoculars.app.ui.theme.AmberGlow
import com.electrobinoculars.app.ui.theme.AmberPrimary
import com.electrobinoculars.app.ui.theme.AmberSubtle
import com.electrobinoculars.app.ui.theme.VisorBlack
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * High-fidelity animated sci-fi synthetic calibration grid test scene.
 *
 * Renders an authentic Star Wars-inspired electrobinocular diagnostic calibration matrix
 * using procedural vector graphics in Jetpack Compose [Canvas].
 *
 * Designed to operate smoothly at 60 FPS in emulator environments, when camera hardware is
 * absent, or while camera permissions are pending/denied.
 *
 * @param modifier Composable layout modifier.
 * @param simulatedZoomRatio Current zoom magnification ratio that scales the grid vectors dynamically.
 * @param showEngagePermissionButton Whether to show the [ ENGAGE OPTICAL SENSOR ] button.
 * @param onRequestPermission Callback triggered when the engage sensor button is tapped.
 */
@Composable
fun SyntheticCalibrationGrid(
    modifier: Modifier = Modifier,
    simulatedZoomRatio: Float = 1.0f,
    showEngagePermissionButton: Boolean = false,
    onRequestPermission: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SyntheticGridTransition")

    // Outer azimuth calibration ring rotation (clockwise)
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRingRotation"
    )

    // Inner reticle calibration ring counter-rotation (counter-clockwise)
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerRingRotation"
    )

    // Continuous vertical laser scanline sweep (top to bottom)
    val scanlineYProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanlineProgress"
    )

    // Subtle radar ping / reticle breathing pulse
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseProgress"
    )

    val amberPrimary = AmberPrimary
    val amberBright = AmberGlow
    val amberDark = AmberDark
    val amberFaint = AmberSubtle
    val backgroundDark = VisorBlack

    // Pre-allocated reusable drawing styles to avoid per-frame GC churn
    val stroke1Px = remember { Stroke(width = 1f) }
    val stroke1_2Px = remember { Stroke(width = 1.2f) }
    val stroke1_5Px = remember { Stroke(width = 1.5f) }
    val stroke1_6Px = remember { Stroke(width = 1.6f) }
    val stroke1_8Px = remember { Stroke(width = 1.8f) }
    val stroke2_5Px = remember { Stroke(width = 2.5f) }
    val scanlineColors = remember { listOf(Color.Transparent, Color(0x2EFFA500)) }

    // Reusable paths to avoid per-frame allocations
    val gridPath = remember { Path() }
    val cardinalTicksPath = remember { Path() }
    val majorTicksPath = remember { Path() }
    val minorTicksPath = remember { Path() }
    val crosshairPath = remember { Path() }
    val milTicksPath = remember { Path() }
    val spectrumPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundDark)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cx = width / 2f
            val cy = height / 2f
            val centerOffset = Offset(cx, cy)

            // 1. DYNAMICALLY SCALED TACTICAL GRID LAYER (scales with zoom ratio)
            val safeZoom = if (simulatedZoomRatio.isNaN() || simulatedZoomRatio <= 0f) 1.0f else simulatedZoomRatio
            withTransform({
                scale(
                    scaleX = safeZoom,
                    scaleY = safeZoom,
                    pivot = centerOffset
                )
            }) {
                // Coordinate grid lines (Batched into a single Path)
                val gridStep = 54.dp.toPx()
                gridPath.reset()
                var gx = 0f
                while (gx <= width) {
                    gridPath.moveTo(gx, 0f)
                    gridPath.lineTo(gx, height)
                    gx += gridStep
                }
                var gy = 0f
                while (gy <= height) {
                    gridPath.moveTo(0f, gy)
                    gridPath.lineTo(width, gy)
                    gy += gridStep
                }
                drawPath(gridPath, amberFaint, style = stroke1Px)

                // Range concentric metric circles
                val radiusStep = 70.dp.toPx()
                for (rIdx in 1..4) {
                    drawCircle(
                        color = amberFaint,
                        radius = radiusStep * rIdx,
                        center = centerOffset,
                        style = stroke1Px
                    )
                }

                // Outer rotating azimuth calibration ring with angle ticks
                val outerRadius = 150.dp.toPx()
                withTransform({
                    rotate(outerRotation, pivot = centerOffset)
                }) {
                    drawCircle(
                        color = amberDark,
                        radius = outerRadius,
                        center = centerOffset,
                        style = stroke1_8Px
                    )

                    // Pre-build tick paths relative to centerOffset
                    cardinalTicksPath.reset()
                    majorTicksPath.reset()
                    minorTicksPath.reset()

                    for (i in 0 until 36) {
                        val angle = (i * 10) * (PI / 180f).toFloat()
                        val isCardinal = (i % 9 == 0)
                        val isMajor = (i % 3 == 0)
                        val tickLength = when {
                            isCardinal -> 16f
                            isMajor -> 10f
                            else -> 6f
                        }
                        val cosA = cos(angle)
                        val sinA = sin(angle)
                        val startX = cx + (outerRadius - tickLength) * cosA
                        val startY = cy + (outerRadius - tickLength) * sinA
                        val endX = cx + outerRadius * cosA
                        val endY = cy + outerRadius * sinA

                        when {
                            isCardinal -> {
                                cardinalTicksPath.moveTo(startX, startY)
                                cardinalTicksPath.lineTo(endX, endY)
                            }
                            isMajor -> {
                                majorTicksPath.moveTo(startX, startY)
                                majorTicksPath.lineTo(endX, endY)
                            }
                            else -> {
                                minorTicksPath.moveTo(startX, startY)
                                minorTicksPath.lineTo(endX, endY)
                            }
                        }
                    }

                    drawPath(cardinalTicksPath, amberPrimary, style = stroke2_5Px)
                    drawPath(majorTicksPath, amberPrimary.copy(alpha = 0.7f), style = stroke1_2Px)
                    drawPath(minorTicksPath, amberDark, style = stroke1_2Px)
                }

                // Inner counter-rotating segmented reticle ring
                val innerRadius = 92.dp.toPx()
                val arcTopLeft = Offset(cx - innerRadius, cy - innerRadius)
                val arcSize = Size(innerRadius * 2, innerRadius * 2)
                withTransform({
                    rotate(innerRotation, pivot = centerOffset)
                }) {
                    val segmentSweep = 50f
                    val gap = 40f
                    for (segmentIdx in 0 until 4) {
                        val startAngle = segmentIdx * (segmentSweep + gap)
                        drawArc(
                            color = amberPrimary.copy(alpha = 0.85f),
                            startAngle = startAngle,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = stroke2_5Px
                        )
                    }
                }

                // Pulsating radar ping circle
                val pulseRadius = 55.dp.toPx() + (pulseProgress * 30.dp.toPx())
                drawCircle(
                    color = amberPrimary.copy(alpha = (1.0f - pulseProgress) * 0.45f),
                    radius = pulseRadius,
                    center = centerOffset,
                    style = stroke1_5Px
                )

                // Central precision crosshair with sub-millimeter target brackets (Batched Path)
                val crosshairArm = 44.dp.toPx()
                val crosshairGap = 8.dp.toPx()
                crosshairPath.reset()
                crosshairPath.moveTo(cx - crosshairArm, cy); crosshairPath.lineTo(cx - crosshairGap, cy)
                crosshairPath.moveTo(cx + crosshairGap, cy); crosshairPath.lineTo(cx + crosshairArm, cy)
                crosshairPath.moveTo(cx, cy - crosshairArm); crosshairPath.lineTo(cx, cy - crosshairGap)
                crosshairPath.moveTo(cx, cy + crosshairGap); crosshairPath.lineTo(cx, cy + crosshairArm)
                drawPath(crosshairPath, amberPrimary, style = stroke1_6Px)

                // Center diamond target pip (Zero-allocation rotated square)
                val pipSize = 5.dp.toPx()
                val halfSide = pipSize * 0.70710677f
                rotate(degrees = 45f, pivot = centerOffset) {
                    drawRect(
                        color = amberBright,
                        topLeft = Offset(cx - halfSide, cy - halfSide),
                        size = Size(halfSide * 2f, halfSide * 2f)
                    )
                }

                // Stadiametric mil-hash ticks along horizontal crosshair (Batched Path)
                val milStep = 14.dp.toPx()
                val milBaseOffset = 10.dp.toPx()
                val tickHMajor = 4.dp.toPx()
                val tickHMinor = 2.5.dp.toPx()
                milTicksPath.reset()
                for (mil in 1..3) {
                    val currentTickHalf = if (mil % 2 == 0) tickHMajor else tickHMinor
                    val dist = mil * milStep + milBaseOffset
                    // Left
                    milTicksPath.moveTo(cx - dist, cy - currentTickHalf)
                    milTicksPath.lineTo(cx - dist, cy + currentTickHalf)
                    // Right
                    milTicksPath.moveTo(cx + dist, cy - currentTickHalf)
                    milTicksPath.lineTo(cx + dist, cy + currentTickHalf)
                }
                drawPath(milTicksPath, amberPrimary.copy(alpha = 0.7f), style = stroke1_2Px)

                // Simulated tactical ground/target calibration markers
                val targetAlphaOffset = Offset(cx + 130.dp.toPx(), cy - 65.dp.toPx())
                drawCircle(
                    color = amberBright.copy(alpha = 0.75f),
                    radius = 4.dp.toPx(),
                    center = targetAlphaOffset,
                    style = stroke1_5Px
                )
                drawLine(
                    color = amberDark,
                    start = Offset(targetAlphaOffset.x + 6.dp.toPx(), targetAlphaOffset.y),
                    end = Offset(targetAlphaOffset.x + 28.dp.toPx(), targetAlphaOffset.y),
                    strokeWidth = 1f
                )

                val targetBetaOffset = Offset(cx - 145.dp.toPx(), cy + 70.dp.toPx())
                drawCircle(
                    color = amberBright.copy(alpha = 0.75f),
                    radius = 4.dp.toPx(),
                    center = targetBetaOffset,
                    style = stroke1_5Px
                )
                drawLine(
                    color = amberDark,
                    start = Offset(targetBetaOffset.x - 6.dp.toPx(), targetBetaOffset.y),
                    end = Offset(targetBetaOffset.x - 28.dp.toPx(), targetBetaOffset.y),
                    strokeWidth = 1f
                )
            }

            // 2. NON-SCALING PROCEDURAL LASER SWEEP
            val scanY = height * scanlineYProgress
            drawLine(
                color = amberPrimary.copy(alpha = 0.85f),
                start = Offset(0f, scanY),
                end = Offset(width, scanY),
                strokeWidth = 2f
            )
            val fadeHeight = 70.dp.toPx()
            val gradientStart = max(0f, scanY - fadeHeight)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = scanlineColors,
                    startY = gradientStart,
                    endY = scanY
                ),
                topLeft = Offset(0f, gradientStart),
                size = Size(width, max(0f, min(scanY, fadeHeight)))
            )

            // 3. NON-SCALING CARRIER FREQUENCY SPECTRUM ANALYZER (Bottom Left)
            val barCount = 18
            val barWidth = 5.dp.toPx()
            val barSpacing = 3.dp.toPx()
            val startX = 36.dp.toPx()
            val baseY = height - 32.dp.toPx()
            val maxWaveHeight = 36.dp.toPx()
            val minWaveHeight = 4.dp.toPx()

            spectrumPath.reset()
            for (i in 0 until barCount) {
                // Harmonic sine wave oscillation simulating live sensor telemetry reception
                val harmonic = sin((outerRotation * (PI / 180f) * 2.2f) + (i * 0.45f)).toFloat()
                val waveHeight = (harmonic * 0.5f + 0.5f) * maxWaveHeight + minWaveHeight
                val xBar = startX + i * (barWidth + barSpacing)
                spectrumPath.addRect(
                    androidx.compose.ui.geometry.Rect(
                        left = xBar,
                        top = baseY - waveHeight,
                        right = xBar + barWidth,
                        bottom = baseY
                    )
                )
            }
            drawPath(spectrumPath, amberPrimary.copy(alpha = 0.65f))
        }

        // Diagnostic tactical telemetry status banners
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "OPTICAL SENSOR OFFLINE // SYNTHETIC DIAGNOSTIC ACTIVE",
                color = amberPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "CARRIER FREQ: 1420.405 MHz  |  SYNC: NOMINAL  |  GRID: ACTIVE",
                color = amberPrimary.copy(alpha = 0.72f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Bottom engage camera permission button if needed
        if (showEngagePermissionButton) {
            OutlinedButton(
                onClick = onRequestPermission,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = amberPrimary
                ),
                border = BorderStroke(1.5.dp, amberPrimary)
            ) {
                Text(
                    text = "[ ENGAGE OPTICAL SENSOR ]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
