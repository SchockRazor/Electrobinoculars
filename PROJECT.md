# Project: Electrobinoculars Android Application

## Architecture
The Electrobinoculars Android application is an authentic Star Wars-inspired tactical sci-fi camera viewer designed natively for Android with Min SDK 33 and Target SDK 35 using Kotlin, Jetpack Compose, and CameraX.

### Package & Module Structure
- Package: `com.electrobinoculars.app`
- Module: `:app`

```
app/src/main/
├── AndroidManifest.xml
├── java/com/electrobinoculars/app/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── VisionMode.kt
│   │   ├── ZoomStateData.kt
│   │   ├── SensorTelemetry.kt
│   │   └── ElectrobinocularsUiState.kt
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt
│   │   │   ├── Theme.kt
│   │   │   └── Typography.kt
│   │   ├── viewfinder/
│   │   │   ├── ViewfinderContainer.kt
│   │   │   ├── CameraFeedView.kt
│   │   │   └── SyntheticCalibrationGrid.kt
│   │   ├── hud/
│   │   │   ├── TacticalVisorHud.kt
│   │   │   ├── ReticleOverlay.kt
│   │   │   ├── CornerBrackets.kt
│   │   │   └── HorizonLadder.kt
│   │   ├── controls/
│   │   │   ├── TacticalZoomSlider.kt
│   │   │   ├── VisionModeSelector.kt
│   │   │   └── DebugControlBar.kt
│   │   ├── shaders/
│   │   │   ├── VisionShaders.kt
│   │   │   ├── ScanlineOverlay.kt
│   │   │   └── GrainOverlay.kt
│   │   └── MainScreen.kt
│   └── sensor/
│       ├── SensorTelemetryManager.kt
│       ├── RangefinderEngine.kt
│       └── SensorFilter.kt
└── res/
    ├── values/
    │   ├── strings.xml
    │   ├── colors.xml
    │   └── themes.xml
    └── drawable/
        ├── ic_launcher.xml
        └── ic_launcher_foreground.xml
```

### Viewport 3-Layer Compose Architecture
To enforce the strict requirement that multi-spectral vision modes filter the camera feed/fallback while the vector HUD retains pure Imperial Amber (`#FFA500` / `#FF9100`), the UI is structured in 3 isolated composable layers:
1. **Layer 1 (Viewfinder Layer)**: Live CameraX `PreviewView` (in `ImplementationMode.COMPATIBLE` / TextureView) or animated `SyntheticCalibrationGrid` fallback. Receives the `ColorMatrix` / AGSL `RuntimeShader` `Modifier.graphicsLayer { renderEffect = ... }`.
2. **Layer 2 (Post-Processing Optical FX Layer)**: Procedural phosphor grain (NVG) and high-contrast CRT scanlines (Tactical Mono) composited over the feed.
3. **Layer 3 (Tactical HUD & Controls Layer)**: Vector HUD overlay in Imperial Amber (`#FFA500` / `#FF9100`), corner brackets, reticle, telemetry readouts, tactical zoom rocker, and 4-mode vision selector. This layer is completely isolated from filters.

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Android Scaffolding & Gradle Setup | Complete buildable Gradle configuration (AGP 8.7.3, Kotlin 2.0.21, Compose Compiler Plugin 2.0.21, Min SDK 33, Target SDK 35, package com.electrobinoculars.app) | M1 | ORIGINAL_REQUEST §R1 |
| 2 | CameraX Viewfinder Integration | ProcessCameraProvider lifecycle binding, PreviewView with TextureView (COMPATIBLE mode) and permission handling | M1 | ORIGINAL_REQUEST §R1 |
| 3 | Synthetic Test-Pattern Fallback | Animated procedural sci-fi calibration grid Canvas test scene when camera hardware or permissions are unavailable | M1 | ORIGINAL_REQUEST §R1 |
| 4 | Fixed Landscape Orientation | Locked landscape orientation (`sensorLandscape`) with immersive edge-to-edge window configuration | M1 | ORIGINAL_REQUEST §R2 |
| 5 | Imperial Amber Tactical Visor HUD | Wide panoramic vector HUD overlay in Imperial Amber (`#FFA500` / `#FF9100`), corner brackets, central targeting reticle, stadiametric markings | M2 | ORIGINAL_REQUEST §R2 |
| 6 | Viewport Color Isolation Architecture | 3-layer Compose Box isolation preventing viewfinder shaders from contaminating Imperial Amber HUD overlay | M2 | ORIGINAL_REQUEST §R4 |
| 7 | Tactical Zoom Slider & Telemetry | Edge-mounted tactile zoom dial/rocker with haptics, CameraControl setLinearZoom binding, live `[ 1.0x ]` magnification readout | M3 | ORIGINAL_REQUEST §R3 |
| 8 | Multi-Spectral Vision Modes Engine | 4-button tactical strip: Standard, Night Vision (phosphor green + grain), Thermal IR (AGSL Ironbow / false-color), Tactical Monochrome (CRT scanlines) | M4 | ORIGINAL_REQUEST §R4 |
| 9 | Compass Bearing Telemetry | SensorManager rotation vector / magnetic field listener, 0°–359° heading with 16-point cardinal bearing (e.g. `248° WSW`) | M5 | ORIGINAL_REQUEST §R5 |
| 10 | Artificial Horizon Pitch/Roll Ladder | Landscape-remapped Euler angles displaying tilt ladder ($4\text{dp/degree}$ camera-frame optical-axis pitch translation, $-\text{roll}$ rotation) | M5 | ORIGINAL_REQUEST §R5 |
| 11 | Dynamic Continuous Rangefinder | Continuous trigonometric formula ($D = H / \tan(|\text{pitch}|)$) based on camera optical-axis depression angle with EMA/dead-zone filtering and dynamic confidence levels (`[LOCK-ON]`, `[TRACKING]`, `[SCANNING]`, `[NO LOCK]`) | M5 | ORIGINAL_REQUEST §R5 |
| 12 | End-to-End Build & Compilation Verification | Successful build with Gradle (`gradlew.bat assembleDebug`) with 0 errors and debug APK output; Compose/Kotlin clean compilation | M6 | ORIGINAL_REQUEST §Acceptance |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Android Scaffolding & CameraX with Fallback | Project build scripts, AndroidManifest, CameraX PreviewView, animated synthetic test pattern Canvas, permissions | none | COMPLETED |
| M2 | Fixed Landscape Tactical Visor HUD | Imperial Amber vector HUD theme, 3-layer viewport, corner brackets, reticle, stadiametric markings | M1 | COMPLETED |
| M3 | Digital Zoom Thumb Slider & Telemetry | Edge-mounted tactical slider, linear zoom mapping, CameraControl binding, dynamic `[ 1.0x ]` telemetry | M1, M2 | COMPLETED |
| M4 | Multi-Spectral Vision Modes Engine | 4-mode selector, ColorMatrix filters, AGSL Ironbow thermal shader, procedural grain & scanlines | M1, M2 | COMPLETED |
| M5 | Real-Time Sensor Telemetry & Rangefinder | SensorManager landscape remapping, compass heading with cardinal bearing, pitch/roll ladder, dynamic rangefinder | M1, M2, M3 | COMPLETED |
| M6 | System Integration & Build Verification | Complete app integration, gradlew assembleDebug build verification, review, adversarial challenge, forensic audit | M1, M2, M3, M4, M5 | COMPLETED |

---

## Interface Contracts

### Viewfinder ↔ State (`com.electrobinoculars.app.data`)
```kotlin
enum class VisionMode(val label: String, val code: String) {
    STANDARD("STANDARD", "STD"),
    NIGHT_VISION("NIGHT VISION", "NVG"),
    THERMAL("THERMAL IR", "FLIR"),
    TACTICAL_MONO("TACTICAL MONO", "CRT")
}

data class ZoomStateData(
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 8.0f,
    val linearZoom: Float = 0.0f
)

data class SensorTelemetry(
    val headingDegrees: Float = 0.0f,
    val cardinalDirection: String = "N",
    val pitch: Float = 0.0f,
    val roll: Float = 0.0f,
    val estimatedDistanceMeters: Int = 100,
    val rangeConfidence: RangeConfidence = RangeConfidence.NO_LOCK,
    val isRangeStable: Boolean = true,
    val isAvailable: Boolean = true
)

data class ElectrobinocularsUiState(
    val visionMode: VisionMode = VisionMode.STANDARD,
    val zoomState: ZoomStateData = ZoomStateData(),
    val telemetry: SensorTelemetry = SensorTelemetry(),
    val hasCameraPermission: Boolean = false,
    val isCameraHardwareAvailable: Boolean = true,
    val isUsingFallbackTestPattern: Boolean = false,
    val isUiVisible: Boolean = false,
    val isDebugPanelVisible: Boolean = true
)
```

### Sensor Telemetry, Attitude & Rangefinder Math (`com.electrobinoculars.app.sensor`)
```kotlin
data class CameraAngles(
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val azimuthDegrees: Float
)

object CameraAttitude {
    // Converts a 9-element rotation matrix into camera optical-axis Euler angles.
    // In landscape orientation, standard device-frame pitch from getOrientation() gimbal-locks.
    // CameraAttitude projects gravity onto the camera's optical axis (-Z axis out the back)
    // to determine true depression/elevation angle, horizon roll, and azimuth.
    fun fromRotationMatrix(matrix: FloatArray): CameraAngles
}

enum class RangeConfidence(val label: String) {
    HIGH("[LOCK-ON]"),     // pitch > 8°: reliable within ~10%
    MEDIUM("[TRACKING]"),  // pitch 4°–8°: reasonable within ~25%
    LOW("[SCANNING]"),     // pitch 2°–4°: approximate only
    NO_LOCK("[NO LOCK]")   // pitch < 2° or looking up
}

data class RangeResult(
    val distanceMeters: Int,
    val confidence: RangeConfidence
)

object RangefinderEngine {
    // Pure trigonometric: D = H / tan(|pitch|)
    // pitch = camera optical-axis depression (0° = level, negative = down), gravity-referenced
    // via CameraAttitude (getOrientation() device-frame pitch is unusable in landscape).
    // Zoom does NOT affect distance (zooming doesn't move the target)
    fun calculateRange(pitchDegrees: Float, observerHeightMeters: Float = 1.70f): RangeResult

    // Quantization: 1m (<50m), 5m (50-200m), 10m (200-500m), 25m (500-1000m), 50m (1-2km), 100m (>2km)

    fun getCardinalDirection(azimuthDegrees: Float): String
}

// Signal Processing (SensorFilter.kt):
// - ExponentialMovingAverage: single-pole EMA (α=0.12 for pitch, α=0.15 for roll)
// - CircularExponentialMovingAverage: 360°-aware EMA for azimuth
// - DeadZoneFilter: holds last stable value when |pitch| < 1.5° (hysteresis 0.5°)
```

---

## Code Layout
- Root project:
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `gradle.properties`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradlew.bat`
  - `gradlew`
- Application module:
  - `app/build.gradle.kts`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/java/com/electrobinoculars/app/MainActivity.kt`
  - `app/src/main/java/com/electrobinoculars/app/data/`
  - `app/src/main/java/com/electrobinoculars/app/ui/`
  - `app/src/main/java/com/electrobinoculars/app/sensor/`
