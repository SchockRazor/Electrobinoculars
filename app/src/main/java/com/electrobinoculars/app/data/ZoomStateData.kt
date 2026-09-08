package com.electrobinoculars.app.data

import java.util.Locale

/**
 * Optical magnification and digital zoom telemetry state.
 *
 * @property zoomRatio Current absolute magnification ratio (e.g. 1.0f, 3.4f).
 * @property minZoomRatio Minimum zoom capability of camera hardware (typically 1.0f).
 * @property maxZoomRatio Maximum zoom capability supported by the hardware or digital engine.
 * @property linearZoom Normalized zoom ratio between 0.0f (min) and 1.0f (max).
 */
data class ZoomStateData(
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 8.0f,
    val linearZoom: Float = 0.0f
) {
    /**
     * Formatted magnification string for tactical military readouts (e.g. `[ 1.0x ]`).
     */
    val formattedMagnification: String
        get() {
            val safeRatio = if (zoomRatio.isNaN() || zoomRatio.isInfinite() || zoomRatio < 0.1f) 1.0f else zoomRatio
            return String.format(Locale.US, "[ %.1fx ]", safeRatio)
        }

    companion object {
        /**
         * Calculates normalized linear zoom [0.0f, 1.0f] from a zoom ratio and bounds,
         * with complete protection against division-by-zero, NaN, and negative ratios.
         */
        fun calculateLinearZoom(zoomRatio: Float, minZoom: Float, maxZoom: Float): Float {
            if (zoomRatio.isNaN() || minZoom.isNaN() || maxZoom.isNaN()) return 0.0f
            val range = maxZoom - minZoom
            if (range <= 0.0001f) return 0.0f
            return ((zoomRatio - minZoom) / range).coerceIn(0.0f, 1.0f)
        }

        /**
         * Calculates absolute zoom ratio from a linear zoom factor [0.0f, 1.0f] and bounds,
         * with complete protection against NaN and out-of-bound values.
         */
        fun calculateZoomRatio(linearZoom: Float, minZoom: Float, maxZoom: Float): Float {
            val cleanMin = if (minZoom.isNaN() || minZoom < 1.0f) 1.0f else minZoom
            val cleanMax = if (maxZoom.isNaN() || maxZoom < cleanMin) cleanMin else maxZoom
            if (linearZoom.isNaN()) return cleanMin
            val clampedLinear = linearZoom.coerceIn(0.0f, 1.0f)
            return (cleanMin + (clampedLinear * (cleanMax - cleanMin)))
        }
    }
}
