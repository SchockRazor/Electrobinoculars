package com.electrobinoculars.app.sensor

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight single-pole Exponential Moving Average (EMA) filter.
 *
 * Smooths noisy sensor readings with O(1) time and space per sample.
 * The smoothing factor [alpha] controls responsiveness:
 * - Higher alpha (0.3–0.5) → faster response, less smoothing
 * - Lower alpha (0.05–0.15) → slower response, more smoothing
 *
 * Effective window size ≈ 2/alpha - 1 samples.
 *
 * @param alpha Smoothing factor in (0.0, 1.0]. Default 0.15 gives ~12-sample effective window.
 */
class ExponentialMovingAverage(private val alpha: Float = 0.15f) {

    private var value: Float = Float.NaN
    private var initialized = false

    /**
     * Feeds a new raw sample and returns the filtered output.
     */
    fun update(rawValue: Float): Float {
        if (rawValue.isNaN() || rawValue.isInfinite()) {
            return if (initialized) value else 0.0f
        }
        if (!initialized) {
            value = rawValue
            initialized = true
            return value
        }
        value = alpha * rawValue + (1.0f - alpha) * value
        return value
    }

    /**
     * Returns the current filtered value without feeding a new sample.
     */
    fun current(): Float = if (initialized) value else 0.0f

    /**
     * Resets the filter state.
     */
    fun reset() {
        value = Float.NaN
        initialized = false
    }
}

/**
 * EMA filter for circular/angular values that wrap at 360°.
 *
 * Prevents the discontinuity at the 0°/360° boundary from causing
 * the filter to swing through 180° when transitioning across north.
 * Uses atan2-based averaging in Cartesian space.
 *
 * @param alpha Smoothing factor in (0.0, 1.0].
 */
class CircularExponentialMovingAverage(private val alpha: Float = 0.15f) {

    private var avgSin: Float = 0.0f
    private var avgCos: Float = 0.0f
    private var initialized = false

    /**
     * Feeds a new angular sample in degrees and returns the filtered output in [0, 360).
     */
    fun update(angleDegrees: Float): Float {
        if (angleDegrees.isNaN() || angleDegrees.isInfinite()) {
            return current()
        }
        val rad = Math.toRadians(angleDegrees.toDouble())
        val s = sin(rad).toFloat()
        val c = cos(rad).toFloat()

        if (!initialized) {
            avgSin = s
            avgCos = c
            initialized = true
        } else {
            avgSin = alpha * s + (1.0f - alpha) * avgSin
            avgCos = alpha * c + (1.0f - alpha) * avgCos
        }
        return current()
    }

    /**
     * Returns the current filtered angle in degrees [0, 360).
     */
    fun current(): Float {
        if (!initialized) return 0.0f
        var degrees = Math.toDegrees(atan2(avgSin.toDouble(), avgCos.toDouble())).toFloat()
        if (degrees < 0) degrees += 360f
        return degrees
    }

    /**
     * Resets the filter state.
     */
    fun reset() {
        avgSin = 0.0f
        avgCos = 0.0f
        initialized = false
    }
}

/**
 * Dead-zone filter that suppresses rapid output changes when the input
 * is within a "low confidence" zone (e.g. near-horizontal pitch where
 * trigonometric distance estimation becomes unstable).
 *
 * When the absolute input value is below [deadZoneThreshold], the filter
 * holds its last stable output and sets [isInDeadZone] to true.
 * When the input exits the dead zone, it resumes tracking.
 *
 * @param deadZoneThreshold Absolute value below which the input is considered unreliable.
 * @param hysteresis Extra margin beyond threshold before re-engaging tracking, to prevent chatter.
 */
class DeadZoneFilter(
    private val deadZoneThreshold: Float = 1.5f,
    private val hysteresis: Float = 0.5f
) {

    private var lastStableValue: Float = Float.NaN
    var isInDeadZone: Boolean = false
        private set
    private var initialized = false

    /**
     * Feeds a new value and returns the filtered output.
     * When inside the dead zone, returns the last stable value.
     */
    fun update(value: Float): Float {
        if (value.isNaN() || value.isInfinite()) {
            return if (initialized) lastStableValue else 0.0f
        }

        val absValue = abs(value)

        if (!initialized) {
            lastStableValue = value
            initialized = true
            isInDeadZone = absValue < deadZoneThreshold
            return value
        }

        if (isInDeadZone) {
            // Exit dead zone only when value exceeds threshold + hysteresis
            if (absValue >= deadZoneThreshold + hysteresis) {
                isInDeadZone = false
                lastStableValue = value
            }
            // While in dead zone, continue returning last stable value
        } else {
            // Enter dead zone when value drops below threshold
            if (absValue < deadZoneThreshold) {
                isInDeadZone = true
                // Keep lastStableValue frozen
            } else {
                lastStableValue = value
            }
        }

        return lastStableValue
    }

    /**
     * Returns the current output value.
     */
    fun current(): Float = if (initialized) lastStableValue else 0.0f

    /**
     * Resets the filter state.
     */
    fun reset() {
        lastStableValue = Float.NaN
        isInDeadZone = false
        initialized = false
    }
}
