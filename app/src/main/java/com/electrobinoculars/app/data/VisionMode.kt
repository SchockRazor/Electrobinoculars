package com.electrobinoculars.app.data

/**
 * Multi-spectral tactical vision modes supported by the Electrobinoculars system.
 *
 * @property label User-facing tactical descriptive label.
 * @property code Short military telemetry designation code.
 */
enum class VisionMode(val label: String, val code: String) {
    STANDARD("STANDARD", "STD"),
    NIGHT_VISION("NIGHT VISION", "NVG"),
    THERMAL("THERMAL IR", "FLIR"),
    TACTICAL_MONO("TACTICAL MONO", "CRT")
}
