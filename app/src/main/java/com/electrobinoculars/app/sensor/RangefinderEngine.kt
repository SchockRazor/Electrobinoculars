package com.electrobinoculars.app.sensor

import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.tan

/**
 * Tactical Rangefinder Engine.
 *
 * Calculates dynamic continuous stadiametric range estimation based on device pitch tilt angle,
 * simulated eye-height / ground baseline, and current optical/digital zoom factor.
 */
object RangefinderEngine {

    private const val DEFAULT_OBSERVER_HEIGHT_METERS = 1.75f // Typical eye-level standing height
    private const val MIN_ESTIMATED_RANGE_METERS = 8
    private const val MAX_ESTIMATED_RANGE_METERS = 9999

    /**
     * Computes the estimated distance to target in meters.
     *
     * @param pitchDegrees Pitch tilt in degrees (-90 down to +90 up).
     * @param zoomRatio Current digital/optical zoom magnification ratio (1.0x to 10.0x+).
     * @return Formatted estimated range in meters, clamped between [MIN_ESTIMATED_RANGE_METERS] and [MAX_ESTIMATED_RANGE_METERS].
     */
    fun calculateEstimatedRangeMeters(pitchDegrees: Float, zoomRatio: Float): Int {
        val cleanZoom = if (zoomRatio.isNaN() || zoomRatio.isInfinite()) 1.0f else zoomRatio.coerceAtLeast(1.0f)
        val cleanPitch = if (pitchDegrees.isNaN() || pitchDegrees.isInfinite()) 0.0f else pitchDegrees
        val absPitch = abs(cleanPitch).coerceIn(0.5f, 85f)
        val pitchRad = Math.toRadians(absPitch.toDouble())
        val tanPitch = tan(pitchRad)
        if (tanPitch <= 0.0 || tanPitch.isNaN()) {
            return MAX_ESTIMATED_RANGE_METERS
        }

        // Stadiametric ground trigonometrics: D = H / tan(pitch)
        // Scaled by zoom resolution to simulate optical focal depth lock
        val baselineDistance = (DEFAULT_OBSERVER_HEIGHT_METERS / tanPitch).toFloat()
        val zoomMagnificationFactor = cleanZoom * 12.5f

        val estimatedRange = (baselineDistance * zoomMagnificationFactor).toInt()
        return estimatedRange.coerceIn(MIN_ESTIMATED_RANGE_METERS, MAX_ESTIMATED_RANGE_METERS)
    }

    /**
     * Converts an azimuth angle (0..359) into a 16-point cardinal compass string.
     */
    fun getCardinalDirection(azimuthDegrees: Float): String {
        val directions = arrayOf(
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
        )
        val cleanAzimuth = if (azimuthDegrees.isNaN() || azimuthDegrees.isInfinite()) 0.0f else azimuthDegrees
        val normalized = ((cleanAzimuth % 360f) + 360f) % 360f
        val rawIndex = ((normalized + 11.25f) / 22.5f).toInt()
        val index = ((rawIndex % 16) + 16) % 16
        return directions[index]
    }
}
