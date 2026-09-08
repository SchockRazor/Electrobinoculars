package com.electrobinoculars.app.data

/**
 * Real-time device attitude, orientation, and rangefinder telemetry.
 *
 * @property headingDegrees Azimuth compass angle in degrees [0.0f, 359.9f].
 * @property cardinalDirection 16-point cardinal compass rose designation (e.g. "N", "WSW").
 * @property pitch Device inclination tilt in degrees (negative = looking down, positive = looking up).
 * @property roll Device lateral roll tilt in degrees.
 * @property estimatedDistanceMeters Estimated target distance in meters computed by rangefinder engine.
 * @property isAvailable True if motion/magnetic sensors are operational on device.
 */
data class SensorTelemetry(
    val headingDegrees: Float = 0.0f,
    val cardinalDirection: String = "N",
    val pitch: Float = 0.0f,
    val roll: Float = 0.0f,
    val estimatedDistanceMeters: Int = 100,
    val isAvailable: Boolean = true
)
