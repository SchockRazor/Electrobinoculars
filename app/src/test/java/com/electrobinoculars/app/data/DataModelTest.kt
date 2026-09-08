package com.electrobinoculars.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying domain contracts and data models for Electrobinoculars.
 */
class DataModelTest {

    @Test
    fun testVisionModeValuesAndCodes() {
        assertEquals("STD", VisionMode.STANDARD.code)
        assertEquals("NVG", VisionMode.NIGHT_VISION.code)
        assertEquals("FLIR", VisionMode.THERMAL.code)
        assertEquals("CRT", VisionMode.TACTICAL_MONO.code)

        assertEquals("STANDARD", VisionMode.STANDARD.label)
        assertEquals("NIGHT VISION", VisionMode.NIGHT_VISION.label)
        assertEquals("THERMAL IR", VisionMode.THERMAL.label)
        assertEquals("TACTICAL MONO", VisionMode.TACTICAL_MONO.label)

        assertEquals(4, VisionMode.entries.size)
        assertEquals(VisionMode.STANDARD, VisionMode.valueOf("STANDARD"))
        assertEquals(VisionMode.NIGHT_VISION, VisionMode.valueOf("NIGHT_VISION"))
        assertEquals(VisionMode.THERMAL, VisionMode.valueOf("THERMAL"))
        assertEquals(VisionMode.TACTICAL_MONO, VisionMode.valueOf("TACTICAL_MONO"))
    }

    @Test
    fun testZoomStateDataFormatting() {
        val defaultZoom = ZoomStateData()
        assertEquals(1.0f, defaultZoom.zoomRatio, 0.001f)
        assertEquals(1.0f, defaultZoom.minZoomRatio, 0.001f)
        assertEquals(8.0f, defaultZoom.maxZoomRatio, 0.001f)
        assertEquals(0.0f, defaultZoom.linearZoom, 0.001f)
        assertEquals("[ 1.0x ]", defaultZoom.formattedMagnification)

        val customZoom = ZoomStateData(
            zoomRatio = 3.45f,
            minZoomRatio = 1.0f,
            maxZoomRatio = 10.0f,
            linearZoom = 0.35f
        )
        assertEquals("[ 3.5x ]", customZoom.formattedMagnification)

        val highZoom = ZoomStateData(zoomRatio = 10.0f)
        assertEquals("[ 10.0x ]", highZoom.formattedMagnification)
    }

    @Test
    fun testZoomStateDataFormattingEdgeCases() {
        // NaN should format to safe 1.0x
        val nanZoom = ZoomStateData(zoomRatio = Float.NaN)
        assertEquals("[ 1.0x ]", nanZoom.formattedMagnification)

        // Negative or zero zoom should format to safe 1.0x
        val zeroZoom = ZoomStateData(zoomRatio = 0.0f)
        assertEquals("[ 1.0x ]", zeroZoom.formattedMagnification)

        val negativeZoom = ZoomStateData(zoomRatio = -4.2f)
        assertEquals("[ 1.0x ]", negativeZoom.formattedMagnification)

        // Infinite zoom should format to safe 1.0x
        val infZoom = ZoomStateData(zoomRatio = Float.POSITIVE_INFINITY)
        assertEquals("[ 1.0x ]", infZoom.formattedMagnification)
    }

    @Test
    fun testCalculateLinearZoomCalculationsAndGuards() {
        // Standard progression [1.0, 9.0]
        assertEquals(0.0f, ZoomStateData.calculateLinearZoom(1.0f, 1.0f, 9.0f), 0.001f)
        assertEquals(1.0f, ZoomStateData.calculateLinearZoom(9.0f, 1.0f, 9.0f), 0.001f)
        assertEquals(0.5f, ZoomStateData.calculateLinearZoom(5.0f, 1.0f, 9.0f), 0.001f)

        // Out-of-bounds clamping
        assertEquals(0.0f, ZoomStateData.calculateLinearZoom(0.5f, 1.0f, 9.0f), 0.001f)
        assertEquals(1.0f, ZoomStateData.calculateLinearZoom(15.0f, 1.0f, 9.0f), 0.001f)

        // Zero range (division-by-zero protection)
        assertEquals(0.0f, ZoomStateData.calculateLinearZoom(5.0f, 5.0f, 5.0f), 0.001f)

        // Inverted range (min > max)
        assertEquals(0.0f, ZoomStateData.calculateLinearZoom(5.0f, 10.0f, 2.0f), 0.001f)

        // NaN inputs
        assertEquals(0.0f, ZoomStateData.calculateLinearZoom(Float.NaN, 1.0f, 9.0f), 0.001f)
        assertEquals(0.0f, ZoomStateData.calculateLinearZoom(5.0f, Float.NaN, 9.0f), 0.001f)
        assertEquals(0.0f, ZoomStateData.calculateLinearZoom(5.0f, 1.0f, Float.NaN), 0.001f)
    }

    @Test
    fun testCalculateZoomRatioCalculationsAndGuards() {
        // Standard progression [1.0, 9.0]
        assertEquals(1.0f, ZoomStateData.calculateZoomRatio(0.0f, 1.0f, 9.0f), 0.001f)
        assertEquals(9.0f, ZoomStateData.calculateZoomRatio(1.0f, 1.0f, 9.0f), 0.001f)
        assertEquals(5.0f, ZoomStateData.calculateZoomRatio(0.5f, 1.0f, 9.0f), 0.001f)

        // Out-of-bounds clamping
        assertEquals(1.0f, ZoomStateData.calculateZoomRatio(-0.5f, 1.0f, 9.0f), 0.001f)
        assertEquals(9.0f, ZoomStateData.calculateZoomRatio(1.8f, 1.0f, 9.0f), 0.001f)

        // NaN linear zoom
        assertEquals(1.0f, ZoomStateData.calculateZoomRatio(Float.NaN, 1.0f, 9.0f), 0.001f)

        // Min zoom less than 1.0 coerced to 1.0
        assertEquals(1.0f, ZoomStateData.calculateZoomRatio(0.0f, 0.2f, 8.0f), 0.001f)

        // Inverted bounds (min > max)
        val invertedRatio = ZoomStateData.calculateZoomRatio(0.5f, 10.0f, 2.0f)
        assertEquals(10.0f, invertedRatio, 0.001f)
    }

    @Test
    fun testSensorTelemetryDefaults() {
        val telemetry = SensorTelemetry()
        assertEquals(0.0f, telemetry.headingDegrees, 0.001f)
        assertEquals("N", telemetry.cardinalDirection)
        assertEquals(0.0f, telemetry.pitch, 0.001f)
        assertEquals(0.0f, telemetry.roll, 0.001f)
        assertEquals(100, telemetry.estimatedDistanceMeters)
        assertTrue(telemetry.isAvailable)
    }

    @Test
    fun testSensorTelemetryMutationsAndEquality() {
        val telemetry1 = SensorTelemetry(
            headingDegrees = 180.0f,
            cardinalDirection = "S",
            pitch = -15.5f,
            roll = 4.2f,
            estimatedDistanceMeters = 340,
            isAvailable = true
        )
        val telemetry2 = telemetry1.copy(isAvailable = false)

        assertFalse(telemetry2.isAvailable)
        assertEquals(180.0f, telemetry2.headingDegrees, 0.001f)
        assertEquals("S", telemetry2.cardinalDirection)
        assertEquals(-15.5f, telemetry2.pitch, 0.001f)
        assertEquals(4.2f, telemetry2.roll, 0.001f)
        assertEquals(340, telemetry2.estimatedDistanceMeters)

        val telemetryCopy = telemetry1.copy()
        assertEquals(telemetry1, telemetryCopy)
        assertEquals(telemetry1.hashCode(), telemetryCopy.hashCode())
    }

    @Test
    fun testElectrobinocularsUiStateDefaultsAndCopy() {
        val initialState = ElectrobinocularsUiState()
        assertEquals(VisionMode.STANDARD, initialState.visionMode)
        assertFalse(initialState.hasCameraPermission)
        assertTrue(initialState.isCameraHardwareAvailable)
        assertFalse(initialState.isUsingFallbackTestPattern)

        val updatedState = initialState.copy(
            visionMode = VisionMode.NIGHT_VISION,
            hasCameraPermission = true,
            isUsingFallbackTestPattern = false
        )
        assertEquals(VisionMode.NIGHT_VISION, updatedState.visionMode)
        assertTrue(updatedState.hasCameraPermission)
        assertFalse(updatedState.isUsingFallbackTestPattern)

        val fallbackState = updatedState.copy(
            isCameraHardwareAvailable = false,
            isUsingFallbackTestPattern = true
        )
        assertFalse(fallbackState.isCameraHardwareAvailable)
        assertTrue(fallbackState.isUsingFallbackTestPattern)
    }
}
