package com.electrobinoculars.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.electrobinoculars.app.data.ElectrobinocularsUiState
import com.electrobinoculars.app.data.VisionMode
import com.electrobinoculars.app.data.ZoomStateData
import com.electrobinoculars.app.sensor.SensorTelemetryManager
import com.electrobinoculars.app.ui.controls.DebugControlBar
import com.electrobinoculars.app.ui.controls.TacticalZoomSlider
import com.electrobinoculars.app.ui.controls.VisionModeSelector
import com.electrobinoculars.app.ui.hud.TacticalVisorHud
import com.electrobinoculars.app.ui.shaders.GrainOverlay
import com.electrobinoculars.app.ui.shaders.ScanlineOverlay
import com.electrobinoculars.app.ui.shaders.VisionShaders
import com.electrobinoculars.app.ui.viewfinder.ViewfinderContainer

/**
 * Root Composable orchestrating the 3-Layer Viewport Architecture:
 * 1. Viewfinder Layer: Live CameraX preview or procedural synthetic calibration grid fallback,
 *    filtered in real-time by AGSL RuntimeShaders and ColorMatrices.
 * 2. Optical FX Layer: Procedural phosphor grain (NVG) and CRT scanlines (Tactical Mono).
 * 3. Tactical Visor HUD Layer: Pure Imperial Amber vector overlays, telemetry readouts,
 *    and tactile edge controls (unaffected by optical filters). Can be toggled on/off.
 * 4. Debug Control Bar: Shown at launch when UI is hidden to test zoom, distance, and compass.
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    forceFallback: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val initialPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    var uiState by remember {
        mutableStateOf(
            ElectrobinocularsUiState(
                isUiVisible = false,
                hasCameraPermission = initialPermission
            )
        )
    }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        uiState = uiState.copy(hasCameraPermission = isGranted)
    }

    // 1. Initialize Sensor Telemetry Manager with landscape attitude & rangefinder calculations
    val sensorManager = remember {
        SensorTelemetryManager(context) { newTelemetry ->
            uiState = uiState.copy(telemetry = newTelemetry)
        }
    }

    // Lifecycle observer for hardware sensors & permission synchronization
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    sensorManager.startListening()
                    val currentPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (currentPermission != uiState.hasCameraPermission) {
                        uiState = uiState.copy(hasCameraPermission = currentPermission)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.stopListening()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            sensorManager.stopListening()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun applyZoomRatio(newRatio: Float) {
        val cleanRatio = if (newRatio.isNaN() || newRatio < 1.0f) 1.0f else newRatio
        val clampedRatio = cleanRatio.coerceIn(
            uiState.zoomState.minZoomRatio,
            uiState.zoomState.maxZoomRatio
        )
        activeCamera?.cameraControl?.setZoomRatio(clampedRatio)
        val linear = ZoomStateData.calculateLinearZoom(
            clampedRatio,
            uiState.zoomState.minZoomRatio,
            uiState.zoomState.maxZoomRatio
        )
        uiState = uiState.copy(
            zoomState = uiState.zoomState.copy(
                zoomRatio = clampedRatio,
                linearZoom = linear
            )
        )
    }

    fun applyLinearZoom(newLinear: Float) {
        val cleanLinear = if (newLinear.isNaN()) 0.0f else newLinear.coerceIn(0f, 1f)
        activeCamera?.cameraControl?.setLinearZoom(cleanLinear)
        val ratio = ZoomStateData.calculateZoomRatio(
            cleanLinear,
            uiState.zoomState.minZoomRatio,
            uiState.zoomState.maxZoomRatio
        )
        uiState = uiState.copy(
            zoomState = uiState.zoomState.copy(
                linearZoom = cleanLinear,
                zoomRatio = ratio
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ====================================================================
        // LAYER 1: VIEWFINDER (CameraX Feed or Synthetic Fallback) + SHADERS
        // ====================================================================
        val renderEffect = remember(uiState.visionMode) {
            VisionShaders.getRenderEffectForMode(uiState.visionMode)
        }

        val currentZoomRatio by rememberUpdatedState(uiState.zoomState.zoomRatio)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.renderEffect = renderEffect
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (zoomChange != 1.0f) {
                            applyZoomRatio(currentZoomRatio * zoomChange)
                        }
                    }
                }
        ) {
            ViewfinderContainer(
                modifier = Modifier.fillMaxSize(),
                zoomRatio = uiState.zoomState.zoomRatio,
                forceFallback = forceFallback,
                hasCameraPermission = uiState.hasCameraPermission,
                externalHasCameraPermission = uiState.hasCameraPermission,
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onRequestPermissionOverride = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onCameraReady = { camera ->
                    activeCamera = camera
                },
                onZoomStateChanged = { newZoomState ->
                    uiState = uiState.copy(zoomState = newZoomState)
                },
                onPermissionStatusChanged = { granted ->
                    uiState = uiState.copy(hasCameraPermission = granted)
                },
                onCameraHardwareAvailabilityChanged = { available ->
                    uiState = uiState.copy(isCameraHardwareAvailable = available)
                },
                onFallbackActiveChanged = { isFallback ->
                    uiState = uiState.copy(isUsingFallbackTestPattern = isFallback)
                }
            )
        }

        // ====================================================================
        // LAYER 2: POST-PROCESSING OPTICAL FX LAYER
        // ====================================================================
        when (uiState.visionMode) {
            VisionMode.NIGHT_VISION -> {
                GrainOverlay(modifier = Modifier.fillMaxSize())
            }
            VisionMode.TACTICAL_MONO -> {
                ScanlineOverlay(modifier = Modifier.fillMaxSize())
            }
            else -> {
                // Clear pass-through or thermal AGSL handles internal coloring
            }
        }

        // ====================================================================
        // LAYER 3: TACTICAL HUD OVERLAY & CONTROLS (Imperial Amber Vector Graphics)
        // Rendered only when isUiVisible is TRUE
        // ====================================================================
        if (uiState.isUiVisible) {
            TacticalVisorHud(
                modifier = Modifier.fillMaxSize(),
                telemetry = uiState.telemetry,
                zoomState = uiState.zoomState,
                visionMode = uiState.visionMode,
                isFallback = uiState.isUsingFallbackTestPattern
            )

            // Left Edge: Multi-Spectral Vision Mode Selector
            VisionModeSelector(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp),
                currentMode = uiState.visionMode,
                onModeSelected = { selectedMode ->
                    uiState = uiState.copy(visionMode = selectedMode)
                }
            )

            // Right Edge: Tactical Zoom Thumb Dial & Presets
            TacticalZoomSlider(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                zoomState = uiState.zoomState,
                onLinearZoomChanged = { newLinear ->
                    applyLinearZoom(newLinear)
                },
                onZoomRatioChanged = { newRatio ->
                    applyZoomRatio(newRatio)
                }
            )
        }

        // ====================================================================
        // DEBUG CONTROL BAR (Rendered only when isDebugPanelVisible is true)
        // ====================================================================
        if (uiState.isDebugPanelVisible) {
            DebugControlBar(
                modifier = if (uiState.isUiVisible) {
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 42.dp)
                } else {
                    Modifier.align(Alignment.TopCenter)
                },
                isUiVisible = uiState.isUiVisible,
                onToggleUiVisibility = {
                    uiState = uiState.copy(isUiVisible = !uiState.isUiVisible)
                },
                zoomState = uiState.zoomState,
                onZoomStep = { delta ->
                    applyZoomRatio(uiState.zoomState.zoomRatio + delta)
                },
                onSetZoomRatio = { ratio ->
                    applyZoomRatio(ratio)
                },
                telemetry = uiState.telemetry,
                onAdjustHeading = { delta ->
                    sensorManager.adjustSimulatedHeading(delta)
                },
                onSetHeading = { heading ->
                    sensorManager.setSimulatedHeading(heading)
                },
                onAdjustPitch = { delta ->
                    sensorManager.adjustSimulatedPitch(delta)
                },
                onSetPitch = { pitch ->
                    sensorManager.setSimulatedPitch(pitch)
                },
                onResetSensors = {
                    sensorManager.resetSimulation()
                },
                hasCameraPermission = uiState.hasCameraPermission,
                onRequestCameraPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                isFallbackActive = uiState.isUsingFallbackTestPattern
            )
        }
    }
}
