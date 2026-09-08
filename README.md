# 🔭 Electrobinoculars

> An immersive, Star Wars-inspired tactical HUD, digital zoom viewer, and multi-spectral vision system.

[![Platform](https://img.shields.io/badge/Platform-Android%2013%2B%20(API%2033%2B)-brightgreen.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-purple.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT%20%2F%20Fan%20Project-orange.svg)](#disclaimer--legal)

---

## 🌌 Project Overview

**Electrobinoculars** is a passion project built purely for fun, cosplay, and creative exploration. It transforms your mobile device into a futuristic macrobinocular / electrobinocular viewfinder reminiscent of the iconic surveillance equipment seen across a galaxy far, far away.

This repository hosts the **Android concept application (Iteration 1)**, which serves as the interactive software core, sensor telemetry engine, and visual effects testbed.

### 🛠️ Hardware & Future Roadmap: Raspberry Pi + 3D Printing

While this initial iteration runs as a native Android app for rapid development and high-performance camera/sensor integration, the long-term vision is to bring Electrobinoculars into the physical world:
* **Custom 3D-Printed Chassis:** Designing authentic, ergonomic dual-grip electrobinocular housings with physical dials, tactile switches, and eyepiece shrouds.
* **Raspberry Pi Embedded Core:** Porting the core HUD and camera pipeline to run on a Raspberry Pi (or similar SBC) paired with wide-angle camera modules, miniature displays, physical rotary encoders, and IMU sensors.
* **Prop & Cosplay Integration:** Creating a fully functional, field-ready physical prop for conventions, fan films, and display.

---

## ✨ Features (Android Iteration 1)

* **Wide Panoramic Tactical Visor:** Fixed landscape orientation with authentic Imperial Amber (`#FFA500` / `#FF9100`) vector HUD styling, perimeter framing brackets, and animated targeting crosshairs.
* **Tactical Digital Zoom:** Smooth edge thumb dial simulating a physical optical zoom wheel, paired with real-time digital magnification readouts (`[ 1.0x ]` up to maximum hardware ratio).
* **Multi-Spectral Vision Modes:** Real-time visual filtering powered by AGSL (`RuntimeShader`):
  * **Standard / Clear:** Unfiltered optical camera pass-through.
  * **Night Vision (NVG):** High-gain phosphor green tint with simulated sensor noise.
  * **Thermal / IR:** False-color gradient heat signature mapping.
  * **Tactical Monochrome:** High-contrast desaturation with CRT scanline raster effects.
* **Live Sensor Telemetry:**
  * **Compass Azimuth:** 0°–359° bearing with cardinal direction readouts (e.g., `248° WSW`).
  * **Pitch & Roll Horizon:** Real-time artificial horizon ladder and device tilt indicator.
  * **Dynamic Continuous Rangefinder:** Continuous tactical distance estimation calculated dynamically from inclination tilt angle and optical zoom factor.
* **Synthetic Test-Pattern Mode:** Built-in animated sci-fi calibration grid fallback for smooth testing in emulator environments or before camera permissions are granted.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio (Ladybug / Iguana or later recommended)
* Android SDK (Min SDK: **33** / Target SDK: **35**)
* JDK 17 or later

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/SchockRazor/Electrobinoculars.git
   cd Electrobinoculars
   ```
2. Build the debug APK via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Deploy to a connected device or Android emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 📜 Disclaimer & Legal / Haftungsausschluss

> [!CAUTION]
> ### ⚠️ USE AT YOUR OWN RISK / NUTZUNG AUF EIGENE GEFAHR
> **EN:** Downloading, installing (including APK sideloading), and running this software is done entirely at your own risk. The authors and contributors assume **NO LIABILITY** for any damages, hardware malfunctions, battery issues, data loss, or system crashes resulting from the use or installation of this software. The software is provided **"AS IS"** without warranties of any kind. Read the full [DISCLAIMER.md](DISCLAIMER.md) and [LICENSE](LICENSE).
>
> **DE:** Das Herunterladen, Installieren (inkl. Sideloading von APKs) und Ausführen dieser Software erfolgt ausdrücklich **auf eigene Gefahr**. Die Autoren und Mitwirkenden übernehmen **keinerlei Haftung für Schäden** an Geräten, Akkus, Datenverlust oder Fehlfunktionen. Die Software wird „wie besehen“ (AS IS) ohne jegliche Gewährleistung bereitgestellt. Details siehe [DISCLAIMER.md](DISCLAIMER.md) und [LICENSE](LICENSE).

### 🌌 Fan Project & Trademark Notice
* **Electrobinoculars** is an unofficial, non-commercial fan project created purely for fun, cosplay, and prop-making exploration.
* **Star Wars** and all associated names, marks, emblems, designs, and images are registered trademarks and/or copyrights of **Lucasfilm Ltd.**, **The Walt Disney Company**, or their respective affiliates and subsidiaries.
* This project is not affiliated with, endorsed by, or sponsored by Lucasfilm Ltd. or Disney.
* All registered trademarks, logos, and brand names mentioned remain the property of their respective owners. No copyright or trademark infringement is intended.

### 🚫 Non-Critical Sci-Fi Simulation
All telemetry readouts (dynamic rangefinder distance, compass heading, artificial horizon, and thermal vision modes) are **artistic, uncalibrated sci-fi simulations**. They must **NEVER** be used for actual navigation, real-world distance measurement, driving, emergency orientation, or any safety- or life-critical applications.