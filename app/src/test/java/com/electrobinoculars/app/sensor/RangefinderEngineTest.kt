package com.electrobinoculars.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for RangefinderEngine stadiametric math and cardinal heading conversions.
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
    fun testRangefinderDistanceBounds() {
        // Looking down at steep angle (e.g. 60 deg) at 1x zoom -> closer distance
        val closeRange = RangefinderEngine.calculateEstimatedRangeMeters(60f, 1.0f)
        assertTrue("Close range ($closeRange) should be in 8..100m", closeRange in 8..100)

        // Looking near horizon (e.g. 2 deg) at 10x zoom -> distant range
        val farRange = RangefinderEngine.calculateEstimatedRangeMeters(2f, 10.0f)
        assertTrue("Far range ($farRange) should be > 500m", farRange > 500)

        // Clamping at boundaries
        val extremeClose = RangefinderEngine.calculateEstimatedRangeMeters(89f, 0.5f)
        assertTrue("Extreme close ($extremeClose) should clamp to >= 8", extremeClose >= 8)

        val extremeFar = RangefinderEngine.calculateEstimatedRangeMeters(0.01f, 100f)
        assertTrue("Extreme far ($extremeFar) should clamp to <= 9999", extremeFar <= 9999)
    }

    @Test
    fun testRangefinderPitchSymmetryAndMonotonicity() {
        // Inverted pitch (looking down vs up) uses absolute pitch
        val downRange = RangefinderEngine.calculateEstimatedRangeMeters(-15f, 2.0f)
        val upRange = RangefinderEngine.calculateEstimatedRangeMeters(15f, 2.0f)
        assertEquals(downRange, upRange)

        // Monotonicity with pitch: Steeper tilt -> closer distance
        val steepAngle = RangefinderEngine.calculateEstimatedRangeMeters(50f, 2.0f)
        val shallowAngle = RangefinderEngine.calculateEstimatedRangeMeters(10f, 2.0f)
        assertTrue(steepAngle < shallowAngle)

        // Monotonicity with zoom: Higher zoom -> deeper focal distance lock
        val zoom1x = RangefinderEngine.calculateEstimatedRangeMeters(20f, 1.0f)
        val zoom5x = RangefinderEngine.calculateEstimatedRangeMeters(20f, 5.0f)
        assertTrue(zoom5x > zoom1x)
    }

    @Test
    fun testRangefinderNaNDivisionByZeroAndExtremeValues() {
        // NaN pitch tilt
        val nanPitchRange = RangefinderEngine.calculateEstimatedRangeMeters(Float.NaN, 2.0f)
        assertTrue(nanPitchRange in 8..9999)

        // NaN zoom ratio
        val nanZoomRange = RangefinderEngine.calculateEstimatedRangeMeters(15f, Float.NaN)
        assertTrue(nanZoomRange in 8..9999)

        // Both NaN
        val bothNanRange = RangefinderEngine.calculateEstimatedRangeMeters(Float.NaN, Float.NaN)
        assertTrue(bothNanRange in 8..9999)

        // Zero pitch tilt (horizontal baseline)
        val zeroPitchRange = RangefinderEngine.calculateEstimatedRangeMeters(0.0f, 1.0f)
        assertTrue(zeroPitchRange in 8..9999)

        // Negative zoom coerced to 1.0f
        val negZoomRange = RangefinderEngine.calculateEstimatedRangeMeters(25f, -3.0f)
        val oneZoomRange = RangefinderEngine.calculateEstimatedRangeMeters(25f, 1.0f)
        assertEquals(oneZoomRange, negZoomRange)

        // Infinite pitch tilt
        val infPitchRange = RangefinderEngine.calculateEstimatedRangeMeters(Float.POSITIVE_INFINITY, 2.0f)
        assertTrue(infPitchRange in 8..9999)

        // Infinite zoom
        val infZoomRange = RangefinderEngine.calculateEstimatedRangeMeters(20f, Float.POSITIVE_INFINITY)
        assertTrue(infZoomRange in 8..9999)
    }
}
