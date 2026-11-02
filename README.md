# FlyskAI ✈️

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)

**FlyskAI** is an open-source in-flight companion app designed for frequent flyers, pilots, and aviation enthusiasts. It turns your Android phone into an aircraft cockpit Primary Flight Display (PFD) and moving map while cruising at 35,000 ft—**100% offline and in Airplane Mode**, with no cellular reception or internet connection needed.

---

## ✨ Features

- **Primary Flight Display (PFD)**:
  - **Attitude Indicator (Artificial Horizon)**: Real-time pitch and bank angles calculated using 9-axis sensor fusion (`TYPE_ROTATION_VECTOR`) to eliminate centrifugal acceleration errors during aircraft turns and climbs.
  - **Airspeed Tape**: Ground speed tracking displayed in knots or km/h.
  - **Altitude Tape**: Satellite GNSS altitude reading in feet or meters.
  - **Heading & Track Compass**: True heading, magnetic heading, and ground track with wind drift indicators.
  - **Vertical Speed Indicator (VSI)**: Filtered rate-of-climb/descent calculation.
  - **Seatbelt Alert Tone**: Automatic cabin chime on steep pitch changes.

- **Moving Map & Navigation**:
  - Live aircraft position and track visualization on Mapbox vector maps.
  - Built-in airport database with ICAO airport search.
  - Real-time great-circle distance to destination, bearing, and estimated time of arrival (ETA).
  - Quick map recentering on aircraft position.

- **Built Specifically for Commercial Passenger Flights**:
  - Direct hardware GNSS (`LocationManager.GPS_PROVIDER`) receiver polling that operates reliably in **Airplane Mode** without requiring Google Play Services network assistance or cell towers.
  - Independent sensor attitude loop running at ~30 Hz, ensuring cockpit instruments remain active even before initial satellite lock.
  - Unit toggle: Switch effortlessly between Metric and Imperial flight units.

- **Adaptive Layout**:
  - Optimized for both Portrait (stacked PFD + map) and Landscape (side-by-side cockpit + map) orientations on phones and tablets.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Architecture**: MVVM with reactive Kotlin Coroutines & `StateFlow`
- **Location & Sensors**:
  - Android Native GNSS (`LocationManager.GPS_PROVIDER`) + `FusedLocationProviderClient` fallback
  - `SensorManager` with hardware rotation vector fusion, accelerometer, and magnetometer
- **Mapping**: [Mapbox Maps SDK for Android](https://docs.mapbox.com/android/maps/guides/)
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalogs (`libs.versions.toml`)

---

## 🚀 Getting Started & Building

### Prerequisites

- Android Studio Meerkat or newer
- JDK 17 or JDK 21
- Android SDK with `compileSdk 37`
- A free [Mapbox Account](https://account.mapbox.com/) for map tiles

### 1. Clone the repository

```bash
git clone https://github.com/Waletta/flyskai.git
cd flyskai
```

### 2. Configure Mapbox Credentials

Copy `local.properties.example` to `local.properties` and add your Mapbox tokens:

```bash
cp local.properties.example local.properties
```

Open `local.properties` and fill in your tokens:
```properties
MAPBOX_ACCESS_TOKEN=pk.your_mapbox_public_token_here
MAPBOX_DOWNLOADS_TOKEN=your_mapbox_download_token_here
```
*(Note: `local.properties` is in `.gitignore`, keeping your personal credentials completely private).*

### 3. Build & Run

Open the project in Android Studio, select your device or emulator, and press **Run** (`Shift + F10`), or build from the command line:

```bash
./gradlew :app:assembleDebug
```

---

## 📱 Flight Tips

When using FlyskAI on a commercial flight:
1. **Window Seat**: Place or hold your phone close to the passenger window for the best satellite signal line-of-sight.
2. **Initial Satellite Lock**: Inside an airplane cabin, cold-start satellite lock without cellular A-GPS typically takes **1 to 3 minutes**. Keep the phone steady near the window until the GPS fix is established.
3. **Airplane Mode**: Feel free to turn on Airplane Mode—FlyskAI speaks directly to the onboard GNSS satellite chip and internal motion sensors without needing Wi-Fi or cellular networks.

---

## 🤝 Contributing

Contributions from pilots, passengers, and developers are warmly welcomed!

- **Airport Database**: Help expand the list of known airports in `model/AirCraftData.kt`.
- **Bug Reports & Ideas**: Open an issue on GitHub to suggest features or report bugs.
- **Pull Requests**:
  1. Fork the repository
  2. Create a feature branch (`git checkout -b feature/amazing-feature`)
  3. Commit your changes (`git commit -m 'Add amazing feature'`)
  4. Push to the branch (`git push origin feature/amazing-feature`)
  5. Open a Pull Request

---

## 📄 License & Privacy

- **License**: This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
- **Privacy Policy**: Read our zero-tracking, on-device policy in [PRIVACY.md](PRIVACY.md).
