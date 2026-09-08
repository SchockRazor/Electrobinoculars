package com.electrobinoculars.app.ui.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrobinoculars.app.data.SensorTelemetry
import com.electrobinoculars.app.data.VisionMode
import com.electrobinoculars.app.sensor.RangeConfidence
import com.electrobinoculars.app.data.ZoomStateData
import com.electrobinoculars.app.ui.theme.AmberAccent
import com.electrobinoculars.app.ui.theme.AmberDark
import com.electrobinoculars.app.ui.theme.AmberPrimary
import com.electrobinoculars.app.ui.theme.TacticalGreen
import com.electrobinoculars.app.ui.theme.VisorPanelBg

/**
 * Panoramic Tactical Visor HUD Overlay (Imperial Amber Vector Layer).
 *
 * Composites tactical corner brackets, artificial horizon ladder, central stadiametric reticle,
 * and top/bottom telemetry readouts into a unified sci-fi viewfinder.
 */
@Composable
fun TacticalVisorHud(
    modifier: Modifier = Modifier,
    telemetry: SensorTelemetry,
    zoomState: ZoomStateData,
    visionMode: VisionMode,
    isFallback: Boolean
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Panoramic Corner Brackets Framing
        CornerBrackets(
            modifier = Modifier.fillMaxSize(),
            bracketLengthDp = 44f,
            strokeWidthDp = 2.5f,
            bracketColor = AmberPrimary,
            accentColor = AmberDark,
            insetDp = 12f
        )

        // 2. Artificial Horizon Ladder (translates & rotates with device attitude)
        HorizonLadder(
            modifier = Modifier.fillMaxSize(),
            pitchDegrees = telemetry.pitch,
            rollDegrees = telemetry.roll,
            primaryColor = AmberPrimary,
            subtleColor = AmberDark
        )

        // 3. Central Targeting Reticle
        ReticleOverlay(
            modifier = Modifier.fillMaxSize(),
            zoomRatio = zoomState.zoomRatio,
            primaryColor = AmberPrimary,
            accentColor = AmberAccent,
            subtleColor = AmberDark
        )

        // 4. Perimeter Telemetry UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP TELEMETRY BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left: Vision Mode & Optical Status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudBadge {
                        Text(
                            text = "MODE: ${visionMode.code} // ${visionMode.label}",
                            color = AmberPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    HudBadge {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .background(
                                        if (isFallback) AmberPrimary else TacticalGreen,
                                        IndicatorCornerShape
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isFallback) "SIM" else "SYS",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = if (isFallback) "OPTICAL: SYNTHETIC" else "OPTICAL: ONLINE",
                                color = AmberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Top Center/Right: Compass Heading Tape & Attitude
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudBadge {
                        val safeHeading = if (telemetry.headingDegrees.isNaN() || telemetry.headingDegrees.isInfinite()) {
                            0f
                        } else {
                            ((telemetry.headingDegrees % 360f) + 360f) % 360f
                        }
                        val headingInt = safeHeading.toInt()
                        Text(
                            text = "HDG: ${headingInt.toString().padStart(3, '0')}° ${telemetry.cardinalDirection}",
                            color = AmberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }

                    HudBadge {
                        val safePitch = if (telemetry.pitch.isNaN() || telemetry.pitch.isInfinite()) 0f else telemetry.pitch
                        val safeRoll = if (telemetry.roll.isNaN() || telemetry.roll.isInfinite()) 0f else telemetry.roll
                        val pitchStr = "${if (safePitch >= 0) "+" else ""}${safePitch.toInt()}°"
                        val rollStr = "${if (safeRoll >= 0) "+" else ""}${safeRoll.toInt()}°"
                        Text(
                            text = "PIT: $pitchStr | ROL: $rollStr",
                            color = AmberPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // BOTTOM TELEMETRY BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom Left: Dynamic Rangefinder
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudBadge {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RNG: ",
                                color = AmberDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${telemetry.estimatedDistanceMeters}M",
                                color = AmberPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val confidenceColor = when (telemetry.rangeConfidence) {
                                RangeConfidence.HIGH -> AmberAccent
                                RangeConfidence.MEDIUM -> AmberPrimary
                                RangeConfidence.LOW -> AmberDark
                                RangeConfidence.NO_LOCK -> AmberDark.copy(alpha = 0.5f)
                            }
                            Text(
                                text = telemetry.rangeConfidence.label,
                                color = confidenceColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    HudBadge {
                        Text(
                            text = "OBS-H: 1.70M",
                            color = AmberPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bottom Right: Magnification Telemetry Readout
                HudBadge {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MAG: ",
                            color = AmberDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = zoomState.formattedMagnification,
                            color = AmberPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }
    }
}

private val HudBadgeCornerShape = RoundedCornerShape(4.dp)
private val HudBadgeBorderStroke = BorderStroke(1.dp, AmberPrimary)
private val IndicatorCornerShape = RoundedCornerShape(2.dp)

@Composable
private fun HudBadge(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(VisorPanelBg, HudBadgeCornerShape)
            .border(HudBadgeBorderStroke, HudBadgeCornerShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
