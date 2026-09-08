package com.electrobinoculars.app.ui.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import com.electrobinoculars.app.data.VisionMode
import com.electrobinoculars.app.ui.theme.AmberDark
import com.electrobinoculars.app.ui.theme.AmberPrimary
import com.electrobinoculars.app.ui.theme.VisorBorder
import com.electrobinoculars.app.ui.theme.VisorPanelBg

private val SelectorPanelShape = RoundedCornerShape(8.dp)
private val SelectorPanelBorder = BorderStroke(1.5.dp, AmberPrimary)
private val ModeButtonShape = RoundedCornerShape(4.dp)
private val ModeButtonSelectedBorder = BorderStroke(2.dp, AmberPrimary)
private val ModeButtonUnselectedBorder = BorderStroke(1.dp, VisorBorder)

/**
 * 4-Button Tactical Vision Mode Selector Strip.
 * Mounted on the left edge of the landscape visor to allow instant thumb-switching of optical modes.
 */
@Composable
fun VisionModeSelector(
    modifier: Modifier = Modifier,
    currentMode: VisionMode,
    onModeSelected: (VisionMode) -> Unit
) {
    Box(
        modifier = modifier
            .background(VisorPanelBg, SelectorPanelShape)
            .border(SelectorPanelBorder, SelectorPanelShape)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SPECTRA",
                color = AmberDark,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            VisionMode.entries.forEach { mode ->
                val isSelected = mode == currentMode

                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(38.dp)
                        .background(
                            color = if (isSelected) AmberPrimary else AmberDark,
                            shape = ModeButtonShape
                        )
                        .border(
                            border = if (isSelected) ModeButtonSelectedBorder else ModeButtonUnselectedBorder,
                            shape = ModeButtonShape
                        )
                        .clickable { onModeSelected(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = mode.code,
                            color = if (isSelected) Color.Black else AmberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .width(16.dp)
                                    .height(2.dp)
                                    .background(Color.Black)
                            )
                        }
                    }
                }
            }
        }
    }
}
