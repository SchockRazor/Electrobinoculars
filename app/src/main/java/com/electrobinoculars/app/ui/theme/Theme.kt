package com.electrobinoculars.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.Black,
    primaryContainer = AmberDark,
    onPrimaryContainer = AmberPrimary,
    secondary = AmberAccent,
    onSecondary = Color.Black,
    background = VisorBlack,
    onBackground = AmberPrimary,
    surface = VisorBlack,
    onSurface = AmberPrimary
)

@Composable
fun ElectrobinocularsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = TacticalTypography,
        content = content
    )
}
