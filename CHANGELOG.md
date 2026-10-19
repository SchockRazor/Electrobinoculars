# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.4] - 2026-10-19

### Added
- Multi-touch pinch-to-zoom gesture support (`detectTransformGestures`) on the live CameraX / fallback viewfinder (`MainScreen.kt`).
- Tactile haptic feedback (`LocalHapticFeedback`) on zoom preset buttons, `0.5x` slider increments (`TacticalZoomSlider.kt`), and vision mode switching (`VisionModeSelector.kt`).
- Simulated roll override API (`setSimulatedRoll`, `adjustSimulatedRoll`) in `SensorTelemetryManager.kt`.
- Custom Imperial Amber vector launcher icon (`res/drawable/ic_launcher.xml`).
- Comprehensive unit test suite `SensorFilterTest.kt` for `ExponentialMovingAverage`, `CircularExponentialMovingAverage` (0°/360° wraparound), and `DeadZoneFilter` (hysteresis transitions), plus cardinal and horizon-crossing azimuth tests in `RangefinderEngineTest.kt`.

### Changed
- Dynamic zoom preset calculation in `TacticalZoomSlider.kt` scaled to the active `[minZoomRatio, maxZoomRatio]` range so the top preset matches the maximum zoom (`8.0x` by default) and highlights accurately.
- Cached static geometry paths (`gridPath`, `cardinalTicksPath`, `majorTicksPath`, `minorTicksPath`, `crosshairPath`, `milTicksPath`) in `SyntheticCalibrationGrid.kt` via `Modifier.drawWithCache`, eliminating per-frame `Path` allocations and trigonometric loops at 60 FPS.
- Cached `ColorFilter` instances in `VisionShaders.kt` and removed redundant `Build.VERSION.SDK_INT` checks in `MainActivity.kt` and `VisionShaders.kt`.
- Wrapped root `MainScreen()` composable in `ElectrobinocularsTheme` (`MainActivity.kt`).

### Fixed
- Fixed upright landscape compass gimbal lock and 180° North-to-South azimuth flip when tilting across the horizon (`0°` pitch) by extracting `azimuthDegrees` directly from the camera's `-Z` optical axis in `CameraAttitude.fromRotationMatrix` (`RangefinderEngine.kt`).
- Fixed background battery drain by binding `SensorTelemetryManager` registration to `Lifecycle.Event.ON_RESUME` and `ON_PAUSE` (`MainScreen.kt`).
- Fixed camera permission state desynchronization between `DebugControlBar`, `MainScreen`, and `ViewfinderContainer`.
- Fixed top HUD bar overlap with `DebugControlBar` by offsetting the debug strip when `isUiVisible` is active and auto-collapsing the expanded debug drawer when toggling the HUD on.
- Replaced deprecated `windowManager.defaultDisplay.rotation` with `context.display?.rotation` (`SensorTelemetryManager.kt`).

## [0.0.3] - 2026-10-09

### Fixed
- Replaced device-frame Euler pitch/roll from `SensorManager.getOrientation()` with camera optical-axis attitude extraction (`CameraAttitude.fromRotationMatrix`) so upright landscape holding reads `0°` at the horizon instead of collapsing range estimates onto the `2m` minimum clamp.
- Applied `CircularExponentialMovingAverage` to roll telemetry and adjusted `HorizonLadder` pitch scale to `4dp/degree`.

## [0.0.2] - 2026-09-08

### Added
- Signal processing filter pipeline (`SensorFilter.kt`): `ExponentialMovingAverage`, `CircularExponentialMovingAverage`, and `DeadZoneFilter` with hysteresis.
- Dynamic rangefinder lock confidence indicators (`[LOCK-ON]`, `[TRACKING]`, `[SCANNING]`, `[NO LOCK]`) and distance quantization steps in `RangefinderEngine.kt`.
- MIT `LICENSE` and legal disclaimer (`Haftungsausschluss`) in `README.md`.

### Changed
- Decoupled optical zoom magnification from trigonometric ground-distance estimation (`D = H / tan(|pitch|)`).

## [0.0.1] - 2026-09-08

### Added
- Initial Electrobinoculars Android application with 3-layer Compose viewport architecture, CameraX viewfinder with animated `SyntheticCalibrationGrid` fallback, Imperial Amber Tactical Visor HUD, 4 multi-spectral vision modes (Standard, Night Vision, Thermal IR AGSL shader, Tactical Monochrome), tactical zoom slider, compass/horizon telemetry, and launch `DebugControlBar`.
