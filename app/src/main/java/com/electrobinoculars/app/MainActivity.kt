package com.electrobinoculars.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.electrobinoculars.app.ui.MainScreen

/**
 * Main Activity for Electrobinoculars.
 *
 * Enforces edge-to-edge short-edge display cutout spanning, sticky immersive system bars,
 * screen keep-awake flags, and fixed sensorLandscape orientation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Edge-to-edge window framing
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Allow viewport to extend into camera pinhole / display cutout edges
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // 3. Immersive sticky system bars (transient swipe to reveal)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // 4. Keep screen illuminated during tactical observation
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 5. Host Compose UI
        setContent {
            MainScreen()
        }
    }
}
