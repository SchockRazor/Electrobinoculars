package com.electrobinoculars.app.data

/**
 * Global UI state model for the Electrobinoculars application.
 *
 * Coordinates multi-spectral vision mode, zoom magnification, sensor telemetry,
 * and camera hardware/fallback state across the 3-layer architecture.
 *
 * @property visionMode Currently active multi-spectral optical filter mode.
 * @property zoomState Current optical and digital zoom ratio telemetry.
 * @property telemetry Current device compass heading, pitch/roll ladder, and rangefinder distance.
 * @property hasCameraPermission True if runtime camera permission has been granted by user.
 * @property isCameraHardwareAvailable True if physical back camera hardware exists on the device.
 * @property isUsingFallbackTestPattern True if the synthetic animated calibration grid is active.
 * @property isUiVisible True if tactical HUD and controls should be displayed, false for minimal debug mode (defaults to false at launch).
 * @property isDebugPanelVisible True if debug buttons are displayed.
 */
data class ElectrobinocularsUiState(
    val visionMode: VisionMode = VisionMode.STANDARD,
    val zoomState: ZoomStateData = ZoomStateData(),
    val telemetry: SensorTelemetry = SensorTelemetry(),
    val hasCameraPermission: Boolean = false,
    val isCameraHardwareAvailable: Boolean = true,
    val isUsingFallbackTestPattern: Boolean = false,
    val isUiVisible: Boolean = false,
    val isDebugPanelVisible: Boolean = true
)
