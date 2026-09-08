package com.electrobinoculars.app.ui.shaders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import java.util.Random

private const val NUM_GRAIN_FRAMES = 8

/**
 * Procedural animated phosphor grain / sensor noise for Night Vision mode.
 * Optimized with a precomputed cyclic noise buffer and batched GPU [drawPoints] calls.
 */
@Composable
fun GrainOverlay(
    modifier: Modifier = Modifier,
    grainDensity: Int = 180,
    grainColor: Color = Color(0xFF00FF66).copy(alpha = 0.12f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GrainNoiseTransition")
    val frameIndex by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = NUM_GRAIN_FRAMES.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GrainFrameIndex"
    )

    // Pre-generate normalized (0..1) random point patterns once
    val normalizedFrames = remember(grainDensity) {
        val random = Random(424242L)
        Array(NUM_GRAIN_FRAMES) {
            FloatArray(grainDensity * 2) {
                random.nextFloat()
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height

                // Cache absolute Offset lists mapped to the viewport dimensions
                val cachedFrames: List<List<Offset>> = normalizedFrames.map { rawCoords ->
                    val list = ArrayList<Offset>(grainDensity)
                    for (i in 0 until grainDensity) {
                        val nx = rawCoords[i * 2]
                        val ny = rawCoords[i * 2 + 1]
                        list.add(Offset(nx * w, ny * h))
                    }
                    list
                }

                val strokeWidthPx = 2.2f

                onDrawBehind {
                    val activeFrame = (frameIndex.toInt()).coerceIn(0, NUM_GRAIN_FRAMES - 1)
                    drawPoints(
                        points = cachedFrames[activeFrame],
                        pointMode = PointMode.Points,
                        color = grainColor,
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }
    ) {}
}
