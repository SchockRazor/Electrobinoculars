# Original User Request

## Initial Request — 2026-09-08T11:24:07Z

Build the initial iteration of the Electrobinoculars Android application: a Star Wars-inspired tactical sci-fi camera viewer featuring a panoramic tactical visor HUD, digital zoom slider with magnification telemetry, live device sensor readouts (compass heading, pitch/roll artificial horizon, dynamic continuous rangefinding), switchable vision modes (Standard, Night Vision Green, Thermal IR, and Tactical Scanline), and an animated test pattern fallback for emulators.

Working directory: C:\Users\schoc\.gemini\antigravity\worktrees\Electrobinoculars\android_app_initial_iteration
Integrity mode: development

## Requirements

### R1. Android Scaffolding & Camera Feed with Test-Pattern Fallback
Initialize a complete, buildable native Android project targeting Min SDK 33 and Target SDK 35 with Kotlin, Jetpack Compose, and CameraX under package `com.electrobinoculars.app`. When camera hardware is available and permissions are granted, render the live camera stream in the viewfinder. If camera hardware is absent or permissions are pending/denied, display an animated synthetic sci-fi calibration grid test scene so the HUD and sensors can be operated in emulator environments.

### R2. Fixed Landscape Tactical Visor HUD
Enforce a fixed landscape orientation and render a wide panoramic tactical visor HUD overlay in Imperial Amber (`#FFA500` / `#FF9100`). The HUD must include perimeter tactical corner brackets, a central targeting reticle, and formatted sensor telemetry readouts.

### R3. Digital Zoom Thumb Slider & Magnification Telemetry
Provide an on-screen edge-mounted tactical slider control simulating a physical zoom dial/rocker. It must adjust the camera preview's digital zoom ratio and continuously update a digital magnification readout (e.g. `[ 1.0x ]` up to the device's maximum zoom capability).

### R4. Multi-Spectral Vision Modes
Provide a 4-button tactical mode selector strip on the screen edge to switch between:
1. Standard (unfiltered clear feed)
2. Night Vision (high-gain phosphor green tint with simulated grain)
3. Thermal / IR (false-color heat map simulation)
4. Tactical Monochrome (high-contrast CRT scanlines and desaturation)
The camera feed must render the selected filter in real-time, while the vector HUD overlay retains its consistent Imperial Amber signature.

### R5. Real-Time Sensor Telemetry & Rangefinder
Read device motion and magnetic sensors to compute and display:
1. Compass heading in degrees (0°–359°) with cardinal bearing (e.g. `248° WSW`).
2. Pitch and roll artificial horizon ladder indicating device tilt.
3. Dynamic continuous rangefinder calculating simulated target distance in meters based on tilt inclination and current zoom magnification.

## Acceptance Criteria

### Build & Compilation
- [ ] The project builds successfully with Gradle (`gradlew.bat assembleDebug`) with 0 errors and generates the debug APK.
- [ ] All Kotlin and Jetpack Compose source files compile with 0 unresolved symbols or syntax errors.

### Viewport & Display
- [ ] Application locks in fixed landscape orientation on launch.
- [ ] Panoramic tactical visor HUD renders in signature Imperial Amber vector styling.
- [ ] Viewfinder displays the live camera stream, or gracefully falls back to an animated synthetic test pattern if camera hardware/permission is unavailable.

### Controls & Shaders
- [ ] Tactical zoom slider adjusts camera zoom and displays live magnification telemetry.
- [ ] Vision mode selector toggles between all 4 modes (Standard, Night Vision, Thermal, Tactical Mono) with real-time visual filtering.

### Sensors & Telemetry
- [ ] Compass heading updates dynamically with device rotation.
- [ ] Pitch/roll artificial horizon reflects device tilt in landscape orientation.
- [ ] Rangefinder distance readout dynamically computes estimated distance in meters.
