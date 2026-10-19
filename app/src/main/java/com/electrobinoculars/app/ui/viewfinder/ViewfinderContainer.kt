package com.electrobinoculars.app.ui.viewfinder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.electrobinoculars.app.data.ZoomStateData

/**
 * Container composable managing viewfinder hardware negotiation, permission lifecycle,
 * and dynamic switching between the live [CameraFeedView] and procedural [SyntheticCalibrationGrid].
 *
 * @param modifier Composable layout modifier.
 * @param zoomRatio Current zoom magnification ratio to pass to the active viewfinder.
 * @param forceFallback Optional simulation/debug override. When true, forces the synthetic calibration grid
 *                      even if camera hardware and permissions are valid. WARNING: Do NOT bind this parameter
 *                      to [onFallbackActiveChanged] output state; doing so creates a circular latch.
 * @param onCameraReady Callback providing the bound [Camera] instance for zoom/control manipulation.
 * @param onZoomStateChanged Callback providing updated zoom telemetry.
 * @param onPermissionStatusChanged Callback notifying state holders of camera permission status.
 * @param onCameraHardwareAvailabilityChanged Callback notifying state holders if camera hardware is absent.
 * @param onFallbackActiveChanged Callback notifying whether the fallback synthetic grid is actively displayed.
 */
@Composable
fun ViewfinderContainer(
    modifier: Modifier = Modifier,
    zoomRatio: Float = 1.0f,
    forceFallback: Boolean = false,
    onCameraReady: (Camera) -> Unit = {},
    onZoomStateChanged: (ZoomStateData) -> Unit = {},
    onPermissionStatusChanged: (Boolean) -> Unit = {},
    onCameraHardwareAvailabilityChanged: (Boolean) -> Unit = {},
    onFallbackActiveChanged: (Boolean) -> Unit = {},
    externalHasCameraPermission: Boolean? = null,
    hasCameraPermission: Boolean? = null,
    onRequestPermissionOverride: (() -> Unit)? = null,
    onRequestPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var internalHasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraHardwareAvailable by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        internalHasCameraPermission = isGranted
        onPermissionStatusChanged(isGranted)
    }

    // Determine effective permission: external state takes priority when supplied
    val effectivePermission = externalHasCameraPermission ?: hasCameraPermission ?: internalHasCameraPermission

    // Keep internal permission state aligned when external permission updates
    LaunchedEffect(externalHasCameraPermission, hasCameraPermission) {
        val external = externalHasCameraPermission ?: hasCameraPermission
        if (external != null && external != internalHasCameraPermission) {
            internalHasCameraPermission = external
        }
    }

    // Automatically check permission when resuming (e.g. user returns from App Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (currentPermission != internalHasCameraPermission) {
                    internalHasCameraPermission = currentPermission
                    onPermissionStatusChanged(currentPermission)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Synchronize permission and hardware state with parent state holders
    LaunchedEffect(effectivePermission) {
        onPermissionStatusChanged(effectivePermission)
    }

    LaunchedEffect(cameraHardwareAvailable) {
        onCameraHardwareAvailabilityChanged(cameraHardwareAvailable)
    }

    // Determine whether fallback synthetic grid must be shown
    val shouldShowFallback = forceFallback || !effectivePermission || !cameraHardwareAvailable

    LaunchedEffect(shouldShowFallback) {
        onFallbackActiveChanged(shouldShowFallback)
    }

    val requestPermissionAction = onRequestPermissionOverride ?: onRequestPermission ?: {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (shouldShowFallback) {
            SyntheticCalibrationGrid(
                modifier = Modifier.fillMaxSize(),
                simulatedZoomRatio = zoomRatio,
                showEngagePermissionButton = !effectivePermission && cameraHardwareAvailable,
                onRequestPermission = requestPermissionAction
            )
        } else {
            CameraFeedView(
                modifier = Modifier.fillMaxSize(),
                onCameraReady = onCameraReady,
                onZoomStateChanged = onZoomStateChanged,
                onCameraUnavailable = {
                    cameraHardwareAvailable = false
                }
            )
        }
    }
}
