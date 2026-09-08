package com.electrobinoculars.app.ui.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrobinoculars.app.data.ZoomStateData
import com.electrobinoculars.app.ui.theme.AmberAccent
import com.electrobinoculars.app.ui.theme.AmberDark
import com.electrobinoculars.app.ui.theme.AmberPrimary
import com.electrobinoculars.app.ui.theme.VisorPanelBg

private val ZOOM_PRESETS = floatArrayOf(1.0f, 2.0f, 5.0f, 10.0f)
private val PanelShape = RoundedCornerShape(8.dp)
private val PanelBorder = BorderStroke(1.5.dp, AmberPrimary)
private val PresetShape = RoundedCornerShape(3.dp)
private val PresetBorder = BorderStroke(1.dp, AmberPrimary)

/**
 * Tactical Edge Zoom Slider and Stepped Preset Controller.
 *
 * Simulates a physical optical zoom wheel on the right edge of the electrobinocular chassis.
 */
@Composable
fun TacticalZoomSlider(
    modifier: Modifier = Modifier,
    zoomState: ZoomStateData,
    onLinearZoomChanged: (Float) -> Unit,
    onZoomRatioChanged: (Float) -> Unit
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = AmberAccent,
        activeTrackColor = AmberPrimary,
        inactiveTrackColor = AmberDark
    )

    Box(
        modifier = modifier
            .fillMaxHeight(0.75f)
            .background(VisorPanelBg, PanelShape)
            .border(PanelBorder, PanelShape)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            // Top Header: Magnification Readout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "OPTICAL",
                    color = AmberDark,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = zoomState.formattedMagnification,
                    color = AmberPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Preset Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZOOM_PRESETS.forEach { preset ->
                    val isNear = kotlin.math.abs(zoomState.zoomRatio - preset) < 0.3f
                    Box(
                        modifier = Modifier
                            .background(
                                if (isNear) AmberPrimary else AmberDark,
                                PresetShape
                            )
                            .border(
                                PresetBorder,
                                PresetShape
                            )
                            .clickable {
                                onZoomRatioChanged(preset)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${preset.toInt()}X",
                            color = if (isNear) Color.Black else AmberPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vertical Slider
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(42.dp),
                contentAlignment = Alignment.Center
            ) {
                val safeLinear = if (zoomState.linearZoom.isNaN()) 0.0f else zoomState.linearZoom.coerceIn(0f, 1f)
                Slider(
                    value = safeLinear,
                    onValueChange = { newLinear ->
                        val cleanLinear = if (newLinear.isNaN()) 0.0f else newLinear.coerceIn(0f, 1f)
                        onLinearZoomChanged(cleanLinear)
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = 270f
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                Constraints(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxWidth
                                )
                            )
                            layout(placeable.height, placeable.width) {
                                placeable.place(-placeable.width / 2 + placeable.height / 2, -placeable.height / 2 + placeable.width / 2)
                            }
                        },
                    colors = sliderColors
                )
            }

            // Bottom Label
            Text(
                text = "MAG-DIAL",
                color = AmberPrimary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
