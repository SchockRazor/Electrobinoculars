package com.electrobinoculars.app.ui.shaders

import com.electrobinoculars.app.data.VisionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for VisionShaders optical filters, color matrices, and mode mappings.
 */
class VisionShadersTest {

    @Test
    fun testNightVisionMatrixValues() {
        val matrix = VisionShaders.NightVisionColorMatrix
        val values = matrix.values

        // 4x5 matrix has 20 elements
        assertEquals(20, values.size)

        // Green channel gain boosted significantly higher than red and blue
        val greenGain = values[6] // row 1, col 1 (green input to green output)
        val redGain = values[0]   // row 0, col 0 (red input to red output)
        val blueGain = values[12] // row 2, col 2 (blue input to blue output)
        assertTrue("Green gain ($greenGain) must be greater than red ($redGain)", greenGain > redGain)
        assertTrue("Green gain ($greenGain) must be greater than blue ($blueGain)", greenGain > blueGain)

        // Green channel offset boost
        val greenOffset = values[9] // row 1, col 4
        assertTrue("Green offset ($greenOffset) must be positive phosphor bias", greenOffset > 0f)

        // Alpha channel preserved
        assertEquals(1.0f, values[18], 0.001f) // row 3, col 3
    }

    @Test
    fun testTacticalMonoMatrixValues() {
        val matrix = VisionShaders.TacticalMonoColorMatrix
        val values = matrix.values

        assertEquals(20, values.size)

        // R, G, B channels should receive identical luminance weights for true monochrome
        val rWeightR = values[0]
        val rWeightG = values[5]
        val rWeightB = values[10]
        assertEquals(rWeightR, rWeightG, 0.001f)
        assertEquals(rWeightR, rWeightB, 0.001f)

        val gWeightR = values[1]
        val gWeightG = values[6]
        val gWeightB = values[11]
        assertEquals(gWeightR, gWeightG, 0.001f)
        assertEquals(gWeightR, gWeightB, 0.001f)

        val bWeightR = values[2]
        val bWeightG = values[7]
        val bWeightB = values[12]
        assertEquals(bWeightR, bWeightG, 0.001f)
        assertEquals(bWeightR, bWeightB, 0.001f)

        // Alpha preserved
        assertEquals(1.0f, values[18], 0.001f)
    }

    @Test
    fun testThermalFallbackMatrixValues() {
        val matrix = VisionShaders.ThermalFallbackColorMatrix
        val values = matrix.values

        assertEquals(20, values.size)
        // Red channel offset (heat glow)
        assertTrue(values[4] > 0f)
        // Alpha preserved
        assertEquals(1.0f, values[18], 0.001f)
    }

    @Test
    fun testColorFiltersForModes() {
        // Standard mode must not apply a color filter
        assertNull(VisionShaders.getColorFilterForMode(VisionMode.STANDARD))

        // Night Vision must provide a color matrix filter
        assertNotNull(VisionShaders.getColorFilterForMode(VisionMode.NIGHT_VISION))

        // Tactical Mono must provide a color matrix filter
        assertNotNull(VisionShaders.getColorFilterForMode(VisionMode.TACTICAL_MONO))
    }
}
