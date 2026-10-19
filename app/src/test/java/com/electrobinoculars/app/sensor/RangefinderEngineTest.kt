package com.electrobinoculars.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unit tests for RangefinderEngine stadiametric math, cardinal heading conversions,
 * and camera-frame attitude extraction from gravity-referenced rotation matrices.
 */
class RangefinderEngineTest {

    @Test
    fun testAllSixteenCardinalDirections() {
        // Precise angles for all 16 cardinal directions
        assertEquals("N", RangefinderEngine.getCardinalDirection(0f))
        assertEquals("NNE", RangefinderEngine.getCardinalDirection(22.5f))
        assertEquals("NE", RangefinderEngine.getCardinalDirection(45.0f))
        assertEquals("ENE", RangefinderEngine.getCardinalDirection(67.5f))
        assertEquals("E", RangefinderEngine.getCardinalDirection(90.0f))
        assertEquals("ESE", RangefinderEngine.getCardinalDirection(112.5f))
        assertEquals("SE", RangefinderEngine.getCardinalDirection(135.0f))
        assertEquals("SSE", RangefinderEngine.getCardinalDirection(157.5f))
        assertEquals("S", RangefinderEngine.getCardinalDirection(180.0f))
        assertEquals("SSW", RangefinderEngine.getCardinalDirection(202.5f))
        assertEquals("SW", RangefinderEngine.getCardinalDirection(225.0f))
        assertEquals("WSW", RangefinderEngine.getCardinalDirection(247.5f))
        assertEquals("W", RangefinderEngine.getCardinalDirection(270.0f))
        assertEquals("WNW", RangefinderEngine.getCardinalDirection(292.5f))
        assertEquals("NW", RangefinderEngine.getCardinalDirection(315.0f))
        assertEquals("NNW", RangefinderEngine.getCardinalDirection(337.5f))
    }

    @Test
    fun testCardinalDirectionThresholdBoundaries() {
        // North sector is [348.75 .. 360) and [0 .. 11.25)
        assertEquals("N", RangefinderEngine.getCardinalDirection(11.24f))
        assertEquals("NNE", RangefinderEngine.getCardinalDirection(11.26f))
        assertEquals("NNE", RangefinderEngine.getCardinalDirection(33.74f))
        assertEquals("NE", RangefinderEngine.getCardinalDirection(33.76f))
        assertEquals("NNW", RangefinderEngine.getCardinalDirection(348.74f))
        assertEquals("N", RangefinderEngine.getCardinalDirection(348.76f))
        assertEquals("N", RangefinderEngine.getCardinalDirection(359.99f))
    }

    @Test
    fun testCardinalDirectionRotationsAndNegativeAngles() {
        // Multi-revolution angles
        assertEquals("N", RangefinderEngine.getCardinalDirection(360f))
        assertEquals("E", RangefinderEngine.getCardinalDirection(450f))
        assertEquals("N", RangefinderEngine.getCardinalDirection(720f))

        // Negative angle conversions
        assertEquals("W", RangefinderEngine.getCardinalDirection(-90f))
        assertEquals("S", RangefinderEngine.getCardinalDirection(-180f))
        assertEquals("E", RangefinderEngine.getCardinalDirection(-270f))
        assertEquals("N", RangefinderEngine.getCardinalDirection(-360f))
        assertEquals("NW", RangefinderEngine.getCardinalDirection(-45f))
        assertEquals("N", RangefinderEngine.getCardinalDirection(-720f))
    }

    @Test
    fun testCardinalDirectionNaNAndInfinitySafety() {
        // Must never throw ArrayIndexOutOfBoundsException
        assertEquals("N", RangefinderEngine.getCardinalDirection(Float.NaN))
        assertEquals("N", RangefinderEngine.getCardinalDirection(Float.POSITIVE_INFINITY))
        assertEquals("N", RangefinderEngine.getCardinalDirection(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun testLevelAimingReportsNoLockInsteadOfMinimumClamp() {
        // Regression: with the device-frame pitch bug, holding the phone upright aiming
        // level collapsed the estimate onto the 2m minimum clamp. Level or upward aiming
        // has no ground target, so the engine must report maximum range with NO LOCK.
        val level = RangefinderEngine.calculateRange(0f)
        assertEquals(9999, level.distanceMeters)
        assertEquals(RangeConfidence.NO_LOCK, level.confidence)

        val lookingUp = RangefinderEngine.calculateRange(10f)
        assertEquals(9999, lookingUp.distanceMeters)
        assertEquals(RangeConfidence.NO_LOCK, lookingUp.confidence)
    }

    @Test
    fun testUprightDeviceYieldsRealisticDistances() {
        // Simulates the phone held upright in landscape (the normal binoculars grip).
        // Issue acceptance: distances must be realistic (10–500m), never stuck at the 2m floor.
        val level = CameraAttitude.fromRotationMatrix(uprightLandscapeMatrix(depressionDeg = 0f))
        assertTrue("Level upright pitch should be ~0°, was ${level.pitchDegrees}", abs(level.pitchDegrees) < 0.01f)
        assertEquals(9999, RangefinderEngine.calculateRange(level.pitchDegrees).distanceMeters)

        val fiveDown = CameraAttitude.fromRotationMatrix(uprightLandscapeMatrix(depressionDeg = 5f))
        assertTrue("5° depression pitch should be ~-5°, was ${fiveDown.pitchDegrees}", abs(fiveDown.pitchDegrees + 5f) < 0.01f)
        val midDistance = RangefinderEngine.calculateRange(fiveDown.pitchDegrees)
        assertTrue(
            "5° depression -> ${midDistance.distanceMeters}m, expected a realistic 10..500m",
            midDistance.distanceMeters in 10..500
        )
        // D = 1.70m / tan(5°) = 19.4m, quantized to nearest 1m
        assertEquals(19, midDistance.distanceMeters)
    }

    @Test
    fun testDistanceDecreasesMonotonicallyWithDepression() {
        // D = 1.70 / tan(θ): 2° -> 48.7m, 10° -> 9.6m, 30° -> 2.9m, 60° -> 0.98m
        val shallow = RangefinderEngine.calculateRange(-2f).distanceMeters
        val mid = RangefinderEngine.calculateRange(-10f).distanceMeters
        val steep = RangefinderEngine.calculateRange(-30f).distanceMeters
        val nadir = RangefinderEngine.calculateRange(-60f).distanceMeters
        assertTrue(shallow > mid)
        assertTrue(mid > steep)
        assertTrue(steep > nadir)

        assertEquals(49, shallow)
        assertEquals(10, mid)
        assertEquals(3, steep)
        assertEquals(2, nadir)
    }

    @Test
    fun testQuantizationSteps() {
        // 1.70 / tan(1°) = 97.4m -> 50–200m bucket rounds to nearest 5m
        assertEquals(95, RangefinderEngine.calculateRange(-1f).distanceMeters)
        // 1.70 / tan(0.5°) = 195.0m -> 200m bucket boundary
        assertTrue(RangefinderEngine.calculateRange(-0.5f).distanceMeters in 190..200)
        // > 2000m quantizes to 100m steps: 1.70 / tan(0.05°) = 1949m -> just below 2km bucket
        assertTrue(RangefinderEngine.calculateRange(-0.05f).distanceMeters >= 1900)
    }

    @Test
    fun testConfidenceLevelsFollowDepressionAngle() {
        assertEquals(RangeConfidence.HIGH, RangefinderEngine.calculateRange(-10f).confidence)
        assertEquals(RangeConfidence.MEDIUM, RangefinderEngine.calculateRange(-5f).confidence)
        assertEquals(RangeConfidence.LOW, RangefinderEngine.calculateRange(-3f).confidence)
        assertEquals(RangeConfidence.NO_LOCK, RangefinderEngine.calculateRange(-1.5f).confidence)
        assertEquals(RangeConfidence.NO_LOCK, RangefinderEngine.calculateRange(5f).confidence)
    }

    @Test
    fun testExtremeDepressionClampsToPhysicalBounds() {
        // Nearly straight down: D = 1.70 / tan(89°) = 0.03m -> clamped to the 2m floor
        assertEquals(2, RangefinderEngine.calculateRange(-89f).distanceMeters)
        // Nearly straight up: indeterminate
        assertEquals(9999, RangefinderEngine.calculateRange(89f).distanceMeters)
    }

    @Test
    fun testNaNDivisionByZeroAndExtremeValues() {
        // NaN pitch is treated as level
        val nanPitch = RangefinderEngine.calculateRange(Float.NaN)
        assertEquals(9999, nanPitch.distanceMeters)
        assertEquals(RangeConfidence.NO_LOCK, nanPitch.confidence)

        // +Inf pitch looks straight up -> NO LOCK at max range
        val infPitch = RangefinderEngine.calculateRange(Float.POSITIVE_INFINITY)
        assertEquals(9999, infPitch.distanceMeters)
        assertEquals(RangeConfidence.NO_LOCK, infPitch.confidence)

        // -Inf pitch has degenerate tangent -> NO LOCK at max range
        val negInfPitch = RangefinderEngine.calculateRange(Float.NEGATIVE_INFINITY)
        assertEquals(9999, negInfPitch.distanceMeters)
        assertEquals(RangeConfidence.NO_LOCK, negInfPitch.confidence)

        // Invalid observer heights fall back to the 1.70m default
        assertEquals(
            RangefinderEngine.calculateRange(-5f).distanceMeters,
            RangefinderEngine.calculateRange(-5f, Float.NaN).distanceMeters
        )
        assertEquals(
            RangefinderEngine.calculateRange(-5f).distanceMeters,
            RangefinderEngine.calculateRange(-5f, -3f).distanceMeters
        )

        // Infinite height saturates at maximum range instead of throwing
        assertEquals(9999, RangefinderEngine.calculateRange(-5f, Float.POSITIVE_INFINITY).distanceMeters)
    }

    @Test
    fun testLevelUprightAttitudeExtraction() {
        val attitude = CameraAttitude.fromRotationMatrix(uprightLandscapeMatrix(depressionDeg = 0f))
        assertTrue("Pitch should be ~0°, was ${attitude.pitchDegrees}", abs(attitude.pitchDegrees) < 0.01f)
        assertTrue("Roll should be ~0°, was ${attitude.rollDegrees}", abs(attitude.rollDegrees) < 0.01f)
    }

    @Test
    fun testDepressionTiltMapsToNegativeCameraPitch() {
        val fiveDown = CameraAttitude.fromRotationMatrix(uprightLandscapeMatrix(depressionDeg = 5f))
        assertTrue(abs(fiveDown.pitchDegrees + 5f) < 0.01f)
        assertTrue(abs(fiveDown.rollDegrees) < 0.01f)

        val thirtyDown = CameraAttitude.fromRotationMatrix(uprightLandscapeMatrix(depressionDeg = 30f))
        assertTrue(abs(thirtyDown.pitchDegrees + 30f) < 0.01f)
    }

    @Test
    fun testUpwardAimMapsToPositiveCameraPitch() {
        val tenUp = CameraAttitude.fromRotationMatrix(uprightLandscapeMatrix(depressionDeg = -10f))
        assertTrue(abs(tenUp.pitchDegrees - 10f) < 0.01f)
    }

    @Test
    fun testRollSignPositiveWhenRightEdgeDown() {
        val rolled = CameraAttitude.fromRotationMatrix(rolledLandscapeMatrix(rollDeg = 10f))
        assertTrue("Roll should be ~+10°, was ${rolled.rollDegrees}", abs(rolled.rollDegrees - 10f) < 0.01f)
        assertTrue(abs(rolled.pitchDegrees) < 0.01f)

        val counterRolled = CameraAttitude.fromRotationMatrix(rolledLandscapeMatrix(rollDeg = -25f))
        assertTrue(abs(counterRolled.rollDegrees + 25f) < 0.01f)
    }

    @Test
    fun testPanDoesNotAffectCameraPitchOrRoll() {
        // Panning 90° to face east must not disturb the camera-frame pitch or roll
        // (the pre-fix device-frame roll swung with yaw because its axis pointed into the ground)
        val eastFacing = CameraAttitude.fromRotationMatrix(eastFacingLandscapeMatrix())
        assertTrue(abs(eastFacing.pitchDegrees) < 0.01f)
        assertTrue(abs(eastFacing.rollDegrees) < 0.01f)
    }

    @Test
    fun testCombinedTiltAndRollAttitude() {
        val combined = CameraAttitude.fromRotationMatrix(
            rolledLandscapeMatrix(rollDeg = 20f, depressionDeg = 15f)
        )
        // Tilt about the rolled right axis couples slightly into the extracted angles
        assertTrue("Pitch should be ~-15°, was ${combined.pitchDegrees}", abs(combined.pitchDegrees + 15f) < 1.5f)
        assertTrue("Roll should be ~+20°, was ${combined.rollDegrees}", abs(combined.rollDegrees - 20f) < 1.5f)
    }

    @Test
    fun testAttitudeExtractionDegenerateInputSafety() {
        val nanMatrix = floatArrayOf(
            Float.NaN, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
        val nanResult = CameraAttitude.fromRotationMatrix(nanMatrix)
        assertEquals(0f, nanResult.pitchDegrees)
        assertEquals(0f, nanResult.rollDegrees)
        assertEquals(0f, nanResult.azimuthDegrees)

        val tooShort = floatArrayOf(1f, 0f, 0f, 0f, 1f)
        val shortResult = CameraAttitude.fromRotationMatrix(tooShort)
        assertEquals(0f, shortResult.pitchDegrees)
        assertEquals(0f, shortResult.rollDegrees)
        assertEquals(0f, shortResult.azimuthDegrees)
    }

    @Test
    fun testCameraAttitudeExtractsAzimuthForCardinals() {
        // Facing North (0°)
        val north = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(0f))
        assertTrue("North azimuth should be ~0°, was ${north.azimuthDegrees}", abs(north.azimuthDegrees) < 0.01f)
        assertTrue("North pitch should be ~0°, was ${north.pitchDegrees}", abs(north.pitchDegrees) < 0.01f)
        assertTrue("North roll should be ~0°, was ${north.rollDegrees}", abs(north.rollDegrees) < 0.01f)

        // Facing East (90°)
        val east = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(90f))
        assertTrue("East azimuth should be ~90°, was ${east.azimuthDegrees}", abs(east.azimuthDegrees - 90f) < 0.01f)
        assertTrue("East pitch should be ~0°, was ${east.pitchDegrees}", abs(east.pitchDegrees) < 0.01f)
        assertTrue("East roll should be ~0°, was ${east.rollDegrees}", abs(east.rollDegrees) < 0.01f)

        // Facing South (180°)
        val south = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(180f))
        assertTrue("South azimuth should be ~180°, was ${south.azimuthDegrees}", abs(south.azimuthDegrees - 180f) < 0.01f)
        assertTrue("South pitch should be ~0°, was ${south.pitchDegrees}", abs(south.pitchDegrees) < 0.01f)
        assertTrue("South roll should be ~0°, was ${south.rollDegrees}", abs(south.rollDegrees) < 0.01f)

        // Facing West (270°)
        val west = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(270f))
        assertTrue("West azimuth should be ~270°, was ${west.azimuthDegrees}", abs(west.azimuthDegrees - 270f) < 0.01f)
        assertTrue("West pitch should be ~0°, was ${west.pitchDegrees}", abs(west.pitchDegrees) < 0.01f)
        assertTrue("West roll should be ~0°, was ${west.rollDegrees}", abs(west.rollDegrees) < 0.01f)
    }

    @Test
    fun testUprightHorizonCrossingKeepsAzimuthStableWithout180DegreeFlip() {
        // Regression test for the upright horizon-crossing bug: verify that when facing North (0°)
        // in upright landscape, tilting from depressionDeg = 15f (looking down -15°), through 0f
        // (level 0°), to -15f (looking up +15°) keeps azimuthDegrees at ~0° (within 0.01f) and NEVER flips to 180°!
        val down15 = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(0f, depressionDeg = 15f))
        assertTrue("Pitch should be -15°, was ${down15.pitchDegrees}", abs(down15.pitchDegrees + 15f) < 0.01f)
        assertTrue("Azimuth at 15° depression should be ~0°, was ${down15.azimuthDegrees}", abs(down15.azimuthDegrees) < 0.01f)

        val down5 = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(0f, depressionDeg = 5f))
        assertTrue("Azimuth at 5° depression should be ~0°, was ${down5.azimuthDegrees}", abs(down5.azimuthDegrees) < 0.01f)

        val level = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(0f, depressionDeg = 0f))
        assertTrue("Pitch should be 0°, was ${level.pitchDegrees}", abs(level.pitchDegrees) < 0.01f)
        assertTrue("Azimuth at level should be ~0°, was ${level.azimuthDegrees}", abs(level.azimuthDegrees) < 0.01f)

        val up5 = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(0f, depressionDeg = -5f))
        assertTrue("Azimuth at 5° elevation should be ~0°, was ${up5.azimuthDegrees}", abs(up5.azimuthDegrees) < 0.01f)

        val up15 = CameraAttitude.fromRotationMatrix(uprightLandscapeHeadingMatrix(0f, depressionDeg = -15f))
        assertTrue("Pitch should be +15°, was ${up15.pitchDegrees}", abs(up15.pitchDegrees - 15f) < 0.01f)
        assertTrue("Azimuth at 15° elevation should be ~0° (never 180°), was ${up15.azimuthDegrees}", abs(up15.azimuthDegrees) < 0.01f)
    }

    /**
     * Builds a row-major screen-aligned device-to-world rotation matrix from orthonormal
     * column vectors (screen right, screen up, toward the user).
     */
    private fun matrixFromColumns(right: FloatArray, up: FloatArray, back: FloatArray): FloatArray =
        floatArrayOf(
            right[0], up[0], back[0],
            right[1], up[1], back[1],
            right[2], up[2], back[2]
        )

    /**
     * Simulates the phone held upright in landscape facing north (world: X east, Y north,
     * Z up) with the back camera depressed by [depressionDeg] degrees. World forward is
     * (0, cos θ, -sin θ).
     */
    private fun uprightLandscapeMatrix(depressionDeg: Float): FloatArray {
        val rad = Math.toRadians(depressionDeg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        val right = floatArrayOf(1f, 0f, 0f)
        val up = floatArrayOf(0f, s, c)
        val back = floatArrayOf(0f, -c, s)
        return matrixFromColumns(right, up, back)
    }

    /**
     * Simulates the phone held upright in landscape facing heading [headingDeg] (0 = North,
     * 90 = East, 180 = South, 270 = West) with depression [depressionDeg] (positive = looking down).
     */
    private fun uprightLandscapeHeadingMatrix(headingDeg: Float, depressionDeg: Float = 0f): FloatArray {
        val hRad = Math.toRadians(headingDeg.toDouble())
        val dRad = Math.toRadians(depressionDeg.toDouble())
        val cosH = cos(hRad).toFloat()
        val sinH = sin(hRad).toFloat()
        val cosD = cos(dRad).toFloat()
        val sinD = sin(dRad).toFloat()

        // Forward vector in world coordinates:
        // horizontal components directed along heading H, vertical depression along -Z
        val forwardEast = sinH * cosD
        val forwardNorth = cosH * cosD
        val forwardUp = -sinD

        // Device back vector = -forward
        val back = floatArrayOf(-forwardEast, -forwardNorth, -forwardUp)

        // Device right vector in horizontal plane (perpendicular to heading):
        val right = floatArrayOf(cosH, -sinH, 0f)

        // Device up vector = back x right (orthonormal right-handed basis):
        val up = floatArrayOf(
            back[1] * right[2] - back[2] * right[1],
            back[2] * right[0] - back[0] * right[2],
            back[0] * right[1] - back[1] * right[0]
        )
        return matrixFromColumns(right, up, back)
    }

    /**
     * Simulates the level upright landscape phone rolled about its optical axis by
     * [rollDeg] degrees (positive = right edge down) and depressed by [depressionDeg].
     */
    private fun rolledLandscapeMatrix(rollDeg: Float, depressionDeg: Float = 0f): FloatArray {
        val roll = Math.toRadians(rollDeg.toDouble())
        val tilt = Math.toRadians(depressionDeg.toDouble())
        val cr = cos(roll).toFloat()
        val sr = sin(roll).toFloat()
        val ct = cos(tilt).toFloat()
        val st = sin(tilt).toFloat()
        // Roll about the forward axis, then depress about the rolled right axis
        val right = floatArrayOf(cr, 0f, -sr)
        val forward = floatArrayOf(-sr * st, ct, -cr * st)
        val up = floatArrayOf(sr * ct, st, cr * ct)
        val back = floatArrayOf(-forward[0], -forward[1], -forward[2])
        return matrixFromColumns(right, up, back)
    }

    /**
     * Simulates the level upright landscape phone panned 90° to face east.
     */
    private fun eastFacingLandscapeMatrix(): FloatArray {
        val right = floatArrayOf(0f, -1f, 0f)
        val up = floatArrayOf(0f, 0f, 1f)
        val back = floatArrayOf(-1f, 0f, 0f)
        return matrixFromColumns(right, up, back)
    }
}
