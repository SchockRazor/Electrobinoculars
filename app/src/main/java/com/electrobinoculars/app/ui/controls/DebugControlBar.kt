package com.electrobinoculars.app.ui.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrobinoculars.app.data.SensorTelemetry
import com.electrobinoculars.app.data.ZoomStateData
import com.electrobinoculars.app.ui.theme.AmberAccent
import com.electrobinoculars.app.ui.theme.AmberPrimary
import com.electrobinoculars.app.ui.theme.TacticalCyan
import com.electrobinoculars.app.ui.theme.TacticalGreen
import com.electrobinoculars.app.ui.theme.TacticalRed
import com.electrobinoculars.app.ui.theme.VisorPanelBg

/**
 * Tactical Debug Control Bar.
 *
 * Rendered at app launch when the main UI is hidden. Provides direct buttons
 * to test core functionality:
 * 1. Zooming (adjust magnification ratio & CameraControl binding).
 * 2. Distance (adjust inclination pitch to dynamically compute range in meters).
 * 3. Compass heading (adjust azimuth degrees & cardinal bearing).
 * 4. Master UI Toggle (show/hide the Imperial Amber tactical HUD overlay).
 */
@Composable
fun DebugControlBar(
    modifier: Modifier = Modifier,
    isUiVisible: Boolean,
    onToggleUiVisibility: () -> Unit,
    zoomState: ZoomStateData,
    onZoomStep: (Float) -> Unit,
    onSetZoomRatio: (Float) -> Unit,
    telemetry: SensorTelemetry,
    onAdjustHeading: (Float) -> Unit,
    onSetHeading: (Float) -> Unit,
    onAdjustPitch: (Float) -> Unit,
    onSetPitch: (Float) -> Unit,
    onResetSensors: () -> Unit,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    isFallbackActive: Boolean
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP STRIP: Master UI Toggle + Collapse Bar + Permission Button
        Row(
            modifier = Modifier
                .background(VisorPanelBg, RoundedCornerShape(6.dp))
                .border(BorderStroke(1.dp, if (isUiVisible) AmberPrimary else TacticalCyan), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Master HUD Toggle Button
            DebugButton(
                label = if (isUiVisible) "▲ HIDE HUD (UI ON)" else "▼ SHOW HUD (UI OFF)",
                color = if (isUiVisible) AmberPrimary else TacticalCyan,
                isHighlighted = true,
                onClick = onToggleUiVisibility
            )

            // Feed status
            Box(
                modifier = Modifier
                    .background(if (isFallbackActive) AmberPrimary.copy(alpha = 0.2f) else TacticalGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .border(1.dp, if (isFallbackActive) AmberPrimary else TacticalGreen, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isFallbackActive) "FEED: SYNTHETIC SIM" else "FEED: LIVE CAMERA",
                    color = if (isFallbackActive) AmberPrimary else TacticalGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Camera permission button if needed
            if (!hasCameraPermission) {
                DebugButton(
                    label = "GRANT CAMERA ACCESS",
                    color = TacticalRed,
                    isHighlighted = true,
                    onClick = onRequestCameraPermission
                )
            }

            // Sensor reset button
            DebugButton(
                label = "RESET SENSORS",
                color = Color.White.copy(alpha = 0.8f),
                onClick = onResetSensors
            )

            // Panel expand / collapse toggle
            Box(
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isExpanded) "[HIDE DEBUG CONTROLS ▲]" else "[SHOW DEBUG CONTROLS ▼]",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // EXPANDABLE TEST CONTROLS PANEL
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(VisorPanelBg, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, TacticalCyan.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ROW 1: ZOOMING TEST CONTROLS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GroupHeader(title = "ZOOM", value = zoomState.formattedMagnification, color = AmberPrimary)

                    DebugButton(label = " -0.5x ", color = AmberPrimary) { onZoomStep(-0.5f) }
                    DebugButton(label = " +0.5x ", color = AmberPrimary) { onZoomStep(0.5f) }

                    Spacer(modifier = Modifier.width(4.dp))
                    DebugButton(label = "1.0x", color = AmberAccent) { onSetZoomRatio(1.0f) }
                    DebugButton(label = "2.0x", color = AmberAccent) { onSetZoomRatio(2.0f) }
                    DebugButton(label = "4.0x", color = AmberAccent) { onSetZoomRatio(4.0f) }
                    DebugButton(label = "8.0x", color = AmberAccent) { onSetZoomRatio(8.0f) }
                }

                // ROW 2: COMPASS HEADING TEST CONTROLS ("The compass thingy")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val safeHdg = ((telemetry.headingDegrees % 360f) + 360f) % 360f
                    GroupHeader(
                        title = "COMPASS",
                        value = "${safeHdg.toInt().toString().padStart(3, '0')}° ${telemetry.cardinalDirection}",
                        color = TacticalCyan
                    )

                    DebugButton(label = " -15° ", color = TacticalCyan) { onAdjustHeading(-15f) }
                    DebugButton(label = " +15° ", color = TacticalCyan) { onAdjustHeading(15f) }

                    Spacer(modifier = Modifier.width(4.dp))
                    DebugButton(label = "0° N", color = TacticalCyan) { onSetHeading(0f) }
                    DebugButton(label = "90° E", color = TacticalCyan) { onSetHeading(90f) }
                    DebugButton(label = "180° S", color = TacticalCyan) { onSetHeading(180f) }
                    DebugButton(label = "270° W", color = TacticalCyan) { onSetHeading(270f) }
                }

                // ROW 3: DISTANCE & TILT TEST CONTROLS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pitchStr = "${if (telemetry.pitch >= 0) "+" else ""}${telemetry.pitch.toInt()}°"
                    GroupHeader(
                        title = "DISTANCE",
                        value = "${telemetry.estimatedDistanceMeters}m (PIT: $pitchStr)",
                        color = TacticalGreen
                    )

                    DebugButton(label = " Tilt -5° ", color = TacticalGreen) { onAdjustPitch(-5f) }
                    DebugButton(label = " Tilt +5° ", color = TacticalGreen) { onAdjustPitch(5f) }

                    Spacer(modifier = Modifier.width(4.dp))
                    DebugButton(label = "Level (0°)", color = TacticalGreen) { onSetPitch(0f) }
                    DebugButton(label = "Ground (-15°)", color = TacticalGreen) { onSetPitch(-15f) }
                    DebugButton(label = "Sky (+25°)", color = TacticalGreen) { onSetPitch(25f) }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    title: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title: ",
            color = Color.LightGray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun DebugButton(
    label: String,
    color: Color,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isHighlighted) color.copy(alpha = 0.25f) else color.copy(alpha = 0.08f),
                RoundedCornerShape(4.dp)
            )
            .border(
                BorderStroke(if (isHighlighted) 1.5.dp else 1.dp, color),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
