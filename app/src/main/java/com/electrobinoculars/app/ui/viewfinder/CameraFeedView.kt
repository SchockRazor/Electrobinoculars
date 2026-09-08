package com.electrobinoculars.app.ui.viewfinder

import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.electrobinoculars.app.data.ZoomStateData

private const val TAG = "CameraFeedView"

/**
 * Holder maintaining CameraX provider, active camera, and observer references
 * for clean lifecycle disposal and race-condition prevention.
 */
private class CameraBindingHolder {
    var cameraProvider: ProcessCameraProvider? = null
    var activeCamera: Camera? = null
    var zoomObserver: Observer<ZoomState>? = null
    var isDisposed: Boolean = false
}

/**
 * CameraX Live Viewfinder Feed.
 *
 * Employs [PreviewView.ImplementationMode.COMPATIBLE] (TextureView backend) so that
 * Compose `graphicsLayer` filters, color matrices, and AGSL runtime shaders can be applied
 * directly to the preview texture without punch-through surface artifacts.
 *
 * @param modifier Composable layout modifier.
 * @param onCameraReady Callback invoked with the bound [Camera] instance once initialized.
 * @param onZoomStateChanged Callback invoked when CameraX updates its zoom ratio/linear zoom state.
 * @param onCameraUnavailable Callback invoked if no back camera hardware exists or binding fails.
 */
@Composable
fun CameraFeedView(
    modifier: Modifier = Modifier,
    onCameraReady: (Camera) -> Unit = {},
    onZoomStateChanged: (ZoomStateData) -> Unit = {},
    onCameraUnavailable: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Safely capture latest callback references across recompositions to prevent stale closures
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)
    val currentOnZoomStateChanged by rememberUpdatedState(onZoomStateChanged)
    val currentOnCameraUnavailable by rememberUpdatedState(onCameraUnavailable)

    // Holder maintaining camera resources for safe lifecycle teardown
    val bindingHolder = remember { CameraBindingHolder() }

    DisposableEffect(lifecycleOwner) {
        bindingHolder.isDisposed = false
        onDispose {
            bindingHolder.isDisposed = true
            try {
                bindingHolder.zoomObserver?.let { observer ->
                    bindingHolder.activeCamera?.cameraInfo?.zoomState?.removeObserver(observer)
                }
                bindingHolder.zoomObserver = null
                bindingHolder.cameraProvider?.unbindAll()
                bindingHolder.cameraProvider = null
                bindingHolder.activeCamera = null
                Log.d(TAG, "CameraX resources cleanly disposed on lifecycle unmount")
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up CameraX resources on disposal", e)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // Critical: COMPATIBLE uses TextureView, allowing Jetpack Compose graphicsLayer
                // color filters, AGSL shaders, and alpha blending to composite cleanly.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val executor = ContextCompat.getMainExecutor(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                // Guard against disposal or lifecycle destruction while the provider future was resolving
                if (bindingHolder.isDisposed || lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    try {
                        cameraProviderFuture.get()?.unbindAll()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error unbinding orphaned camera provider", e)
                    }
                    return@addListener
                }

                try {
                    val cameraProvider = cameraProviderFuture.get()
                    bindingHolder.cameraProvider = cameraProvider

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    if (!cameraProvider.hasCamera(cameraSelector)) {
                        Log.w(TAG, "Device reports no DEFAULT_BACK_CAMERA available")
                        currentOnCameraUnavailable()
                        return@addListener
                    }

                    val preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    bindingHolder.activeCamera = camera
                    currentOnCameraReady(camera)

                    // Observe CameraX ZoomState LiveData
                    val zoomObserver = Observer<ZoomState> { state ->
                        state?.let {
                            currentOnZoomStateChanged(
                                ZoomStateData(
                                    zoomRatio = it.zoomRatio,
                                    minZoomRatio = it.minZoomRatio,
                                    maxZoomRatio = it.maxZoomRatio,
                                    linearZoom = it.linearZoom
                                )
                            )
                        }
                    }
                    bindingHolder.zoomObserver = zoomObserver
                    camera.cameraInfo.zoomState.observe(lifecycleOwner, zoomObserver)
                    Log.d(TAG, "CameraX successfully bound to lifecycle")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bind CameraX lifecycle use cases", e)
                    currentOnCameraUnavailable()
                }
            }, executor)

            previewView
        },
        modifier = modifier.fillMaxSize(),
        update = {
            // No-op: CameraX independently manages rendering through PreviewView.SurfaceProvider.
            // Keeping update empty prevents unbinding/rebinding thrash on parent recomposition.
        }
    )
}
