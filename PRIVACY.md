# Privacy Policy for FlyskAI

*Last updated: November 2026*

**FlyskAI** is a free, open-source Android application created for aviation enthusiasts, pilots, and frequent flyers. We are committed to protecting your privacy. This policy explains what information the app uses, why it is needed, and how it is handled.

---

## 1. Information Collection and Usage

### Location Data (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)
- **What is collected**: Real-time geographic coordinates (latitude and longitude), GPS altitude, ground speed, and bearing.
- **Why it is needed**: To calculate and display your live flight parameters—such as airspeed tape, altitude tape, vertical speed (VSI), track, destination distance, and aircraft position on the moving map.
- **How it is processed**: **All location data is processed locally on your device in real time**. FlyskAI does not transmit your location to external servers, does not save a persistent history of your location, and does not sell or share your location data with third parties.

### Device Sensors
- **What is accessed**: Motion and geomagnetic sensors, including the hardware rotation vector sensor, accelerometer, and magnetometer.
- **Why it is needed**: To calculate the aircraft's attitude (pitch and roll angles for the artificial horizon) and magnetic heading for the compass.
- **How it is processed**: Sensor readings are processed ephemeral-only on your device at runtime. No sensor data is stored or uploaded.

---

## 2. Background Data Access

FlyskAI **does not collect location or sensor data in the background**. Location tracking and sensor monitoring are active strictly while the application is in the foreground on your screen and are immediately suspended when the app is minimized or closed.

---

## 3. Third-Party Services & Network Access

FlyskAI is designed to function **100% offline in Airplane Mode**. 

When an active internet connection is available (e.g. before departure or after landing), the app may contact:
- **Mapbox Maps SDK**: Used to render map tiles. Mapbox may receive standard network requests (e.g., IP address and device details) to serve map tiles in accordance with the [Mapbox Privacy Policy](https://www.mapbox.com/legal/privacy).

FlyskAI contains **no advertising SDKs, no analytics trackers, no third-party telemetry, and no in-app purchases**.

---

## 4. Data Retention and Account Information

FlyskAI does not require account creation, registration, or login. We do not store, retain, or have access to any of your personal data on external servers.

---

## 5. Open Source Transparency

The full source code of FlyskAI is public and open-source under the Apache License 2.0. You can inspect the codebase and verify our data handling practices at:
[https://github.com/Waletta/flyskai](https://github.com/Waletta/flyskai)

---

## 6. Children's Privacy

FlyskAI does not collect any personally identifiable information from anyone, including children under the age of 13.

---

## 7. Changes to This Privacy Policy

If any changes are made to this Privacy Policy, the updated version will be posted here with a revised "Last updated" date.

---

## 8. Contact Us

If you have any questions or suggestions regarding this Privacy Policy, please feel free to reach out via:
- **GitHub Issues**: [https://github.com/Waletta/flyskai/issues](https://github.com/Waletta/flyskai/issues)
- **Email**: attilio.mucciaccio@gmail.com
