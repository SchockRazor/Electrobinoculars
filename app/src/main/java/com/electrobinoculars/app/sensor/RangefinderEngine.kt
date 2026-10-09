package com.electrobinoculars.app.sensor

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Confidence level of the rangefinder distance estimate.
 *
 * Based on the pitch angle magnitude — larger downward angles produce
 * geometrically more reliable trigonometric estimates because the
 * derivative of `H/tan(θ)` w.r.t. θ is smaller relative to the distance.
 *
 * @property label HUD display label for tactical readout.
 */
enum class RangeConfidence(val label: String) {
    /** Pitch > 8°: strong geometry, distance estimate is reliable within ~10%. */
    HIGH("[LOCK-ON]"),
    /** Pitch 4°–8°: moderate geometry, estimate is reasonable within ~25%. */
    MEDIUM("[TRACKING]"),
    /** Pitch 2°–4°: weak geometry, estimate is approximate only. */
    LOW("[SCANNING]"),
    /** Pitch < 2° or looking up: trigonometric estimation is meaningless. */
    NO_LOCK("[NO LOCK]")
}

/**
 * Result of a rangefinder distance computation.
 *
 * @property distanceMeters Estimated distance in meters, quantized to contextually appropriate precision.
 * @property confidence Confidence level of the estimate based on pitch angle quality.
 */
data class RangeResult(
    val distanceMeters: Int,
    val confidence: RangeConfidence
)

/**
 * Camera-frame attitude angles derived from a gravity-referenced rotation matrix.
 *
 * @property pitchDegrees Camera optical-axis pitch (elevation) in degrees: 0° = level aiming,
 * negative = looking down (depression), positive = looking up.
 * @property rollDegrees Camera roll about the optical axis in degrees: positive = right edge down.
 */
data class CameraAngles(
    val pitchDegrees: Float,
    val rollDegrees: Float
)

/**
 * Extracts camera-referenced attitude from a screen-aligned, world-referenced rotation matrix.
 *
 * [android.hardware.SensorManager.getOrientation] returns Euler angles of the device frame:
 * after a landscape remap its "pitch" reads ~±90° when the phone is held upright aiming at the
 * horizon, and its "roll" axis points into the ground — neither is usable for camera-frame HUD
 * geometry. Deriving the angles from the matrix columns instead yields the true optical-axis
 * attitude:
 * - The camera looks along the device -Z axis (out of the back of the phone), which the screen
 *   remap leaves unchanged, so the optical-axis pitch is remap-invariant and gimbal-safe.
 * - Screen right/up directions are matrix columns 0/1 expressed in world coordinates
 *   (X = east, Y = north, Z = up), giving the roll of the horizon within the viewfinder.
 */
object CameraAttitude {

    /**
     * Computes camera pitch and roll from a row-major 3x3 rotation matrix (9 floats).
     *
     * @param matrix Screen-aligned device-to-world rotation matrix (e.g. the output of
     * [android.hardware.SensorManager.remapCoordinateSystem]).
     * @return [CameraAngles] with 0°/0° fallback for degenerate or malformed input.
     */
    fun fromRotationMatrix(matrix: FloatArray): CameraAngles {
        if (matrix.size < 9) return CameraAngles(0.0f, 0.0f)
        for (i in 0 until 9) {
            val v = matrix[i]
            if (v.isNaN() || v.isInfinite()) return CameraAngles(0.0f, 0.0f)
        }

        // Forward world Z at index 8 (negative third column of the matrix): level = 0,
        // looking down = negative pitch, matching the rangefinder's depression convention.
        val forwardZ = -matrix[8].toDouble().coerceIn(-1.0, 1.0)
        val pitchDeg = Math.toDegrees(asin(forwardZ)).toFloat()

        // Screen right/up world Z components at indices 6/7 (third matrix row); the negative
        // sign yields the positive = right-edge-down roll convention used by the HUD ladder.
        val rollDeg = -Math.toDegrees(atan2(matrix[6].toDouble(), matrix[7].toDouble())).toFloat()

        return CameraAngles(pitchDeg, rollDeg)
    }
}

/**
 * Tactical Rangefinder Engine.
 *
 * Computes distance estimates using monocular trigonometric ranging:
 * `D = H / tan(|pitch|)`, where H is the observer's eye height above ground
 * and pitch is the depression angle of the camera's optical axis relative to
 * the horizon (0° = level aiming, negative = looking down) — see [CameraAttitude]
 * for how that angle is derived from the sensor rotation matrix.
 *
 * This is the same principle used by military mil-dot reticles and stadiametric
 * rangefinders — given a known baseline (observer height) and a measured angle,
 * the horizontal distance to the point where the line of sight meets the ground
 * can be computed trigonometrically.
 *
 * **Key design decisions:**
 * - Zoom/magnification is NOT factored into distance — zooming changes apparent
 *   size but not physical distance.
 * - Distances are quantized to contextually meaningful precision to avoid false
 *   precision that the sensor noise cannot support.
 * - A confidence level is provided based on pitch angle magnitude, since the
 *   estimation becomes unreliable at near-horizontal angles.
 */
object RangefinderEngine {

    private const val DEFAULT_OBSERVER_HEIGHT_METERS = 1.70f
    private const val MIN_ESTIMATED_RANGE_METERS = 2
    private const val MAX_ESTIMATED_RANGE_METERS = 9999

    // Confidence thresholds (absolute pitch in degrees)
    private const val PITCH_HIGH_CONFIDENCE = 8.0f
    private const val PITCH_MEDIUM_CONFIDENCE = 4.0f
    private const val PITCH_LOW_CONFIDENCE = 2.0f

    /**
     * Computes the estimated distance to the ground-level target.
     *
     * Uses the fundamental trigonometric relationship:
     * ```
     * D = H / tan(|pitch|)
     * ```
     * where:
     * - D = horizontal distance to target
     * - H = observer eye height above ground plane
     * - pitch = camera optical-axis depression angle below horizontal (negative = looking down)
     *
     * The result is quantized to avoid false precision:
     * - < 50m: nearest 1m
     * - 50–200m: nearest 5m
     * - 200–500m: nearest 10m
     * - 500–1000m: nearest 25m
     * - 1000–2000m: nearest 50m
     * - > 2000m: nearest 100m
     *
     * @param pitchDegrees Camera line-of-sight pitch in degrees: 0° = level aiming,
     * negative = looking down, positive = looking up.
     * @param observerHeightMeters Eye-level height of the observer. Defaults to 1.70m.
     * @return [RangeResult] containing quantized distance and confidence level.
     */
    fun calculateRange(
        pitchDegrees: Float,
        observerHeightMeters: Float = DEFAULT_OBSERVER_HEIGHT_METERS
    ): RangeResult {
        val cleanPitch = if (pitchDegrees.isNaN() || pitchDegrees.isInfinite()) 0.0f else pitchDegrees
        val absPitch = abs(cleanPitch)

        // Determine confidence based on pitch magnitude
        val confidence = when {
            absPitch >= PITCH_HIGH_CONFIDENCE -> RangeConfidence.HIGH
            absPitch >= PITCH_MEDIUM_CONFIDENCE -> RangeConfidence.MEDIUM
            absPitch >= PITCH_LOW_CONFIDENCE -> RangeConfidence.LOW
            else -> RangeConfidence.NO_LOCK
        }

        // If looking up or nearly horizontal, distance is indeterminate
        if (cleanPitch >= 0.0f || absPitch < 0.3f) {
            return RangeResult(
                distanceMeters = MAX_ESTIMATED_RANGE_METERS,
                confidence = if (cleanPitch > 1.0f) RangeConfidence.NO_LOCK else confidence
            )
        }

        // Core trigonometric formula: D = H / tan(|pitch|)
        val pitchRad = Math.toRadians(absPitch.toDouble())
        val tanPitch = tan(pitchRad)

        if (tanPitch <= 0.0 || tanPitch.isNaN() || tanPitch.isInfinite()) {
            return RangeResult(MAX_ESTIMATED_RANGE_METERS, RangeConfidence.NO_LOCK)
        }

        val safeHeight = if (observerHeightMeters.isNaN() || observerHeightMeters <= 0f) {
            DEFAULT_OBSERVER_HEIGHT_METERS
        } else {
            observerHeightMeters
        }

        val rawDistance = (safeHeight / tanPitch).toFloat()
        val clampedDistance = rawDistance.coerceIn(
            MIN_ESTIMATED_RANGE_METERS.toFloat(),
            MAX_ESTIMATED_RANGE_METERS.toFloat()
        )

        // Quantize to contextually appropriate precision
        val quantized = quantizeDistance(clampedDistance)

        return RangeResult(
            distanceMeters = quantized.coerceIn(MIN_ESTIMATED_RANGE_METERS, MAX_ESTIMATED_RANGE_METERS),
            confidence = confidence
        )
    }

    /**
     * Quantizes a raw distance to contextually appropriate precision.
     *
     * The sensor noise at various pitch angles translates to different
     * distance uncertainties. This quantization prevents displaying
     * false precision that the measurement cannot support.
     */
    private fun quantizeDistance(distanceMeters: Float): Int {
        val step = when {
            distanceMeters < 50f -> 1f
            distanceMeters < 200f -> 5f
            distanceMeters < 500f -> 10f
            distanceMeters < 1000f -> 25f
            distanceMeters < 2000f -> 50f
            else -> 100f
        }
        return (distanceMeters / step).roundToInt() * step.toInt()
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
