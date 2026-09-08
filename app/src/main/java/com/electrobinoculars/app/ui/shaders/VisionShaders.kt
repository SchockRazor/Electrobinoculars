package com.electrobinoculars.app.ui.shaders

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.electrobinoculars.app.data.VisionMode

/**
 * Optical Filters and AGSL Shaders for Electrobinoculars Vision Modes.
 */
object VisionShaders {

    // AGSL Shader for Thermal IR (Ironbow / False-Color Heatmap)
    private const val THERMAL_AGSL = """
        uniform shader uContent;
        
        vec4 main(vec2 fragCoord) {
            vec4 color = uContent.eval(fragCoord);
            // Calculate luminance
            float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            
            // False-color thermal mapping (Black -> Blue -> Magenta -> Red -> Yellow -> White)
            vec3 thermal;
            if (lum < 0.2) {
                // Black to Navy/Blue
                thermal = mix(vec3(0.0, 0.0, 0.15), vec3(0.0, 0.2, 0.8), lum / 0.2);
            } else if (lum < 0.45) {
                // Blue to Purple/Magenta
                thermal = mix(vec3(0.0, 0.2, 0.8), vec3(0.7, 0.0, 0.7), (lum - 0.2) / 0.25);
            } else if (lum < 0.7) {
                // Magenta to Bright Red/Orange
                thermal = mix(vec3(0.7, 0.0, 0.7), vec3(1.0, 0.3, 0.0), (lum - 0.45) / 0.25);
            } else if (lum < 0.9) {
                // Red/Orange to Bright Yellow
                thermal = mix(vec3(1.0, 0.3, 0.0), vec3(1.0, 1.0, 0.1), (lum - 0.7) / 0.2);
            } else {
                // Yellow to White hot
                thermal = mix(vec3(1.0, 1.0, 0.1), vec3(1.0, 1.0, 1.0), (lum - 0.9) / 0.1);
            }
            
            return vec4(thermal, color.a);
        }
    """

    private val thermalRuntimeShader: RuntimeShader? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(THERMAL_AGSL)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Raw float array for Night Vision High-Gain Phosphor Green Color Matrix.
     * Enhances green channel and luminance while suppressing red and blue.
     */
    private val NIGHT_VISION_MATRIX_VALUES = floatArrayOf(
        0.1f, 0.3f, 0.1f, 0f, 0f,    // Red suppressed
        0.3f, 1.6f, 0.4f, 0f, 20f,   // Green heavily boosted with gain offset
        0.1f, 0.2f, 0.1f, 0f, 0f,    // Blue suppressed
        0f,   0f,   0f,   1f, 0f     // Alpha preserved
    )

    /**
     * Raw float array for Tactical Monochrome High-Contrast Color Matrix.
     */
    private val TACTICAL_MONO_MATRIX_VALUES = floatArrayOf(
        0.35f, 0.55f, 0.15f, 0f, 10f,
        0.35f, 0.55f, 0.15f, 0f, 10f,
        0.35f, 0.55f, 0.15f, 0f, 10f,
        0f,    0f,    0f,    1f, 0f
    )

    /**
     * Raw float array for Thermal fallback ColorMatrix (if AGSL is unsupported or fails).
     */
    private val THERMAL_FALLBACK_MATRIX_VALUES = floatArrayOf(
        -0.5f, 1.5f, 0.0f, 0f, 50f,
         1.2f, -0.2f, 0.0f, 0f, 0f,
         0.0f, 0.2f, 1.8f, 0f, 0f,
         0f,   0f,   0f,   1f, 0f
    )

    val NightVisionColorMatrix = ColorMatrix(NIGHT_VISION_MATRIX_VALUES)
    val TacticalMonoColorMatrix = ColorMatrix(TACTICAL_MONO_MATRIX_VALUES)
    val ThermalFallbackColorMatrix = ColorMatrix(THERMAL_FALLBACK_MATRIX_VALUES)

    // Cached RenderEffects to avoid per-recomposition allocations
    private val nightVisionRenderEffect: androidx.compose.ui.graphics.RenderEffect? by lazy {
        try {
            RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(NIGHT_VISION_MATRIX_VALUES)).asComposeRenderEffect()
        } catch (e: Exception) {
            null
        }
    }

    private val tacticalMonoRenderEffect: androidx.compose.ui.graphics.RenderEffect? by lazy {
        try {
            RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(TACTICAL_MONO_MATRIX_VALUES)).asComposeRenderEffect()
        } catch (e: Exception) {
            null
        }
    }

    private val thermalRenderEffect: androidx.compose.ui.graphics.RenderEffect? by lazy {
        thermalRuntimeShader?.let { shader ->
            try {
                RenderEffect.createRuntimeShaderEffect(shader, "uContent").asComposeRenderEffect()
            } catch (e: Exception) {
                null
            }
        } ?: try {
            RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(THERMAL_FALLBACK_MATRIX_VALUES)).asComposeRenderEffect()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns the cached Compose [androidx.compose.ui.graphics.RenderEffect] for the given vision mode.
     */
    fun getRenderEffectForMode(mode: VisionMode): androidx.compose.ui.graphics.RenderEffect? {
        return when (mode) {
            VisionMode.STANDARD -> null
            VisionMode.NIGHT_VISION -> nightVisionRenderEffect
            VisionMode.TACTICAL_MONO -> tacticalMonoRenderEffect
            VisionMode.THERMAL -> thermalRenderEffect
        }
    }

    /**
     * Returns the ColorFilter for the given vision mode (used for fallback or Image painting).
     */
    fun getColorFilterForMode(mode: VisionMode): ColorFilter? {
        return when (mode) {
            VisionMode.STANDARD -> null
            VisionMode.NIGHT_VISION -> ColorFilter.colorMatrix(NightVisionColorMatrix)
            VisionMode.TACTICAL_MONO -> ColorFilter.colorMatrix(TacticalMonoColorMatrix)
            VisionMode.THERMAL -> {
                if (thermalRuntimeShader != null) null
                else ColorFilter.colorMatrix(ThermalFallbackColorMatrix)
            }
        }
    }
}
