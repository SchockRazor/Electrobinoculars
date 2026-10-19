package com.electrobinoculars.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for signal processing and attitude telemetry filters:
 * [ExponentialMovingAverage], [CircularExponentialMovingAverage], and [DeadZoneFilter].
 */
class SensorFilterTest {

    // =========================================================================
    // ExponentialMovingAverage Tests
    // =========================================================================

    @Test
    fun testEmaInitialSamplePassThrough() {
        val filter = ExponentialMovingAverage(alpha = 0.2f)
        val initialValue = 42.5f
        val result = filter.update(initialValue)
        assertEquals(
            "Initial sample must pass through unchanged without filter lag",
            initialValue,
            result,
            0.001f
        )
    }

    @Test
    fun testEmaStepResponseConvergence() {
        // Step response with alpha = 0.5:
        // step 0: y_0 = 0.0
        // step 1: y_1 = 0.0 + 0.5 * (10.0 - 0.0) = 5.0
        // step 2: y_2 = 5.0 + 0.5 * (10.0 - 5.0) = 7.5
        // step 3: y_3 = 7.5 + 0.5 * (10.0 - 7.5) = 8.75
        val filter = ExponentialMovingAverage(alpha = 0.5f)
        filter.update(0.0f)

        val step1 = filter.update(10.0f)
        assertEquals(5.0f, step1, 0.001f)

        val step2 = filter.update(10.0f)
        assertEquals(7.5f, step2, 0.001f)

        val step3 = filter.update(10.0f)
        assertEquals(8.75f, step3, 0.001f)

        // Monotonic convergence toward steady-state
        var current = step3
        for (i in 0 until 20) {
            current = filter.update(10.0f)
        }
        assertEquals(
            "After multiple iterations EMA must converge to the step target",
            10.0f,
            current,
            0.001f
        )
    }

    @Test
    fun testEmaRejectsNaNAndInfinity() {
        val filter = ExponentialMovingAverage(alpha = 0.25f)
        filter.update(15.0f)

        // NaN must be rejected and preserve the previous valid state
        val afterNaN = filter.update(Float.NaN)
        assertEquals(15.0f, afterNaN, 0.001f)

        // +Infinity must be rejected
        val afterPosInf = filter.update(Float.POSITIVE_INFINITY)
        assertEquals(15.0f, afterPosInf, 0.001f)

        // -Infinity must be rejected
        val afterNegInf = filter.update(Float.NEGATIVE_INFINITY)
        assertEquals(15.0f, afterNegInf, 0.001f)

        // Next valid value should smoothly update from the preserved state (15.0)
        // 15.0 + 0.25 * (25.0 - 15.0) = 17.5
        val nextValid = filter.update(25.0f)
        assertEquals(17.5f, nextValid, 0.001f)
    }

    @Test
    fun testEmaReset() {
        val filter = ExponentialMovingAverage(alpha = 0.1f)
        filter.update(100.0f)

        filter.reset()

        // After reset, next value should pass through directly as a new initial sample
        val freshSample = 5.0f
        val result = filter.update(freshSample)
        assertEquals(
            "First sample after reset() must pass through directly without history",
            freshSample,
            result,
            0.001f
        )
    }

    // =========================================================================
    // CircularExponentialMovingAverage Tests
    // =========================================================================

    @Test
    fun testCircularEmaWraparoundAcrossZeroAnd360() {
        // Test smoothing across 0°/360° discontinuity:
        // Heading starts at 355° and updates with 5°.
        // Shortest angular difference is +10° (crossing 360°/0°), NOT -350°.
        // With alpha = 0.5: delta = +10° * 0.5 = +5°, landing at 360° = 0°.
        val filter = CircularExponentialMovingAverage(alpha = 0.5f)
        filter.update(355.0f)

        val blended = filter.update(5.0f)
        // Normalize blended angle distance from 0°
        val distFromZero = if (blended > 180f) 360f - blended else blended

        assertTrue(
            "Blended angle across 0°/360° must stay near 0°/360° (was $blended)",
            distFromZero <= 10.0f
        )
        assertTrue(
            "Blended angle must never swing the long way around through 180° (was $blended)",
            abs(blended - 180.0f) > 90.0f
        )
    }

    @Test
    fun testCircularEmaWraparoundReverse() {
        // Start at 5° and update with 355°
        // Shortest angular difference is -10° (crossing 0°/360°).
        // With alpha = 0.5: delta = -5°, landing at 0°.
        val filter = CircularExponentialMovingAverage(alpha = 0.5f)
        filter.update(5.0f)

        val blended = filter.update(355.0f)
        val distFromZero = if (blended > 180f) 360f - blended else blended

        assertTrue(
            "Blended angle across 0°/360° must stay near 0°/360° (was $blended)",
            distFromZero <= 10.0f
        )
        assertTrue(
            "Blended angle must never swing the long way around through 180° (was $blended)",
            abs(blended - 180.0f) > 90.0f
        )
    }

    @Test
    fun testCircularEmaRejectsNaNAndInfinity() {
        val filter = CircularExponentialMovingAverage(alpha = 0.2f)
        filter.update(90.0f)

        val afterNaN = filter.update(Float.NaN)
        assertEquals(90.0f, afterNaN, 0.001f)

        val afterPosInf = filter.update(Float.POSITIVE_INFINITY)
        assertEquals(90.0f, afterPosInf, 0.001f)

        val afterNegInf = filter.update(Float.NEGATIVE_INFINITY)
        assertEquals(90.0f, afterNegInf, 0.001f)
    }

    @Test
    fun testCircularEmaReset() {
        val filter = CircularExponentialMovingAverage(alpha = 0.15f)
        filter.update(270.0f)

        filter.reset()

        // After reset, next angle should pass through directly
        val newAngle = 45.0f
        val result = filter.update(newAngle)
        assertEquals(
            "First angle after reset() must pass through directly without angular blending",
            newAngle,
            result,
            0.001f
        )
    }

    // =========================================================================
    // DeadZoneFilter Tests
    // =========================================================================

    @Test
    fun testDeadZoneFilterEnteringAndHoldingLastStableValue() {
        val threshold = 1.5f
        val hysteresis = 0.5f
        val filter = DeadZoneFilter(deadZoneThreshold = threshold, hysteresis = hysteresis)

        assertFalse("DeadZoneFilter initially not in dead zone", filter.isInDeadZone)

        // Establish an initial stable value outside the dead zone (|5.0| >= 1.5 + 0.5)
        val stable = filter.update(5.0f)
        assertEquals(5.0f, stable, 0.001f)
        assertFalse(filter.isInDeadZone)

        // Input falls inside dead zone (|1.0| < 1.5)
        val inZone = filter.update(1.0f)
        assertTrue("Filter should enter dead zone when |value| < threshold", filter.isInDeadZone)
        assertEquals(
            "Filter must hold last stable value while inside dead zone",
            5.0f,
            inZone,
            0.001f
        )

        // Further jitter inside dead zone (|0.2| < 1.5) continues to hold last stable value
        val jitter = filter.update(0.2f)
        assertTrue(filter.isInDeadZone)
        assertEquals(5.0f, jitter, 0.001f)
    }

    @Test
    fun testDeadZoneFilterHysteresisExitRequirement() {
        val threshold = 1.5f
        val hysteresis = 0.5f
        val filter = DeadZoneFilter(deadZoneThreshold = threshold, hysteresis = hysteresis)

        // Set stable value, then enter dead zone
        filter.update(4.0f)
        filter.update(0.5f)
        assertTrue(filter.isInDeadZone)

        // Value increases to 1.8f: this is >= threshold (1.5f), but < threshold + hysteresis (2.0f)
        // Must remain in dead zone due to hysteresis!
        val inHysteresisBand = filter.update(1.8f)
        assertTrue(
            "Filter must stay in dead zone until value reaches threshold + hysteresis",
            filter.isInDeadZone
        )
        assertEquals(4.0f, inHysteresisBand, 0.001f)

        // Negative direction inside hysteresis band: -1.9f has magnitude < 2.0f
        val negHysteresisBand = filter.update(-1.9f)
        assertTrue(filter.isInDeadZone)
        assertEquals(4.0f, negHysteresisBand, 0.001f)

        // Value reaches threshold + hysteresis (2.0f): exits dead zone
        val exitPositive = filter.update(2.1f)
        assertFalse("Filter must exit dead zone when |value| >= threshold + hysteresis", filter.isInDeadZone)
        assertEquals(2.1f, exitPositive, 0.001f)

        // Now last stable value is 2.1f; re-enter dead zone with negative small angle
        filter.update(-0.3f)
        assertTrue(filter.isInDeadZone)
        assertEquals(2.1f, filter.update(-0.8f), 0.001f)

        // Exit in negative direction: -2.2f has magnitude >= 2.0f
        val exitNegative = filter.update(-2.2f)
        assertFalse(filter.isInDeadZone)
        assertEquals(-2.2f, exitNegative, 0.001f)
    }

    @Test
    fun testDeadZoneFilterNaNAndInfinityHandling() {
        val filter = DeadZoneFilter(deadZoneThreshold = 1.5f, hysteresis = 0.5f)
        filter.update(3.0f)

        // NaN input must not crash or corrupt stable value
        val nanResult = filter.update(Float.NaN)
        assertEquals(3.0f, nanResult, 0.001f)

        // Infinity inputs must not crash or corrupt stable value
        val posInfResult = filter.update(Float.POSITIVE_INFINITY)
        assertEquals(3.0f, posInfResult, 0.001f)

        val negInfResult = filter.update(Float.NEGATIVE_INFINITY)
        assertEquals(3.0f, negInfResult, 0.001f)
    }

    @Test
    fun testDeadZoneFilterReset() {
        val filter = DeadZoneFilter(deadZoneThreshold = 1.5f, hysteresis = 0.5f)
        filter.update(4.0f)
        filter.update(0.5f)
        assertTrue(filter.isInDeadZone)

        filter.reset()

        assertFalse("Filter isInDeadZone must be false after reset()", filter.isInDeadZone)
    }
}
