package com.electrobinoculars.app.data

import com.electrobinoculars.app.sensor.RangeConfidence

/**
 * Real-time device attitude, orientation, and rangefinder telemetry.
 *
 * @property headingDegrees Azimuth compass angle in degrees [0.0f, 359.9f].
 * @property cardinalDirection 16-point cardinal compass rose designation (e.g. "N", "WSW").
 * @property pitch Device inclination tilt in degrees (negative = looking down, positive = looking up).
 * @property roll Device lateral roll tilt in degrees.
 * @property estimatedDistanceMeters Estimated target distance in meters computed by rangefinder engine.
 * @property rangeConfidence Confidence level of the distance estimate based on pitch angle quality.
 * @property isRangeStable True if the pitch dead-zone filter is NOT actively suppressing (stable reading).
 * @property isAvailable True if motion/magnetic sensors are operational on device.
 */
data class SensorTelemetry(
    val headingDegrees: Float = 0.0f,
    val cardinalDirection: String = "N",
    val pitch: Float = 0.0f,
    val roll: Float = 0.0f,
    val estimatedDistanceMeters: Int = 100,
    val rangeConfidence: RangeConfidence = RangeConfidence.NO_LOCK,
    val isRangeStable: Boolean = true,
    val isAvailable: Boolean = true
)
